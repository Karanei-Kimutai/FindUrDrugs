import { Request, Response } from 'express';
import pool from '../config/db';
import { AuthRequest } from '../middleware/authMiddleware';
import { checkPremiumStatus } from './subscriptionController';

/**
 * Creates a delivery order for the authenticated customer.
 *
 * Premium status is checked via RevenueCat-backed verification so the delivery
 * fee can be discounted before the order is persisted. The handler also performs
 * a preflight stock check to reject obviously unfulfillable orders before the
 * pharmacy accepts them.
 */
export const createOrder = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const customerId = req.user?.id;
        if (!customerId) {
            res.status(401).json({ status: 'error', message: 'Unauthorized' });
            return;
        }
        const isPremium = await checkPremiumStatus(customerId);
        const { pharmacyId, medicineId, quantity, deliveryAddress, customerPhone } = req.body;

        if (!pharmacyId || !medicineId || !quantity || !deliveryAddress || !customerPhone) {
            res.status(400).json({ status: 'error', message: 'All fields are required.' });
            return;
        }

        // Fetch the pharmacy's standard delivery fee
        const pharmacyResult = await pool.query('SELECT delivery_fee FROM pharmacies WHERE id = $1', [pharmacyId]);
        const standardFee = pharmacyResult.rows[0].delivery_fee;

        // Apply MVP Benefit: 50% discount for premium users
        const finalDeliveryFee = isPremium ? standardFee * 0.5 : standardFee;

        // Verify enough stock exists before allowing the order
        const stockCheck = await pool.query(
            'SELECT quantity FROM inventory WHERE pharmacy_id = $1 AND medicine_id = $2',
            [pharmacyId, medicineId]
        );

        if (stockCheck.rows.length === 0 || stockCheck.rows[0].quantity < quantity) {
            res.status(400).json({ status: 'error', message: 'Insufficient stock available.' });
            return;
        }

        const sql = `
            INSERT INTO orders (customer_id, pharmacy_id, medicine_id, quantity, delivery_address, customer_phone)
            VALUES ($1, $2, $3, $4, $5, $6)
            RETURNING *
        `;

        const result = await pool.query(sql, [customerId, pharmacyId, medicineId, quantity, deliveryAddress, customerPhone]);

        res.status(201).json({ status: 'success', data: result.rows[0] });
    } catch (error) {
        console.error('Create order error:', error);
        res.status(500).json({ status: 'error', message: 'Failed to create order' });
    }
};

/**
 * Updates an order lifecycle status inside a PostgreSQL transaction.
 *
 * The selected order row is locked with FOR UPDATE so concurrent pharmacy
 * actions cannot accept the same stock twice. When a pending order moves to
 * ACCEPTED, the inventory row is decremented in the same transaction and only
 * succeeds if the requested quantity is still available.
 */
export const updateOrderStatus = async (req: AuthRequest, res: Response): Promise<void> => {
    const client = await pool.connect();
    try {
        const { orderId } = req.params;
        const { status } = req.body;
        // Valid statuses: ACCEPTED, OUT_FOR_DELIVERY, COMPLETED, REJECTED, CANCELLED

        await client.query('BEGIN');

        // Fetch current order state
        const orderResult = await client.query('SELECT * FROM orders WHERE id = $1 FOR UPDATE', [orderId]);
        if (orderResult.rows.length === 0) {
            res.status(404).json({ status: 'error', message: 'Order not found' });
            return;
        }

        const order = orderResult.rows[0];

        // Deduct inventory if transitioning to ACCEPTED
        if (status === 'ACCEPTED' && order.status === 'PENDING') {
            const inventoryUpdate = await client.query(`
                UPDATE inventory 
                SET quantity = quantity - $1, last_updated = NOW() 
                WHERE pharmacy_id = $2 AND medicine_id = $3 AND quantity >= $1
                RETURNING id
            `, [order.quantity, order.pharmacy_id, order.medicine_id]);

            if (inventoryUpdate.rows.length === 0) {
                await client.query('ROLLBACK');
                res.status(400).json({ status: 'error', message: 'Not enough stock to accept this order. Stock may have changed.' });
                return;
            }
        }

        const updateResult = await client.query(
            'UPDATE orders SET status = $1, updated_at = NOW() WHERE id = $2 RETURNING *',
            [status, orderId]
        );

        await client.query('COMMIT');
        res.status(200).json({ status: 'success', data: updateResult.rows[0] });
    } catch (error) {
        await client.query('ROLLBACK');
        console.error('Update order status error:', error);
        res.status(500).json({ status: 'error', message: 'Failed to update order status' });
    } finally {
        client.release();
    }
};

/**
 * Returns the authenticated customer's delivery order history.
 */
export const getCustomerOrders = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const sql = `
            SELECT o.*, p.name AS pharmacy_name, m.name AS medicine_name, i.price
            FROM orders o
            JOIN pharmacies p ON o.pharmacy_id = p.id
            JOIN medicines m ON o.medicine_id = m.id
            JOIN inventory i ON o.pharmacy_id = i.pharmacy_id AND o.medicine_id = i.medicine_id
            WHERE o.customer_id = $1
            ORDER BY o.created_at DESC
        `;
        const result = await pool.query(sql, [req.user?.id]);
        res.status(200).json({ status: 'success', data: result.rows });
    } catch (error) {
        res.status(500).json({ status: 'error', message: 'Failed to fetch customer orders' });
    }
};

/**
 * Returns incoming orders for a specific pharmacy.
 */
export const getPharmacyOrders = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const { pharmacyId } = req.params;
        const sql = `
            SELECT o.*, u.name AS customer_name, m.name AS medicine_name
            FROM orders o
            JOIN users u ON o.customer_id = u.id
            JOIN medicines m ON o.medicine_id = m.id
            WHERE o.pharmacy_id = $1
            ORDER BY o.created_at DESC
        `;
        const result = await pool.query(sql, [pharmacyId]);
        res.status(200).json({ status: 'success', data: result.rows });
    } catch (error) {
        res.status(500).json({ status: 'error', message: 'Failed to fetch pharmacy orders' });
    }
};