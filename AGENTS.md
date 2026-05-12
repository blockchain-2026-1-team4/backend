# AGENTS.md

This file is the working contract for agents contributing to this repository. Keep it updated whenever architecture, tooling, or collaboration rules change.

## Project Identity

- Repository: `blockchain-2026-1-team4/backend`
- Domain: blockchain ticketing backend for event creation, NFT ticket issuance, primary sale, resale, QR generation, check-in, organizer approval, and admin management.
- Stack: Spring Boot 4.0.6, Kotlin 2.2.21, Gradle Kotlin DSL, PostgreSQL, Liquibase, Spring Security JWT, MapStruct, springdoc-openapi, Web3j, ZXing.
- Runtime target: JDK 26 toolchain. Kotlin currently supports JVM bytecode up to 24, so Gradle compiles Java/Kotlin to JVM 24 until Kotlin supports a higher target.

## Required Architecture

Every API flow must follow this direction:

```text
REST controller -> facade -> service -> repository
```

Rules:

- Controllers only accept request objects and return response objects.
- Facades orchestrate use cases and translate between controller request/response models and service DTO/command models.
- Services own business rules and transactions.
- Repositories are the only layer that directly talks to JPA.
- Entities must never cross above the service layer.
- Repository results must be converted to DTOs inside the service layer as soon as practical.
- Use MapStruct for entity-to-DTO and DTO-to-response mapping.
- Do not manually map large models unless MapStruct is genuinely unsuitable.

## Package Layout

Each domain should use this structure:

```text
<domain>/
  controller/
    request/
    response/
  dto/
  entity/
  facade/
  mapper/
  repository/
  service/
```

Dedicated model directories are mandatory:

- Request classes live only under `controller/request`.
- Response classes live only under `controller/response`.
- DTOs and commands live only under `dto`.
- Do not place request, response, or DTO classes directly in controller, facade, service, repository, or entity packages.

Shared infrastructure belongs under `common/`. Blockchain adapter code belongs under `blockchain/`.

## API Standards

- All API responses must be wrapped in the standardized success/error envelope.
- Success responses include status, code, message, data, and metadata.
- Error responses include status, code, message, path, field errors, stack trace excerpt when enabled, and metadata.
- Add concise Korean Swagger documentation to every API using `@Operation`.
- Use `@Tag` for controller groups.
- Keep Swagger text brief, compact, and explanatory.
- Prefer RESTful resource paths under `/api/v1`.

## Database Standards

- PostgreSQL is the local and default database.
- Docker Compose must launch local PostgreSQL.
- Liquibase is the source of truth for ERD/table changes.
- Do not rely on Hibernate DDL generation except `ddl-auto: validate`.
- Every table/index/constraint change must be represented in a Liquibase changelog.

## Blockchain Standards

- Backend submits blockchain transactions.
- Local development defaults to the no-op/simulated gateway unless `app.blockchain.enabled=true`.
- Use local Anvil for free local chain testing.
- Keep network, RPC URL, chain ID, contract address, gas settings, and operator private key configurable.
- Never hard-code private keys or production RPC credentials.

## Security Standards

- Primary login is wallet login.
- Email/password login is secondary.
- Use JWT for API authentication.
- Roles are `USER`, `ORGANIZER`, `ADMIN`, and `VALIDATOR`.
- Protect admin and organizer APIs with method security.

## Code Style

- Prefer small, explicit Kotlin classes over clever abstractions.
- Keep functions short and intention-revealing.
- Use constructor injection only.
- Use `@Transactional` on service methods, not controllers or facades.
- Keep comments rare and useful.
- Use UTC timestamps with `Instant`.
- Use `BigInteger` for wei, token IDs, event IDs, and other on-chain integer values.
- Keep Korean API descriptions polished and concise.

## Commit Discipline

The user explicitly requires highly sharded commits.

- Default to one file per commit.
- Only bundle files when they are inseparable, such as a tiny request/response pair or a generated mechanical pair.
- Never create broad commits like "add backend foundation" when many unrelated files are involved.
- Before committing, inspect `git status --short`.
- Commit documentation updates separately from code changes.
- Commit Liquibase changes separately from Kotlin code.
- Do not push until the user asks or approves.
- If a commit accidentally bundles too much and it has not been pushed, stop and split it before continuing.

## Collaboration Behavior

- Read this file before making changes.
- Keep this file and `README.md` updated as the project evolves.
- Protect user changes. Do not revert or overwrite work you did not make.
- If the worktree is dirty, inspect it and work around unrelated changes.
- Prefer implementing requested changes end to end, then verify with build/tests.
- Mention any verification that could not be run.
