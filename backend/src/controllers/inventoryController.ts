import { Request, Response } from 'express';
import pool from '../config/db';

/**
 * Returns the current inventory catalog for a pharmacy by joining inventory and
 * medicine metadata into a single response payload.
 */
export const getInventory = async (req: Request, res: Response): Promise<void> => {
    try {
        const { pharmacyId } = req.params;

        const sql = `
            SELECT 
                i.id AS inventory_id,
                m.id AS medicine_id,
                m.name AS medicine_name,
                m.strength,
                i.quantity,
                i.price,
                i.last_updated
            FROM inventory i
            JOIN medicines m ON i.medicine_id = m.id
            WHERE i.pharmacy_id = $1
            ORDER BY m.name ASC
        `;

        const result = await pool.query(sql, [pharmacyId]);

        res.status(200).json({
            status: 'success',
            data: result.rows
        });
    } catch (error) {
        console.error('Fetch inventory error:', error);
        res.status(500).json({ status: 'error', message: 'Failed to fetch inventory' });
    }
};

/**
 * Inserts a new medicine row into a pharmacy's inventory.
 *
 * PostgreSQL error code 23505 is handled explicitly because the schema enforces
 * a unique pharmacy-and-medicine pair, so duplicate adds should return a clear
 * conflict response instead of a generic server error.
 */
export const addInventoryItem = async (req: Request, res: Response): Promise<void> => {
    try {
        const { pharmacyId } = req.params;
        const { medicineId, quantity, price } = req.body;

        if (!medicineId || quantity === undefined || !price) {
            res.status(400).json({ status: 'error', message: 'Medicine ID, quantity, and price are required' });
            return;
        }

        const sql = `
            INSERT INTO inventory (pharmacy_id, medicine_id, quantity, price, last_updated)
            VALUES ($1, $2, $3, $4, NOW())
            RETURNING *
        `;

        const result = await pool.query(sql, [pharmacyId, medicineId, quantity, price]);

        res.status(201).json({
            status: 'success',
            message: 'Medicine added to inventory',
            data: result.rows[0]
        });
    } catch (error: any) {
        console.error('Add inventory error:', error);
        // Handle unique constraint violation if they try to add the same medicine twice
        if (error.code === '23505') {
            res.status(409).json({ status: 'error', message: 'Medicine already exists in inventory. Please update it instead.' });
        } else {
            res.status(500).json({ status: 'error', message: 'Failed to add medicine to inventory' });
        }
    }
};

/**
 * Updates price and quantity for an existing inventory item and forces
 * last_updated to NOW() so consumers can see the freshness of the stock data.
 */
export const updateInventoryItem = async (req: Request, res: Response): Promise<void> => {
    try {
        const { pharmacyId, medicineId } = req.params;
        const { quantity, price } = req.body;

        if (quantity === undefined || !price) {
            res.status(400).json({ status: 'error', message: 'Quantity and price are required' });
            return;
        }

        const sql = `
            UPDATE inventory 
            SET quantity = $1, price = $2, last_updated = NOW()
            WHERE pharmacy_id = $3 AND medicine_id = $4
            RETURNING *
        `;

        const result = await pool.query(sql, [quantity, price, pharmacyId, medicineId]);

        if (result.rows.length === 0) {
            res.status(404).json({ status: 'error', message: 'Inventory item not found' });
            return;
        }

        res.status(200).json({
            status: 'success',
            message: 'Inventory updated successfully',
            data: result.rows[0]
        });
    } catch (error) {
        console.error('Update inventory error:', error);
        res.status(500).json({ status: 'error', message: 'Failed to update inventory' });
    }
};

/**
 * Permanently removes a medicine from a pharmacy catalog.
 */
export const deleteInventoryItem = async (req: Request, res: Response): Promise<void> => {
    try {
        const { pharmacyId, medicineId } = req.params;

        const sql = `
            DELETE FROM inventory 
            WHERE pharmacy_id = $1 AND medicine_id = $2
            RETURNING id
        `;

        const result = await pool.query(sql, [pharmacyId, medicineId]);

        if (result.rows.length === 0) {
            res.status(404).json({ status: 'error', message: 'Inventory item not found' });
            return;
        }

        res.status(200).json({
            status: 'success',
            message: 'Medicine removed from inventory'
        });
    } catch (error) {
        console.error('Delete inventory error:', error);
        res.status(500).json({ status: 'error', message: 'Failed to delete inventory item' });
    }
};