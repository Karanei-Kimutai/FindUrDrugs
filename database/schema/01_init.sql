-- ENUMS for strict status and role management
CREATE TYPE user_role AS ENUM ('CUSTOMER', 'PHARMACY', 'ADMIN');
CREATE TYPE order_status AS ENUM ('PENDING', 'ACCEPTED', 'OUT_FOR_DELIVERY', 'COMPLETED', 'REJECTED', 'CANCELLED');
CREATE TYPE reservation_status AS ENUM ('PENDING', 'CONFIRMED', 'READY_FOR_PICKUP', 'COLLECTED', 'CANCELLED');
CREATE TYPE payment_method AS ENUM ('CASH_ON_DELIVERY');

-- USERS
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(50),
    password VARCHAR(255) NOT NULL,
    role user_role DEFAULT 'CUSTOMER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- PHARMACIES
CREATE TABLE pharmacies (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address TEXT NOT NULL,
    phone VARCHAR(50) NOT NULL,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    verified BOOLEAN DEFAULT FALSE,
    delivery_available BOOLEAN DEFAULT FALSE,
    delivery_fee DECIMAL(10, 2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- MEDICINES (Global catalog)
CREATE TABLE medicines (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    generic_name VARCHAR(255),
    brand_name VARCHAR(255),
    strength VARCHAR(100),
    dosage_form VARCHAR(100),
    package_description TEXT,
    -- Add indexes for search performance
    CONSTRAINT unq_medicine UNIQUE (name, strength, dosage_form)
);
CREATE INDEX idx_medicines_name ON medicines (name);
CREATE INDEX idx_medicines_generic ON medicines (generic_name);

-- INVENTORY (Junction table linking pharmacies and medicines)
CREATE TABLE inventory (
    id SERIAL PRIMARY KEY,
    pharmacy_id INTEGER REFERENCES pharmacies(id) ON DELETE CASCADE,
    medicine_id INTEGER REFERENCES medicines(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL DEFAULT 0,
    price DECIMAL(10, 2) NOT NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unq_pharmacy_medicine UNIQUE (pharmacy_id, medicine_id)
);

-- ORDERS (Delivery)
CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    customer_id INTEGER REFERENCES users(id),
    pharmacy_id INTEGER REFERENCES pharmacies(id),
    medicine_id INTEGER REFERENCES medicines(id),
    quantity INTEGER NOT NULL,
    delivery_address TEXT NOT NULL,
    customer_phone VARCHAR(50) NOT NULL,
    status order_status DEFAULT 'PENDING',
    payment_method payment_method DEFAULT 'CASH_ON_DELIVERY',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- RESERVATIONS (Pickup)
CREATE TABLE reservations (
    id SERIAL PRIMARY KEY,
    customer_id INTEGER REFERENCES users(id),
    pharmacy_id INTEGER REFERENCES pharmacies(id),
    medicine_id INTEGER REFERENCES medicines(id),
    quantity INTEGER NOT NULL,
    status reservation_status DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Automatically update timestamps for orders and reservations
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_reservations_updated_at
    BEFORE UPDATE ON reservations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();