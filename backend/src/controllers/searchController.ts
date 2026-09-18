import { Request, Response } from 'express';
import pool from '../config/db';

/**
 * Searches stocked medicines across pharmacies and ranks results by either
 * distance or price.
 *
 * The SQL embeds the Haversine formula directly in PostgreSQL so each result
 * includes an approximate distance in kilometers from the caller's latitude and
 * longitude. Zero-stock inventory is excluded at the query level, and the ORDER
 * BY clause is reduced to a small allowlist so callers can switch between
 * nearest and cheapest sorting without opening a SQL injection path.
 */
export const searchMedicines = async (req: Request, res: Response): Promise<void> => {
    try {
        const { query, lat, lng, sortBy = 'nearest' } = req.query;

        // 1. Validate inputs
        if (!query || !lat || !lng) {
            res.status(400).json({ 
                status: 'error', 
                message: 'Missing required parameters: query, lat, and lng are required.' 
            });
            return;
        }

        const userLat = parseFloat(lat as string);
        const userLng = parseFloat(lng as string);
        const searchQuery = `%${query}%`;

        // 2. Safely determine the order clause to prevent SQL injection
        const orderClause = sortBy === 'cheapest' ? 'i.price ASC, distance ASC' : 'distance ASC';

        // 3. Construct the SQL query with Haversine distance calculation (in kilometers)
        const sql = `
            SELECT 
                p.id AS pharmacy_id,
                p.name AS pharmacy_name,
                p.address,
                p.verified,
                p.delivery_available,
                p.delivery_fee,
                m.id AS medicine_id,
                m.name AS medicine_name,
                m.strength,
                i.price,
                i.quantity,
                i.last_updated,
                (6371 * acos(
                    cos(radians($1)) * cos(radians(p.latitude)) * 
                    cos(radians(p.longitude) - radians($2)) + 
                    sin(radians($1)) * sin(radians(p.latitude))
                )) AS distance
            FROM inventory i
            JOIN pharmacies p ON i.pharmacy_id = p.id
            JOIN medicines m ON i.medicine_id = m.id
            WHERE i.quantity > 0 
            AND (
                m.name ILIKE $3 OR 
                m.generic_name ILIKE $3 OR 
                m.brand_name ILIKE $3
            )
            ORDER BY ${orderClause}
        `;

        const result = await pool.query(sql, [userLat, userLng, searchQuery]);

        // 4. Return results
        res.status(200).json({
            status: 'success',
            results: result.rows.length,
            data: result.rows
        });

    } catch (error) {
        console.error('Search error:', error);
        res.status(500).json({ status: 'error', message: 'An error occurred while searching' });
    }
};