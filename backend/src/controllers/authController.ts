import { Request, Response } from 'express';
import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import pool from '../config/db';

const JWT_SECRET = process.env.JWT_SECRET || 'fallback_secret';

/**
 * Registers a new user, hashes their password with bcrypt, and returns a JWT
 * valid for seven days so the client can begin authenticated flows immediately.
 */
export const register = async (req: Request, res: Response): Promise<void> => {
    try {
        const { name, email, phone, password, role = 'CUSTOMER' } = req.body;

        if (!name || !email || !password) {
            res.status(400).json({ status: 'error', message: 'Name, email, and password are required' });
            return;
        }

        // Check if user already exists
        const userCheck = await pool.query('SELECT id FROM users WHERE email = $1', [email]);
        if (userCheck.rows.length > 0) {
            res.status(409).json({ status: 'error', message: 'Email is already registered' });
            return;
        }

        // Hash password
        const salt = await bcrypt.genSalt(10);
        const hashedPassword = await bcrypt.hash(password, salt);

        // Insert new user
        const result = await pool.query(
            'INSERT INTO users (name, email, phone, password, role) VALUES ($1, $2, $3, $4, $5) RETURNING id, name, email, role',
            [name, email, phone, hashedPassword, role]
        );

        const user = result.rows[0];

        // Generate JWT
        const token = jwt.sign({ id: user.id, role: user.role }, JWT_SECRET, { expiresIn: '7d' });

        res.status(201).json({
            status: 'success',
            data: { user, token }
        });
    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({ status: 'error', message: 'Server error during registration' });
    }
};

/**
 * Authenticates an existing user by comparing the submitted password against
 * the stored bcrypt hash before issuing a fresh seven-day JWT.
 */
export const login = async (req: Request, res: Response): Promise<void> => {
    try {
        const { email, password } = req.body;

        if (!email || !password) {
            res.status(400).json({ status: 'error', message: 'Email and password are required' });
            return;
        }

        // Fetch user
        const result = await pool.query('SELECT * FROM users WHERE email = $1', [email]);
        const user = result.rows[0];

        if (!user) {
            res.status(401).json({ status: 'error', message: 'Invalid credentials' });
            return;
        }

        // Verify password
        const isMatch = await bcrypt.compare(password, user.password);
        if (!isMatch) {
            res.status(401).json({ status: 'error', message: 'Invalid credentials' });
            return;
        }

        // Generate JWT
        const token = jwt.sign({ id: user.id, role: user.role }, JWT_SECRET, { expiresIn: '7d' });

        res.status(200).json({
            status: 'success',
            data: {
                user: {
                    id: user.id,
                    name: user.name,
                    email: user.email,
                    role: user.role
                },
                token
            }
        });
    } catch (error) {
        console.error('Login error:', error);
        res.status(500).json({ status: 'error', message: 'Server error during login' });
    }
};