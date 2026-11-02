# Coding agent guide

## Project purpose
- Journey Assistant tracks a traveller's bus journey and initiates an automated IVR call when the traveller reaches the destination geofence.
- IVR choices accept (send an SMS with a Rapido deep link), retry in five minutes, or reject calls for that journey.
- This repository is a Java modular monolith for the MVP; external integrations include Exotel, Cloud Tasks, and SMS delivery.

## Technology
- Java 21, Maven, Spring Boot 3, PostgreSQL, Flyway, Docker, and GCP Cloud Run.
- Use Spring MVC, Bean Validation, Spring Data JPA, and Actuator where appropriate.

## Package structure
- Root package: `com.journeyassistant`.
- `journey`: journey lifecycle and domain model.
- `location`: location updates and geofence handling.
- `notification`: IVR/SMS orchestration and notification state.
- `integration.exotel`: Exotel client and webhook adapters.
- `integration.cloudtasks`: scheduled retry adapter.
- `api`: REST controllers, request/response DTOs, and exception handling.
- `config`: application configuration and cross-cutting setup.

## Coding conventions
- Use constructor injection; avoid field injection.
- Use Java records for API request and response DTOs when suitable.
- Keep business logic out of controllers; delegate to application services.
- Put all database schema changes in versioned Flyway migrations. Never rely on Hibernate schema generation in production.
- Keep provider-specific code behind integration adapters.
- Never hardcode secrets. Read credentials and sensitive configuration from environment variables.

## Testing
- Use JUnit 5 and Mockito for unit tests.
- Use Testcontainers with PostgreSQL for database integration tests.
- Keep tests deterministic and avoid calls to real external providers.
