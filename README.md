# Cartify

Cartify is a microservices-based e-commerce application built with **Java, Spring Boot, Spring Cloud, React, and MySQL**. It provides a public product catalogue, customer authentication, shopping cart, order management, payment integration, and administrative/analytics features.

> **Project status:** This README documents the application structure and the intended local development workflow. Verify environment variable names and Docker Compose service definitions against the versions in your working repository before deployment. Do not commit real credentials or secret keys.

## Contents

- [Features](#features)
- [Architecture](#architecture)
- [Technology stack](#technology-stack)
- [Service overview](#service-overview)
- [Customer shopping flow](#customer-shopping-flow)
- [Prerequisites](#prerequisites)
- [Project structure](#project-structure)
- [Configuration](#configuration)
- [Run with Docker Compose](#run-with-docker-compose)
- [Run services individually](#run-services-individually)
- [Frontend setup](#frontend-setup)
- [API overview](#api-overview)
- [Authentication and authorization](#authentication-and-authorization)
- [Database design](#database-design)
- [Testing and troubleshooting](#testing-and-troubleshooting)
- [Security notes](#security-notes)
- [Future improvements](#future-improvements)

## Features

### Customer features

- Browse products, categories, and product details without signing in.
- Search and filter the product catalogue.
- Redirect unauthenticated visitors to the login page when they select **Add to Cart**.
- After successful login, return to the shopping page and continue the pending Add to Cart action.
- Register and sign in as a customer.
- Add products to the cart, view the cart, update quantities, and remove items.
- Place orders and view order history.
- Continue to the payment flow through the payment service.

### Administration and analytics

- Separate admin-facing application area.
- Admin-only product and user management operations.
- Business and sales analytics endpoints.
- Analytics service obtains order data through service-to-service communication.

### Platform features

- Independent Spring Boot services.
- Service registration and discovery through Netflix Eureka.
- API routing through Spring Cloud Gateway.
- Separate database ownership for data-owning services.
- Docker-based service packaging and deployment.
- Internal service calls through HTTP/Feign clients.

## Architecture

```text
                         React Frontend
                         (Vite / Browser)
                                |
                                | HTTP + Cookie
                                v
                       API Gateway :8080
                                |
                 +--------------+--------------+
                 |              |              |
                 v              v              v
             Auth Service   User Service   Product Service
               :8081          :8082           :8083
                 |              |              |
                 v              v              v
             Auth DB          User DB        Product DB

                 API Gateway routes customer operations
                                |
              +-----------------+-----------------+
              |                 |                 |
              v                 v                 v
          Cart Service     Order Service    Payment Service
             :8084            :8085             :8086
              |                 |                 |
              v                 v                 v
            Cart DB          Order DB         Payment DB

                                |
                    +-----------+-----------+
                    |                       |
                    v                       v
               Admin Service          Analytics Service
                  :8088                    :8087
                    |                       |
                    +------ Feign / HTTP ---+
                            |
                     Order Service APIs

             Service Discovery / Eureka :8761
             Registers and discovers application services
```

All browser-facing application requests should go through the API Gateway. Internal service endpoints are intended for service-to-service communication and should not be exposed as public customer APIs.

## Technology stack

| Area | Technology |
|---|---|
| Backend language | Java 17 |
| Backend framework | Spring Boot 4.0.8 |
| Microservices | Spring Cloud 2025.1.2 |
| Service discovery | Netflix Eureka |
| API gateway | Spring Cloud Gateway |
| Inter-service communication | OpenFeign / HTTP |
| Persistence | Spring Data JPA, Hibernate |
| Database | MySQL 8.x |
| Frontend | React, JavaScript |
| Frontend tooling | Vite, npm |
| Authentication | JWT in an HTTP-only cookie |
| Password hashing | BCrypt |
| Payments | Razorpay |
| Containers | Docker, Docker Compose |
| Build tools | Maven, npm |

Confirm the versions in each service's `pom.xml` and the frontend `package.json` if your checked-out branch differs.

## Service overview

The default local ports used by this application are:

| Service | Port | Responsibility |
|---|---:|---|
| API Gateway | `8080` | Public API entry point and request routing |
| Service Discovery | `8761` | Eureka registry |
| Auth Service | `8081` | Login, token validation, logout |
| User Service | `8082` | Customer and user data |
| Product Service | `8083` | Products, categories, product images |
| Cart Service | `8084` | Customer shopping carts |
| Order Service | `8085` | Order creation, order history, internal reporting |
| Payment Service | `8086` | Payment order creation and verification |
| Analytics Service | `8087` | Sales and business analytics |
| Admin Service | `8088` | Admin operations and service orchestration |
| React frontend | `5173` | Browser application in Vite development mode |

## Customer shopping flow

1. A visitor opens the Products page. Product and category browsing is public.
2. The visitor selects **Add to Cart**.
3. If the visitor is not authenticated, the frontend redirects to `/login` and preserves the pending product action.
4. The customer signs in.
5. After successful authentication, the application returns to the shopping page and retries the pending Add to Cart action.
6. The authenticated customer can open the cart, manage cart items, and place an order.
7. The customer proceeds through the payment flow.
8. Authenticated customers can view their orders.

The frontend redirect is a usability feature, not a security boundary. The gateway and backend services must still enforce authentication and resource ownership.

## Prerequisites

Install the following tools:

- Java 17 JDK
- Maven 3.9+
- Node.js and npm (use a version compatible with the frontend lockfile)
- Docker Engine or Docker Desktop
- Docker Compose v2
- Git

A MySQL server is required when running the backend services outside Docker. When using the supplied container configuration, use the MySQL container defined by that configuration.

## Project structure

The repository is organized as separate service projects and a frontend application:

```text
Cartify/
├── api-gateway/
├── service-discovery/
├── auth-service/
├── user-service/
├── product-service/
├── cart-service/
├── order-service/
├── payment-service/
├── analytics-service/
├── admin-service/
├── cartify-frontend/
├── database/
│   └── init.sql
├── docker-compose.yml
├── .env
└── .gitignore
```

The exact folder names may differ between the backend and frontend repositories or ZIP packages. Keep the service names consistent with the Docker Compose build contexts and frontend API configuration.

## Configuration

### Environment variables

Configure secrets outside source control. The backend deployment described for this project uses the following configuration values; check each service's `application.properties` and the Compose file for the exact variable mapping.

| Variable | Purpose |
|---|---|
| `MYSQL_ROOT_PASSWORD` | MySQL root password |
| `DB_USERNAME` | Application database username |
| `DB_PASSWORD` | Application database password |
| `INTERNAL_API_KEY` | Shared key for protected internal service endpoints |
| `JWT_SECRET` | JWT signing secret; use a sufficiently long random value compatible with the configured signing algorithm |
| `RAZORPAY_KEY_ID` | Razorpay API key ID |
| `RAZORPAY_KEY_SECRET` | Razorpay API key secret |

Create a local `.env` file in the directory expected by Docker Compose. Use a private, randomly generated value for each secret.

Example template (replace every placeholder with a local value):

```dotenv
MYSQL_ROOT_PASSWORD=replace_with_a_strong_local_password
DB_USERNAME=cartify_app
DB_PASSWORD=replace_with_a_strong_local_password
INTERNAL_API_KEY=replace_with_a_long_random_internal_key
JWT_SECRET=replace_with_a_long_random_jwt_signing_secret
RAZORPAY_KEY_ID=your_razorpay_test_key_id
RAZORPAY_KEY_SECRET=your_razorpay_test_key_secret
```

Do not use these example placeholders as actual credentials. Do not commit `.env` to Git. If a secret has been exposed in logs, screenshots, ZIP files, or a public repository, rotate it.

### Database configuration

The application uses separate databases for data-owning services:

- `cartify_auth_db`
- `cartify_user_db`
- `cartify_product_db`
- `cartify_cart_db`
- `cartify_order_db`
- `cartify_payment_db`

Analytics and Admin are orchestration/reporting services in the documented design and do not require their own application database.

When running inside Docker, service-to-service database URLs should use the MySQL Compose service hostname and container port, for example:

```text
jdbc:mysql://mysql:3306/cartify_user_db
```

When connecting from the host machine, use the host port published by your Compose file. The project's documented local mapping has used `3307:3306`, but confirm the current `docker-compose.yml`.

## Run with Docker Compose

Run these commands from the directory containing `docker-compose.yml`.

1. Create and populate `.env` using the configuration section above.
2. Build and start the application:

```bash
docker compose up -d --build
```

3. Check container status:

```bash
docker compose ps
```

4. Follow logs for a service:

```bash
docker compose logs -f service-discovery
docker compose logs -f api-gateway
docker compose logs -f auth-service
```

5. Open the application:

- Frontend: `http://localhost:5173` (if the frontend is included in Compose; otherwise start it separately)
- API Gateway: `http://localhost:8080`
- Eureka dashboard: `http://localhost:8761`

Eureka and the application services may take time to become healthy and register. Wait until the required service instances appear as `UP` in Eureka before testing requests that depend on service discovery.

Stop the containers:

```bash
docker compose down
```

This preserves named volumes. To delete database data as well, Docker volumes must be removed separately. Do not use `docker compose down -v` unless you intentionally want to delete the persisted local database data.

## Run services individually

If you are not using Docker Compose:

1. Start MySQL and create the databases listed above.
2. Start `service-discovery`.
3. Start the data-owning services and configure each service with the correct database URL and Eureka URL.
4. Start `api-gateway`, `admin-service`, and `analytics-service`.
5. Start the frontend.

For a Spring Boot service:

```bash
cd <service-directory>
mvn clean package
mvn spring-boot:run
```

Alternatively, run the generated executable JAR:

```bash
java -jar target/<application-jar>.jar
```

Use the service's own `pom.xml` and `application.properties` as the source of truth for its build and runtime configuration.

## Frontend setup

Open a terminal in the frontend directory:

```bash
cd cartify-frontend
npm ci
npm run dev
```

Vite normally serves the development application at:

```text
http://localhost:5173
```

The frontend should call the API Gateway, not individual microservices or the MySQL database. Configure the frontend API base URL using the environment-variable convention implemented in its API client and Vite configuration.

For a production frontend build:

```bash
npm run build
```

The output directory is normally `dist/` for Vite. Serve that directory through a production web server or static hosting service and configure the API base URL for the deployed gateway.

## API overview

The following routes describe the application-facing API areas. Exact request DTOs, response fields, and HTTP methods are defined in the controller classes of each service.

| API route | Purpose | Access |
|---|---|---|
| `/api/auth/**` | Login, validation, logout | Public/authenticated as appropriate |
| `/api/users/**` | User operations | Authenticated, with ownership rules |
| `/api/products/**` | Product catalogue and product operations | Catalogue reads public; mutations admin-only |
| `/api/categories/**` | Category operations | Catalogue reads public; mutations admin-only |
| `/api/cart/**` | Cart operations | Authenticated customer |
| `/api/orders/**` | Order operations | Authenticated customer |
| `/api/payment/**` | Payment operations | Authenticated customer |
| `/api/analytics/**` | Analytics data | Admin-only |
| `/admin/**` | Administrative operations | Admin-only |

### Cart API routes

The documented cart operations include:

```text
POST   /api/cart/{userId}/items
GET    /api/cart/{userId}
PUT    /api/cart/{userId}/items/{productId}
DELETE /api/cart/{userId}/items/{productId}
DELETE /api/cart/{userId}/clear
GET    /api/cart/{userId}/count
```

### Order API routes

```text
POST /api/orders/{userId}
GET  /api/orders/user/{userId}
GET  /api/orders/{orderId}/user/{userId}
PUT  /api/orders/{orderId}/user/{userId}/cancel
```

Payment and internal service endpoints are implemented in their respective controllers. Consult those controllers for exact request bodies and response formats.

## Authentication and authorization

- Login is handled by the Auth Service.
- The application uses a JWT stored in an HTTP-only cookie named `authToken` in the documented configuration.
- Passwords are hashed with BCrypt in the user-registration flow.
- The API Gateway validates the authentication token and applies route-level authorization.
- Customer operations must be restricted to the authenticated customer's own resources.
- Admin operations require the `ADMIN` role.
- Product and category GET requests are public so visitors can browse without an account.
- Internal endpoints should be protected with the configured internal API key and should not be exposed as public frontend routes.

For local HTTP development, cookie security settings may differ from production. Production deployments should use HTTPS and secure cookie settings.

## Database design

Each data-owning microservice owns its database. A service should access its own data store and communicate with other services through APIs rather than querying another service's database directly.

| Database | Owning service |
|---|---|
| `cartify_auth_db` | Auth Service |
| `cartify_user_db` | User Service |
| `cartify_product_db` | Product Service |
| `cartify_cart_db` | Cart Service |
| `cartify_order_db` | Order Service |
| `cartify_payment_db` | Payment Service |

The product catalogue stores product and category information. Cart items refer to customer and product identifiers. Orders retain product snapshots so order history is not dependent on later catalogue changes.

## Testing and troubleshooting

### Check service registration

Open the Eureka dashboard:

```text
http://localhost:8761
```

Confirm the required service instances are registered and show as `UP`.

### Inspect service logs

```bash
docker compose logs -f <service-name>
```

Examples:

```bash
docker compose logs -f order-service
docker compose logs -f analytics-service
docker compose logs -f api-gateway
```

### Common issues

| Symptom | Checks |
|---|---|
| Gateway returns `503` | Confirm the target service is running and registered in Eureka; check the route service ID and gateway logs. |
| Feign reports no instances | Confirm the target service's `spring.application.name`, Eureka URL, registry registration, and client registry fetch. Wait for discovery to converge after startup. |
| Database connection fails | Check MySQL health, database existence, credentials, hostname, and container port. |
| Login cookie is not sent | Check `withCredentials`, CORS allowed origins/credentials, cookie domain, `SameSite`, `Secure`, and HTTP/HTTPS consistency. |
| Add to Cart redirects repeatedly | Check that login succeeds, the browser retains the cookie, and the gateway accepts the token. |
| Payment verification fails | Check Razorpay test credentials, order/payment identifiers, signature verification, and Payment Service logs. |
| Frontend cannot reach the API | Check the Vite API base URL, gateway port, CORS configuration, and browser network console. |

### Suggested manual acceptance test

- [ ] Open the Products page in a signed-out browser session.
- [ ] Confirm that products and categories load.
- [ ] Click **Add to Cart**.
- [ ] Confirm redirection to Login.
- [ ] Sign in with a customer account.
- [ ] Confirm the pending product is added to the cart.
- [ ] Refresh the page and confirm the authenticated session is still valid.
- [ ] Update the cart quantity and remove an item.
- [ ] Place an order and confirm it appears in order history.
- [ ] Test the payment flow using Razorpay test credentials.
- [ ] Sign in as an admin and verify admin-only routes.
- [ ] Try accessing another customer's cart/order and confirm access is denied.

## Security notes

- Never commit `.env`, database passwords, JWT signing secrets, internal API keys, or payment credentials.
- Use HTTPS in production.
- Use secure, HTTP-only cookies with an appropriate `SameSite` policy.
- Keep internal service endpoints inaccessible from the public internet.
- Validate authentication and resource ownership on the backend; do not rely only on React route guards.
- Validate product availability and price on the server during order creation. Do not trust prices sent by the browser.
- Consider CSRF protection when using cookie-based authentication.
- Rotate any credentials that have been included in logs or shared archives.

## Future improvements

Potential areas for further development:

- Add automated unit, integration, and end-to-end tests.
- Add service health checks and readiness-based startup dependencies.
- Add order/payment reconciliation and payment webhooks.
- Improve transactional consistency between order creation, inventory updates, and cart clearing.
- Add inventory reservation to reduce overselling.
- Add centralized logging, metrics, and distributed tracing.
- Add rate limiting and stronger internal-service authentication.
- Add production deployment configuration, HTTPS, domain routing, and secrets management.

---

## License

Add the license and copyright information applicable to this repository before publishing or distributing it.
