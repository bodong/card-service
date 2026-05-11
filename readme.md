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
- Retrieve authorizations with fixed page size of 10
- Call external risk API through `RiskApiClient`
- Log request and response payloads into `logs/card-service.log`
- Mask card numbers in request/response logs
- Global exception handling
- Correlation ID support using `X-Correlation-Id`
- Liquibase-managed database schema

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
  └── service
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
When encounter `failed to register layer: sync /var/lib/docker/image/overlay2/layerdb/tmp/write-set-1646617768/diff: input/output error`
Check your docker/config json file, example `vi ~/.docker/config.json`, ensure to back up first, if containing `"credStore": "desktop"`, then remove it and run the docker compose again. 

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
| GET | `/api/v1/authorizations?page=0` | Get paginated authorizations, 10 records/page |
| PUT | `/api/v1/authorizations/{id}` | Update authorization status |
| POST | `/api/v1/authorizations/{id}/risk-check` | Call external risk API and update risk result |
| GET | `/actuator/health` | Health check |

## Sample Create Request

```json
{
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

Sensitive card numbers are masked before being written to logs.

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
postman/Card-Service.postman_collection.json
```

## Design Notes

- DTOs use Java records for immutable request/response models.
- JPA entity uses Lombok carefully with `@Getter`, `@Setter`, `@Builder`, and no `@Data`.
- MapStruct is used for compile-time DTO/entity mapping.
- Transaction boundaries are placed in the service layer.
- `GET` operations use `@Transactional(readOnly = true)`.
- External API integration is isolated behind `RiskApiClient`.
- Database schema is managed by Liquibase instead of relying on Hibernate auto-create.
