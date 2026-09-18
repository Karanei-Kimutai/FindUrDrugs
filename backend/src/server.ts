import express, { Request, Response } from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import pool from './config/db';
import searchRoutes from './routes/searchRoutes';
import authRoutes from './routes/authRoutes';
import inventoryRoutes from './routes/inventoryRoutes';
import orderRoutes from './routes/orderRoutes';
import reservationRoutes from './routes/reservationRoutes';
import subscriptionRoutes from './routes/subscriptionRoutes';

/**
 * Express application entry point.
 *
 * This module configures the shared middleware stack, mounts all feature
 * routers, and exposes a health check that verifies both the API process and
 * the PostgreSQL connection are available.
 */

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

//Routes
app.use('/api/search', searchRoutes);
app.use('/api/auth', authRoutes);
app.use('/api/inventory', inventoryRoutes);
app.use('/api/orders', orderRoutes);
app.use('/api/reservations', reservationRoutes);
app.use('/api/subscription', subscriptionRoutes);

/**
 * Lightweight runtime probe that also exercises PostgreSQL with SELECT NOW().
 * Returning a 200 here means the web server and database are both reachable.
 */
app.get('/health', async (req: Request, res: Response) => {
    try {
        const dbResult = await pool.query('SELECT NOW()');
        res.status(200).json({
            status: 'ok',
            message: 'FindUrDrugz API is running',
            timestamp: dbResult.rows[0].now
        });
    } catch (error) {
        console.error('Database connection failed:', error);
        res.status(500).json({ status: 'error', message: 'Database connection failed' });
    }
});

/**
 * Starts the HTTP server after middleware and routes have been registered.
 */
app.listen(PORT, () => {
    console.log(`Server is running on http://localhost:${PORT}`);
});
