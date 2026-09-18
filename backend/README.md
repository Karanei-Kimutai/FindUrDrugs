# FindUrDrugz API

The backend service for the FindUrDrugz MVP, built with Node.js, Express, TypeScript, and PostgreSQL. It provides secure REST APIs for location-based medicine discovery, pharmacy inventory management, order and reservation processing, and premium subscription verification via RevenueCat.

## Tech Stack

- Runtime: Node.js
- Framework: Express.js
- Language: TypeScript
- Database: PostgreSQL via Docker Compose
- Authentication: JSON Web Tokens and bcrypt
- Monetization: RevenueCat REST API

## Project Structure

```text
backend/
├── src/
│   ├── config/          # Database connection pooling (db.ts)
│   ├── controllers/     # Business logic (auth, search, inventory, orders, reservations, subscriptions)
│   ├── middleware/      # JWT verification and RBAC guards
│   ├── routes/          # Express router definitions
│   ├── scripts/         # Database seeding scripts
│   └── server.ts        # Express app initialization
├── .env                 # Local environment variables
├── .env.example         # Example environment variables
├── package.json         # Dependencies and npm scripts
└── tsconfig.json        # TypeScript compiler configuration
```

## Setup and Installation

1. Start the database

Ensure Docker is running, then start PostgreSQL from the repository root:

```bash
cd ..
docker-compose up -d
```

The database container runs the schema in `database/schema/01_init.sql` on first boot.

2. Install dependencies

```bash
cd backend
npm install
```

3. Environment variables

Create `.env` in the `backend/` directory:

```env
PORT=3000
JWT_SECRET=your_jwt_secret_here
REVENUECAT_SECRET_KEY=your_revenuecat_secret_key_here
```

This project intentionally uses host port `5433` for PostgreSQL because the machine already has a local PostgreSQL instance running on the default host port `5432`.
The backend reads database host, port, and credentials from the repository root `.env` file via `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, and `DB_NAME`, so they do not need to be duplicated in `backend/.env`.

Create the root `.env` file with the shared database settings:

```env
DB_HOST=localhost
DB_PORT=5433
DB_USER=your_database_user
DB_PASSWORD=your_database_password
DB_NAME=your_database_name
```

4. Seed the database

Populate demo users, pharmacies, medicines, and inventory data, including Nairobi coordinates and zero-stock edge cases used by the search and transactional flows:

```bash
npm run seed
```

5. Start the server

Run the development server with hot reloading:

```bash
npm run dev
```

## Scripts

- `npm run dev`: starts the API with nodemon and ts-node
- `npm run build`: compiles TypeScript from `src` into `dist`
- `npm start`: runs the compiled server from `dist/server.js`
- `npm run seed`: reseeds the local database with demo data

## API Reference

### Health Check

- `GET /health`: verifies API and database connectivity.

### Authentication

- `POST /api/auth/register`: registers a new user and returns a JWT.
- `POST /api/auth/login`: authenticates a user and returns a JWT.

### Discovery

- `GET /api/search`: searches for medicines.
- Query params: `query`, `lat`, `lng`, `sortBy` where `sortBy` is `nearest` or `cheapest`.
- The search query uses the Haversine formula in PostgreSQL and excludes zero-stock inventory.

### Inventory

- `GET /api/inventory/:pharmacyId`: fetches a pharmacy's current stock.
- `POST /api/inventory/:pharmacyId`: adds a medicine to stock.
- `PUT /api/inventory/:pharmacyId/:medicineId`: updates price and quantity.
- `DELETE /api/inventory/:pharmacyId/:medicineId`: removes a medicine from the catalog.
- All inventory routes require an authenticated `PHARMACY` user.

### Delivery Orders

- `POST /api/orders`: creates a delivery order for the authenticated customer.
- `GET /api/orders/customer`: returns the authenticated customer's order history.
- `GET /api/orders/pharmacy/:pharmacyId`: returns incoming orders for a pharmacy.
- `PATCH /api/orders/:orderId/status`: updates an order status and deducts stock on `ACCEPTED`.

### Pickup Reservations

- `POST /api/reservations`: creates a pickup reservation for the authenticated customer.
- `GET /api/reservations/customer`: returns the authenticated customer's reservation history.
- `GET /api/reservations/pharmacy/:pharmacyId`: returns incoming reservations for a pharmacy.
- `PATCH /api/reservations/:reservationId/status`: updates a reservation status and deducts stock on `CONFIRMED`.

### Subscriptions

- `GET /api/subscription/status`: verifies the authenticated customer's premium entitlement through a server-to-server RevenueCat request.

## Security Notes

- Search and authentication are public entry points; all other business routes require a valid JWT.
- Role-based middleware strictly separates customer and pharmacy capabilities.
- Passwords are hashed with bcrypt before storage.
- PostgreSQL transactions and row locking via `FOR UPDATE` are used during order acceptance and reservation confirmation to prevent race conditions and overselling.

## Troubleshooting

- If `npm run dev` fails during TypeScript startup, reinstall dependencies and keep the local `typescript` version compatible with `ts-node`.
- If the health check returns a database error, confirm Docker is running and that root `.env` still contains `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, and `DB_NAME`.
- If schema changes in `database/schema/01_init.sql` do not appear locally, recreate the PostgreSQL volume so initialization runs again.