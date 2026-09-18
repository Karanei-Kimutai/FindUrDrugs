import { Pool } from 'pg';
import dotenv from 'dotenv';
import path from 'path';

dotenv.config({ path: path.resolve(__dirname, '../../../.env') });
dotenv.config({ path: path.resolve(__dirname, '../../.env') });

const connectionString = process.env.DATABASE_URL ?? (
    (process.env.DB_HOST ?? 'localhost') &&
    (process.env.DB_PORT ?? '5433') &&
    process.env.DB_NAME &&
    process.env.DB_USER &&
    process.env.DB_PASSWORD
        ? `postgres://${encodeURIComponent(process.env.DB_USER)}:${encodeURIComponent(process.env.DB_PASSWORD)}@${process.env.DB_HOST ?? 'localhost'}:${process.env.DB_PORT ?? '5433'}/${process.env.DB_NAME}`
        : undefined
);

/**
 * Shared PostgreSQL connection pool for the backend.
 *
 * The pool reads its connection string from DATABASE_URL when provided, or
 * constructs one from the shared root database environment variables so the
 * backend and Docker Compose use a single source of truth.
 */
const pool = new Pool({
    connectionString,
});

/**
 * Logs successful client acquisition so local setup issues are visible during
 * development and seeding runs.
 */
pool.on('connect', () => {
    console.log('Successfully connected to the PostgreSQL database.');
});

/**
 * Fails loudly when an idle pooled client errors, which usually indicates the
 * backing database connection has become unhealthy.
 */
pool.on('error', (err) => {
    console.error('Unexpected error on idle client', err);
    process.exit(-1);
});

export default pool;