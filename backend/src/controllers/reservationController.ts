import { Request, Response } from 'express';
import pool from '../config/db';
import { AuthRequest } from '../middleware/authMiddleware';

/**
 * Creates a pickup reservation for the authenticated customer after confirming
 * that the requested inventory exists and is currently in stock.
 */
export const createReservation = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const customerId = req.user?.id;
        const { pharmacyId, medicineId, quantity } = req.body;

        if (!pharmacyId || !medicineId || !quantity) {
            res.status(400).json({ status: 'error', message: 'Pharmacy, medicine, and quantity are required.' });
            return;
        }

        const stockCheck = await pool.query(
            'SELECT quantity FROM inventory WHERE pharmacy_id = $1 AND medicine_id = $2',
            [pharmacyId, medicineId]
        );

        if (stockCheck.rows.length === 0 || stockCheck.rows[0].quantity < quantity) {
            res.status(400).json({ status: 'error', message: 'Insufficient stock available.' });
            return;
        }

        const result = await pool.query(
            'INSERT INTO reservations (customer_id, pharmacy_id, medicine_id, quantity) VALUES ($1, $2, $3, $4) RETURNING *',
            [customerId, pharmacyId, medicineId, quantity]
        );

        res.status(201).json({ status: 'success', data: result.rows[0] });
    } catch (error) {
        res.status(500).json({ status: 'error', message: 'Failed to create reservation' });
    }
};

/**
 * Updates a reservation status inside a PostgreSQL transaction.
 *
 * The reservation row is locked with FOR UPDATE and inventory is decremented in
 * the same BEGIN/COMMIT block when a pending reservation becomes CONFIRMED,
 * preventing race conditions and overselling during concurrent confirmations.
 */
export const updateReservationStatus = async (req: AuthRequest, res: Response): Promise<void> => {
    const client = await pool.connect();
    try {
        const { reservationId } = req.params;
        const { status } = req.body; 
        // Valid statuses: CONFIRMED, READY_FOR_PICKUP, COLLECTED, CANCELLED

        await client.query('BEGIN');

        const resResult = await client.query('SELECT * FROM reservations WHERE id = $1 FOR UPDATE', [reservationId]);
        if (resResult.rows.length === 0) {
            res.status(404).json({ status: 'error', message: 'Reservation not found' });
            return;
        }

        const reservation = resResult.rows[0];

        if (status === 'CONFIRMED' && reservation.status === 'PENDING') {
            const inventoryUpdate = await client.query(`
                UPDATE inventory 
                SET quantity = quantity - $1, last_updated = NOW() 
                WHERE pharmacy_id = $2 AND medicine_id = $3 AND quantity >= $1
                RETURNING id
            `, [reservation.quantity, reservation.pharmacy_id, reservation.medicine_id]);

            if (inventoryUpdate.rows.length === 0) {
                await client.query('ROLLBACK');
                res.status(400).json({ status: 'error', message: 'Not enough stock to confirm. Stock may have changed.' });
                return;
            }
        }

        const updateResult = await client.query(
            'UPDATE reservations SET status = $1, updated_at = NOW() WHERE id = $2 RETURNING *',
            [status, reservationId]
        );

        await client.query('COMMIT');
        res.status(200).json({ status: 'success', data: updateResult.rows[0] });
    } catch (error) {
        await client.query('ROLLBACK');
        res.status(500).json({ status: 'error', message: 'Failed to update reservation status' });
    } finally {
        client.release();
    }
};

/**
 * Returns the authenticated customer's pickup reservation history.
 */
export const getCustomerReservations = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const sql = `
            SELECT r.*, p.name AS pharmacy_name, m.name AS medicine_name, i.price
            FROM reservations r
            JOIN pharmacies p ON r.pharmacy_id = p.id
            JOIN medicines m ON r.medicine_id = m.id
            JOIN inventory i ON r.pharmacy_id = i.pharmacy_id AND r.medicine_id = i.medicine_id
            WHERE r.customer_id = $1
            ORDER BY r.created_at DESC
        `;
        const result = await pool.query(sql, [req.user?.id]);
        res.status(200).json({ status: 'success', data: result.rows });
    } catch (error) {
        res.status(500).json({ status: 'error', message: 'Failed to fetch customer reservations' });
    }
};

/**
 * Returns reservations that have been placed against a specific pharmacy.
 */
export const getPharmacyReservations = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const { pharmacyId } = req.params;
        const sql = `
            SELECT r.*, u.name AS customer_name, m.name AS medicine_name
            FROM reservations r
            JOIN users u ON r.customer_id = u.id
            JOIN medicines m ON r.medicine_id = m.id
            WHERE r.pharmacy_id = $1
            ORDER BY r.created_at DESC
        `;
        const result = await pool.query(sql, [pharmacyId]);
        res.status(200).json({ status: 'success', data: result.rows });
    } catch (error) {
        res.status(500).json({ status: 'error', message: 'Failed to fetch pharmacy reservations' });
    }
};