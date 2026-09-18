import bcrypt from 'bcrypt';
import pool from '../config/db';

/**
 * Rebuilds local demo data for development and manual API testing.
 *
 * The script truncates the transactional tables with CASCADE, then inserts demo
 * users, Nairobi-based pharmacies, and inventory with intentional edge cases
 * such as zero-stock items and competing prices so search ranking and stock
 * handling logic can be exercised realistically.
 */
async function seed() {
    try {
        const demoPassword = 'password123';
        const hashedPassword = await bcrypt.hash(demoPassword, 10);

        console.log('Clearing existing data...');
        // Cascade delete ensures we wipe everything cleanly before reseeding
        await pool.query(`
            TRUNCATE TABLE inventory, reservations, orders, medicines, pharmacies, users RESTART IDENTITY CASCADE;
        `);

        console.log('Inserting Users...');
        await pool.query(
            `
                INSERT INTO users (name, email, phone, password, role) VALUES 
                ('Customer Demo', 'customer@test.com', '0700000001', $1, 'CUSTOMER'),
                ('Pharmacy A Admin', 'adminA@test.com', '0700000002', $1, 'PHARMACY'),
                ('Pharmacy B Admin', 'adminB@test.com', '0700000003', $1, 'PHARMACY');
            `,
            [hashedPassword]
        );

        console.log('Inserting Pharmacies...');
        // Coordinates set around Nairobi for realistic demo distances
        await pool.query(`
            INSERT INTO pharmacies (name, address, phone, latitude, longitude, verified, delivery_available, delivery_fee) VALUES 
            ('Westlands Care Pharmacy', 'Woodvale Grove, Westlands', '0711000001', -1.2640000, 36.8040000, TRUE, TRUE, 150.00),
            ('Kilimani Meds', 'Argwings Kodhek Rd, Kilimani', '0711000002', -1.2880000, 36.7820000, TRUE, TRUE, 100.00),
            ('CBD Health Pharmacy', 'Moi Avenue, CBD', '0711000003', -1.2830000, 36.8220000, FALSE, TRUE, 50.00);
        `);

        console.log('Inserting Medicines...');
        await pool.query(`
            INSERT INTO medicines (name, generic_name, brand_name, strength, dosage_form, package_description) VALUES 
            ('Paracetamol 500mg', 'Paracetamol', 'Panadol', '500mg', 'Tablet', 'Pack of 20 tablets'),
            ('Amoxicillin 500mg', 'Amoxicillin', 'Amoxil', '500mg', 'Capsule', 'Pack of 15 capsules'),
            ('Cetirizine 10mg', 'Cetirizine', 'Zyrtec', '10mg', 'Tablet', 'Pack of 10 tablets'),
            ('Ibuprofen 400mg', 'Ibuprofen', 'Brufen', '400mg', 'Tablet', 'Pack of 20 tablets');
        `);

        console.log('Inserting Inventory...');
        // Demo setup: 
        // Kilimani is cheaper (90) but might be further. 
        // Westlands is more expensive (120). 
        // CBD is out of stock for Amoxicillin (quantity: 0).
        await pool.query(`
            INSERT INTO inventory (pharmacy_id, medicine_id, quantity, price, last_updated) VALUES 
            -- Westlands Care (ID 1)
            (1, 1, 50, 120.00, NOW() - INTERVAL '5 minutes'),   -- Paracetamol
            (1, 2, 20, 350.00, NOW() - INTERVAL '2 hours'),     -- Amoxicillin
            
            -- Kilimani Meds (ID 2)
            (2, 1, 100, 90.00, NOW() - INTERVAL '10 minutes'),  -- Paracetamol (Cheapest)
            (2, 3, 15, 150.00, NOW() - INTERVAL '1 day'),       -- Cetirizine
            
            -- CBD Health (ID 3)
            (3, 1, 30, 105.00, NOW() - INTERVAL '1 minute'),    -- Paracetamol
            (3, 2, 0, 300.00, NOW() - INTERVAL '5 days');       -- Amoxicillin (Out of stock)
        `);

        console.log('Database seeded successfully!');
        console.log(`Seeded demo users with password: ${demoPassword}`);
        process.exit(0);
    } catch (error) {
        console.error('Seeding failed:', error);
        process.exit(1);
    }
}

seed();