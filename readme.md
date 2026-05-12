# Card Authorization Service

A Spring Boot backend service for managing card authorization records. The project demonstrates maintainable layering, MSSQL persistence, Liquibase migration, transactional service methods, request/response logging, pagination, validation, and integration with an external API.

## Tech Stack

- Java 21
- Spring Boot 3.5.x
- Spring Web
- Spring Data JPA / Hibernate
- MSSQL Server
- Liquibase
- MapStruct
- Lombok
- Jakarta Bean Validation
- Maven
- Docker Compose

## Features

- Create card authorization record
- Update authorization status
- Retrieve authorization by ID
- Retrieve authorizations with default page size of 10 and configurable maximum page size
- Call a configurable external API through `RiskApiClient` for risk-check simulation
- Log request and response payloads into `logs/card-service.log`
- Mask configured sensitive fields in request/response logs
- Global exception handling
- Correlation ID support using `X-Correlation-Id`
- Liquibase-managed database schema

## Business Rules

- New authorization records are created with `PENDING` status.
- Risk check must be completed before an authorization can be approved.
- `HIGH` risk authorization cannot be approved.
- `APPROVED`, `DECLINED`, and `FAILED` are terminal statuses.
- Terminal authorizations cannot be updated again.

## Project Structure

```text
src/main/java/xyz/pakwo/cardservice
  ├── config
  ├── controller
  ├── dto
  ├── entity
  ├── exception
  ├── integration
  ├── logging
  ├── mapper
  ├── repository
  ├── service
  └── util
```

## Run MSSQL Locally

Start SQL Server and create `TESTDB`:

```bash
docker compose -f docker/db/docker-compose.yml up -d
```
The local database uses the official SQL Server 2022 container image for ease of setup. In production or controlled environments, the image tag should be pinned to an approved SQL Server build.

The compose file starts MSSQL and runs an initialization script to create `TESTDB`.

Default local credentials:

```text
DB URL: jdbc:sqlserver://localhost:1433;databaseName=TESTDB;encrypt=false;trustServerCertificate=true
Username: sa
Password: Dur1an!Super$
```
### Common Issue
If Docker returns a credential helper error such as `docker-credential-desktop: executable file not found`, back up and check `~/.docker/config.json`. If it contains `"credsStore": "desktop"` or `"credStore": "desktop"`, remove that entry and run Docker Compose again.

## Database Reset and Rollback

For this assessment project, the local MSSQL database is treated as disposable because it runs through Docker Compose. To keep local development simple, the database can be reset by removing the Docker volume:

```bash
docker compose -f docker/db/docker-compose.yml down -v
docker compose -f docker/db/docker-compose.yml up -d
```
The -v option removes the Docker volume used by MSSQL, so all local database data is deleted. On the next application startup, Liquibase applies the changelog files again and recreates the schema.

This approach is intentionally simple for local development and assessment review. In shared or production-like environments, database rollback should be handled through proper Liquibase rollback practices, such as rollback tags, reviewed rollback scripts, and rollback SQL preview before execution.


## Run Application

```bash
mvn clean spring-boot:run
```

Or:

```bash
mvn clean package
java -jar target/card-service-0.0.1-SNAPSHOT.jar
```

## Run Tests

```bash
mvn clean test
```

Tests use H2 in-memory database with Liquibase migration for portability.

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/authorizations` | Create authorization |
| GET | `/api/v1/authorizations/{id}` | Get authorization by ID |
| GET | `/api/v1/authorizations?page=0&size=10` | Get paginated authorizations, default 10 records/page |
| PUT | `/api/v1/authorizations/{id}` | Update authorization status |
| POST | `/api/v1/authorizations/{id}/risk-check` | Call external risk API and update risk result |
| GET | `/actuator/health` | Health check |

## Sample Create Request

```json
{
  "transactionReference": "TXN-20260512-0001",
  "cardNumber": "5454545454545454",
  "customerId": "CUST-001",
  "merchantName": "Pakwo Store",
  "amount": 120.50,
  "currency": "MYR"
}
```

## Request/Response Logging

The service logs HTTP request and response information into:

```text
logs/card-service.log
```

The log includes:

- HTTP method
- URI
- request body
- response body
- HTTP status
- duration
- correlation ID

Configured sensitive fields such as card number, CVV, PIN, password, and tokens are masked before being written to logs.

## External API Integration

`POST /api/v1/authorizations/{id}/risk-check` calls a configurable external API. By default, it uses:

```text
https://jsonplaceholder.typicode.com
```

The external URL can be overridden:

```bash
RISK_API_BASE_URL=http://localhost:9090 mvn spring-boot:run
```

## Postman Collection

Import this file into Postman:

```text
postman/Card Authorization Service.postman_collection.json
```

## Design Notes

- DTOs use Java records for immutable request/response models.
- JPA entity uses Lombok carefully with `@Getter`, `@Setter`, `@Builder`, and no `@Data`.
- MapStruct is used for compile-time DTO/entity mapping.
- Transaction boundaries are placed in the service layer.
- `GET` operations use `@Transactional(readOnly = true)`.
- External API integration is isolated behind `RiskApiClient`.
- Database schema is managed by Liquibase instead of relying on Hibernate auto-create.
- Liquibase migration files are organized by release version. The master changelog includes release-specific changelog files such as `release-1.0.0.yaml`, making future database changes easier to maintain.

### Identifier Strategy

The `CardAuthorization` entity uses `@GeneratedValue(strategy = GenerationType.IDENTITY)` because it is simple, database-native, and suitable for this assessment project using MSSQL.

For a high-throughput production system, the identifier strategy should be reviewed carefully. Depending on the scale and database design, alternatives such as database sequences, UUID/ULID, or Snowflake-style IDs may be preferred to improve scalability, batching, and distributed ID generation.

The API also uses `transactionReference` as a business-level idempotency key to prevent duplicate authorization records.

### API Identifier Design

For simplicity, this project uses the database-generated authorization ID in endpoints such as:
- `GET /api/v1/authorizations/{id}`
- `PUT /api/v1/authorizations/{id}`
- `POST /api/v1/authorizations/{id}/risk-check`

This keeps the API simple and easy to test with Postman.
For a production system, I would avoid exposing internal database identifiers directly to external clients. A business identifier such as `transactionReference` would be preferred for client-facing APIs because it is stable, meaningful to the requester, and avoids coupling external consumers to internal persistence design.

### Security Scope

The following production security controls are out of scope for this assessment version:
- API Gateway authentication
- OAuth2/JWT-based authorization
- mTLS for service-to-service communication
- RBAC for operational access
- Rate limiting and throttling
- Audit logging for sensitive operations
