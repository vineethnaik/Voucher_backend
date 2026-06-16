# VoucherPro Backend

Spring Boot API with **MongoDB** user storage and **JWT** authentication.

## Prerequisites

- Java 17+
- Maven 3.9+
- MongoDB running locally (or MongoDB Atlas URI)

## Configuration

Copy and edit the environment file:

```bash
cp .env.example .env
```

Then open `.env` and replace `YOUR_PASSWORD_HERE` with your MongoDB Atlas database user password.

The app loads `backend/.env` automatically when you run from the `backend` folder.

| Variable | Description |
|----------|-------------|
| `MONGODB_URI` | MongoDB Atlas connection string (database: `voucherpro`) |
| `JWT_SECRET` | dev secret (change in prod) | HMAC key for JWT signing |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | Token lifetime |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Frontend origin |

## Run

```bash
cd backend
mvn spring-boot:run
```

API runs at **http://localhost:8080**

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/health` | No | Health check |
| POST | `/api/auth/register` | No | Create account |
| POST | `/api/auth/login` | No | Sign in |
| GET | `/api/auth/me` | Bearer JWT | Current user |

### Register / Login body

```json
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "secret123"
}
```

Login only requires `email` and `password`.

### Response

```json
{
  "token": "eyJhbG...",
  "name": "Jane Doe",
  "email": "jane@example.com"
}
```

Users are stored in MongoDB collection `users` with BCrypt-hashed passwords.

## Voucher API

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/vouchers` | No | List all vouchers (marketplace) |
| GET | `/api/vouchers/{id}` | No | Get one voucher |
| POST | `/api/vouchers` | Admin JWT | Create voucher |
| PUT | `/api/vouchers/{id}` | Admin JWT | Update voucher |
| DELETE | `/api/vouchers/{id}` | Admin JWT | Delete voucher |

Vouchers are stored in MongoDB collection `vouchers`. Six default courses are seeded on first startup.

### Default admin (seeded on first startup)

| Email | Password |
|-------|----------|
| `admin@voucherpro.com` | `voucherpro-admin` |

Override via `ADMIN_EMAIL`, `ADMIN_PASSWORD`, and `ADMIN_NAME` environment variables.
