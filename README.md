# Trust Ticket Backend

Backend API for the Kyunghee blockchain ticketing project. The service manages off-chain data such as users, organizer approval, event metadata, images, search, admin views, QR image generation, and operational dashboards while delegating trusted ticket ownership, resale, and check-in state to the `TrustTicket` smart contract.

## Stack

- Spring Boot 4.0.6
- Kotlin 2.2.21
- Gradle Kotlin DSL
- JDK 26 toolchain with JVM 24 bytecode target
- PostgreSQL
- Liquibase
- Spring Security + JWT
- MapStruct
- springdoc-openapi / Swagger UI
- Web3j
- ZXing for QR images
- Docker Compose for local infrastructure

## Architecture

All request flows follow:

```text
REST controller -> facade -> service -> repository
```

Layer responsibilities:

- `controller`: HTTP endpoints, validation annotations, Swagger annotations, request/response models.
- `facade`: use-case orchestration and conversion between API models and service DTOs.
- `service`: business rules, transactions, entity-to-DTO conversion, integration decisions.
- `repository`: JPA persistence only.
- `entity`: JPA entities only.
- `dto`: service DTOs and commands only.
- `mapper`: MapStruct mappers.

Entities must not leave the service layer. Controllers and facades should never expose or accept entities.

## Domain Packages

```text
auth          wallet login, email login, JWT issuance
user          user profiles, roles, admin user management
organizer     organizer applications and approvals
event         event metadata, image URL, sales policy, event validators
ticket        ticket issuance, purchase, ownership projection
resale        official resale listings, purchases, transaction history
checkin       QR/barcode generation, signing hash lookup, verification, check-in records
dispute       resale/ticket dispute reports and review workflow
admin         dashboard, event supervision, users, disputes, operational summaries
blockchain    TrustTicket contract gateway and transaction log
common        shared API envelope, error handling, security, config
```

Each domain keeps request, response, DTO, entity, facade, mapper, repository, and service classes in dedicated directories.

## Local Launch

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Optional local EVM chain:

```bash
docker compose --profile chain up -d anvil
```

Run the backend:

```bash
./gradlew bootRun
```

Useful URLs:

- API base: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`

Local database defaults:

```text
host: localhost
port: 5432
database: blockchain_backend
user: blockchain
password: blockchain
```

Docker Compose uses an explicit project name and volume name so it does not share the `postgres-data` volume with other `Backend` folders. If host ports conflict, override them:

```bash
BLOCKCHAIN_POSTGRES_PORT=55432 BLOCKCHAIN_ANVIL_PORT=18545 docker compose up -d
```

## Configuration

Main config lives in `src/main/resources/application.yml`.

Important environment variables:

```text
JWT_SECRET
IMAGE_DIRECTORY
IMAGE_PUBLIC_URL_PREFIX
BLOCKCHAIN_POSTGRES_PORT
BLOCKCHAIN_ANVIL_PORT
BLOCKCHAIN_RPC_URL
BLOCKCHAIN_CHAIN_ID
TRUST_TICKET_CONTRACT_ADDRESS
BLOCKCHAIN_OPERATOR_PRIVATE_KEY
BLOCKCHAIN_GAS_PRICE_WEI
BLOCKCHAIN_GAS_LIMIT
```

Blockchain submission is disabled by default:

```yaml
app:
  blockchain:
    enabled: false
