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
event         event metadata, image URL, sales policy
ticket        ticket issuance, purchase, ownership projection
resale        official resale listings and purchases
checkin       QR generation, verification, check-in records
admin         dashboard and operational summaries
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

## Configuration

Main config lives in `src/main/resources/application.yml`.

Important environment variables:

```text
JWT_SECRET
IMAGE_DIRECTORY
IMAGE_PUBLIC_URL_PREFIX
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

## Commit Rules

This repository uses intentionally small commits.

- Prefer one file per commit.
- Commit documentation separately.
- Commit Liquibase separately from Kotlin code.
- Do not push until requested.
- Read `AGENTS.md` before making changes.
