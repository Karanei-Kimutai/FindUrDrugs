# FindUrDrugs

FindUrDrugs is a medicine discovery and pharmacy operations project with an Android client, a Node.js and TypeScript backend, and a PostgreSQL database running in Docker.

## Project Structure

```text
FindUrDrugs/
├── .env.example        # Shared database environment template
├── .gitignore          # Ignore rules for local env files and build artifacts
├── android/            # Android client
├── backend/            # Express + TypeScript API
├── database/           # SQL schema and initialization scripts
├── docs/               # Project documentation and design notes
├── docker-compose.yml  # Local PostgreSQL service
└── README.md           # Root setup and development guide
```

The `android/` and `docs/` directories are included intentionally even when they are empty, so the repository structure already reflects the planned client and documentation areas.

## Prerequisites

- Node.js 20+
- npm
- Docker and Docker Compose

## Database Setup

Create a root `.env` file from the example:

```bash
cp .env.example .env
```

Root `.env.example` contains the shared database settings:

```env
DB_HOST=localhost
DB_PORT=5433
DB_USER=your_database_user
DB_PASSWORD=your_database_password
DB_NAME=your_database_name
```

This project uses host port `5433` for PostgreSQL instead of the usual `5432` because the machine already has a local PostgreSQL instance running on `5432`. Docker still exposes PostgreSQL internally on `5432`; only the host-side published port is changed.

Start the database from the repository root:

```bash
docker-compose up -d
```

The first container boot runs [database/schema/01_init.sql](database/schema/01_init.sql) automatically.

## Backend Setup

Move into the backend folder and install dependencies:

```bash
cd backend
npm install
```

Create the backend env file:

```bash
cp .env.example .env
```

Backend `.env.example` contains only backend-specific settings because the database connection details are read from the root `.env`:

```env
PORT=3000
JWT_SECRET=your_jwt_secret_here
REVENUECAT_SECRET_KEY=your_revenuecat_secret_key_here
```

Seed the local database with demo data:

```bash
npm run seed
```

Start the backend in development mode:

```bash
npm run dev
```

The API will be available at `http://localhost:3000`.

## Smoke Check

Once the backend is running, verify API and database connectivity:

```bash
curl http://localhost:3000/health
```

Expected response shape:

```json
{
	"status": "ok",
	"message": "FindUrDrugz API is running",
	"timestamp": "2026-09-18T12:32:49.221Z"
}
```

## Additional Notes

- The backend compiles TypeScript into `backend/dist/` when you run `npm run build`.
- Real env files are ignored by git; example env files stay committed as templates.
- For backend-specific API and architecture details, see [backend/README.md](backend/README.md).
