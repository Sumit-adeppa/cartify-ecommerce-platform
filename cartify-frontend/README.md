# Cartify Frontend

React 18 + Vite frontend for the Cartify microservices backend.

## Requirements
- Node.js 18 or later
- npm
- Cartify API Gateway running at `http://localhost:8080`

## Run locally

```bash
npm ci
```

Copy the environment example:

```powershell
Copy-Item .env.example .env
```

Start Vite:

```bash
npm run dev
```

Open `http://localhost:5173`.

## API and authentication

The frontend sends requests through the API Gateway only. It does not call individual microservices directly.

- Default Gateway URL: `http://localhost:8080`
- Configure another URL with `VITE_API_BASE_URL` in `.env`.
- The Gateway CORS configuration must allow `http://localhost:5173` and credentials.
- Authentication uses the HttpOnly `authToken` cookie. Requests use `credentials: include`; the frontend does not read or store the JWT in JavaScript.
- Registration creates a CUSTOMER account. Admin role assignment must be performed through the trusted admin workflow, not from the public registration form.

## Backend endpoints used

- `POST /api/users/register`
- `POST /api/auth/login`
- `POST /api/auth/validate`
- `POST /api/auth/logout`
- `GET /api/products`
- `GET /api/categories`
- `GET /api/cart/{userId}`
- `GET /api/cart/{userId}/count`
- `POST /api/cart/{userId}/items`
- `PUT /api/cart/{userId}/items/{productId}`
- `DELETE /api/cart/{userId}/items/{productId}`
- `POST /api/orders/{userId}`
- `GET /api/orders/user/{userId}`
- `POST /api/payment/create/{orderId}/user/{userId}`
- `POST /api/payment/verify/{userId}`
- Admin endpoints under `/admin/**`
- Analytics endpoints under `/api/analytics/**`

## Important integration contract checks

The existing backend summaries establish the paths above and the main cart/product/order DTO fields. Before using live checkout, confirm the Payment Service create response includes:
- `razorpayOrderId`
- `amount` in paise
- `currency`
- public Razorpay key ID (`keyId` or equivalent)

If your Payment Service uses different response field names, update the small mapping in `src/CartPage.jsx`. Never put the Razorpay secret key in this frontend.