```

When disabled, the backend records simulated transaction hashes so API development can continue without funded wallets. For real contract calls, set `app.blockchain.enabled=true` and provide RPC, chain ID, contract address, and operator private key.

Recommended development chain order:

1. Local Anvil for free local testing.
2. Public testnet such as Base Sepolia or Polygon Amoy for team integration.
3. Production network only after contract, API, and operational flows are stable.

## Database

Liquibase is the source of truth for schema changes.

```text
src/main/resources/db/changelog/db.changelog-master.yml
src/main/resources/db/changelog/*.yml
```

Hibernate is configured with `ddl-auto: validate`, so schema drift should fail fast instead of silently mutating tables.

## API Response Shape

Success responses are wrapped:

```json
{
  "success": true,
  "status": 200,
  "code": "OK",
  "message": "OK",
  "data": {},
  "meta": {
    "requestId": "uuid",
    "timestamp": "2026-05-12T00:00:00Z"
  }
}
```

Error responses are also standardized:

```json
{
  "success": false,
  "status": 400,
  "code": "INVALID_REQUEST",
  "message": "요청 값이 올바르지 않습니다.",
  "path": "/api/v1/example",
  "errors": [],
  "stackTrace": [],
  "meta": {
    "requestId": "uuid",
    "timestamp": "2026-05-12T00:00:00Z"
  }
}
```

## API Overview

Authentication:

```text
POST /api/v1/auth/wallet/nonce
POST /api/v1/auth/wallet/login
POST /api/v1/auth/email/register
POST /api/v1/auth/email/login
```

Users:

```text
GET   /api/v1/users/me
PATCH /api/v1/users/me
GET   /api/v1/users
PATCH /api/v1/users/{userId}/suspend
PATCH /api/v1/users/{userId}/activate
PATCH /api/v1/users/{userId}/delete
PATCH /api/v1/users/{userId}/validator
```

Organizer approval:

```text
POST  /api/v1/organizer-applications
GET   /api/v1/organizer-applications/me
GET   /api/v1/organizer-applications
PATCH /api/v1/organizer-applications/{applicationId}/review
```

Events and tickets:

```text
GET   /api/v1/events
GET   /api/v1/events/{eventId}
GET   /api/v1/events/me
POST  /api/v1/events
PATCH /api/v1/events/{eventId}
PATCH /api/v1/events/{eventId}/status
PATCH /api/v1/events/{eventId}/resale-policy
POST  /api/v1/events/{eventId}/image
POST  /api/v1/events/{eventId}/validators
GET   /api/v1/events/{eventId}/validators
POST  /api/v1/events/{eventId}/tickets
GET   /api/v1/events/{eventId}/tickets
GET   /api/v1/tickets/me
GET   /api/v1/tickets/{ticketId}
GET   /api/v1/tickets/{ticketId}/validity
GET   /api/v1/wallets/{walletAddress}/tickets
POST  /api/v1/tickets/{ticketId}/purchase
```

Resale and check-in:

```text
GET   /api/v1/resale-listings
GET   /api/v1/resale-listings/{listingId}
POST  /api/v1/tickets/{ticketId}/resale-listing
POST  /api/v1/resale-listings/{listingId}/purchase
PATCH /api/v1/resale-listings/{listingId}/cancel
POST  /api/v1/tickets/{ticketId}/qr
GET   /api/v1/tickets/{ticketId}/check-in-message
POST  /api/v1/check-ins
GET   /api/v1/tickets/{ticketId}/check-ins
POST  /api/v1/disputes
GET   /api/v1/disputes/me
```

Admin:

```text
GET   /api/v1/admin/dashboard
GET   /api/v1/admin/blockchain-transactions
GET   /api/v1/admin/events
PATCH /api/v1/admin/events/{eventId}/flag
PATCH /api/v1/admin/events/{eventId}/unflag
GET   /api/v1/admin/resale-transactions
GET   /api/v1/admin/disputes
PATCH /api/v1/admin/disputes/{disputeId}/review
```

Detailed Korean endpoint descriptions are available in Swagger UI.

## Build And Test

Compile:

```bash
./gradlew compileKotlin
```

Run tests:

```bash
./gradlew test
```

Full verification:

```bash
./gradlew clean test
```

Tests use the `test` Spring profile with H2 in PostgreSQL compatibility mode. Local application runtime still uses PostgreSQL from Docker Compose.

## Commit Rules

This repository uses intentionally small commits.

- Prefer one file per commit.
- Commit documentation separately.
- Commit Liquibase separately from Kotlin code.
- Do not push until requested.
- Read `AGENTS.md` before making changes.
