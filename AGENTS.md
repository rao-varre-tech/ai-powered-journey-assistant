# Coding agent guide

## Project purpose
Journey Assistant tracks a traveller's location, detects arrival at a destination geofence, and starts an automated IVR flow. A traveller can accept, request a retry, or decline. Acceptance sends a ride-link notification by SMS, WhatsApp, or both, subject to recorded notification consent.

This is a Java modular monolith. The implemented call and messaging provider is Twilio, with fake adapters as the default. Local retries use Spring scheduling; the `gcp` profile uses Cloud Tasks. Exotel is not implemented.

## Technology and structure
- Java 21, Maven, Spring Boot 3, PostgreSQL, Flyway, Docker, and GCP Cloud Run.
- Root package: `com.journeyassistant`.
- `journey`: journey entity, lifecycle, state machine, and persistence.
- `location`: location updates, geofence distance, arrival handling.
- `notification`: IVR, message channels, retry/fallback orchestration, and persistence.
- `api`: REST and Twilio webhook controllers, DTOs, and exception handling.
- `config`: Spring/provider/scheduler configuration.
- `integration.cloudtasks`: Cloud Tasks adapter package (provider implementation is currently under `notification`).
- `integration.exotel`: reserved package only; no Exotel adapter is implemented.

## Implemented behavior to preserve
- Journey states are `TRACKING`, `ARRIVED`, `CALLING`, `RETRY_SCHEDULED`, `ACCEPTED`, `DECLINED`, `NO_RESPONSE`, and legacy `REJECTED`. Keep valid transitions in `JourneyStateMachine`.
- A location update computes haversine distance. Arrival triggers an IVR call only when consent is true.
- IVR choices are 1 (accept and notify), 2 (retry in five minutes), and 3 (decline). Retry limit defaults to three retries per journey; exhausted retries end in `NO_RESPONSE`.
- Fake IVR and messaging adapters are defaults. Twilio webhooks require signature validation when Twilio is selected. Do not log credentials, signatures, full phone numbers, or message bodies.
- Consent gates outbound calls and messages. Keep webhook handling idempotent and preserve final journey states. Use transactions and existing locking around state changes and scheduled retries.
- Database schema is managed only by versioned Flyway migrations; Hibernate is configured to validate.

## Coding conventions
- Use constructor injection and keep business logic in services/domain classes, not controllers.
- Use records for API DTOs where practical and Bean Validation for request constraints.
- Keep provider-specific code behind interfaces/adapters and never hardcode secrets.
- Mask phone numbers in logs, showing at most the last four digits.
- Update `docs/spec.md`, `docs/twilio-setup.md`, or `docs/deploy.md` when changing their documented contracts or operations.

## Testing
- Use JUnit 5 and Mockito for deterministic unit tests; do not call real providers.
- Use MockMvc for HTTP/webhook behavior and Testcontainers PostgreSQL for persistence integration tests.
- Run the relevant tests and `mvn clean verify` when validating code changes. Keep Docker/GCP-dependent checks opt-in and document required local services.
