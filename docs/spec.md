# Journey Assistant: implemented MVP spec

## Purpose and scope
Journey Assistant tracks a traveller's location during a bus journey. When the traveller enters the configured destination geofence, the service can place an IVR call. The traveller can accept and receive a ride link, ask for another call in five minutes, or decline further calls for that journey. Outbound calls and messages require recorded notification consent.

The application is a Java 21 / Spring Boot 3 modular monolith using PostgreSQL, Flyway, Spring MVC, Spring Data JPA, and Actuator. Twilio adapters are implemented; fake adapters are the defaults. Local retry scheduling and a Cloud Tasks adapter are provided. Exotel is not currently implemented.

## Data model
All schema changes are versioned Flyway migrations (`V1` through `V6`). Hibernate uses schema validation.

| Table | Implemented data |
|---|---|
| `journeys` | UUID `id`; `traveller_phone`; destination latitude/longitude; geofence radius in metres; string `state`; nullable `arrived_at`; `notification_consent` (false by default); nullable `consent_timestamp`; `created_at`, `updated_at`. |
| `location_updates` | UUID id; journey foreign key; latitude, longitude, and supplied timestamp. |
| `notification_attempts` | UUID id; journey foreign key; attempt number; unique nullable provider call ID; status (`PENDING`, `IN_PROGRESS`, `COMPLETED`, `FAILED`); scheduled/started/completed times; last processed digit; invalid digit count; creation time. Journey/attempt number is unique. |
| `journey_messages` | UUID id; journey foreign key; channel (`SMS`, `WHATSAPP`); unique nullable provider message ID; status (`QUEUED`, `SENT`, `FAILED`, `UNDELIVERED`, `DELIVERED`, `READ`); creation/update times. Journey/channel is unique to prevent duplicate sends. |

## Journey lifecycle

| Current state | Event | Next state |
|---|---|---|
| `TRACKING` | `DESTINATION_REACHED` | `ARRIVED` |
| `ARRIVED` | `CALL_STARTED` | `CALLING` |
| `CALLING` | `ACCEPT` | `ACCEPTED` |
| `CALLING` | `RETRY` | `RETRY_SCHEDULED` |
| `CALLING` | `REJECT` | `DECLINED` |
| `RETRY_SCHEDULED` | `RETRY_DUE` | `CALLING` |
| `RETRY_SCHEDULED` or `CALLING` | `RETRIES_EXHAUSTED` | `NO_RESPONSE` |

All other transitions are invalid. `REJECTED` remains in the enum for compatibility with existing data; new digit-3 handling uses `DECLINED`. A journey is never called again after decline. The configured retry limit defaults to three retries after the initial attempt; exhausted attempts produce `NO_RESPONSE`.

## HTTP contracts

| Method and path | Behavior |
|---|---|
| `POST /api/v1/journeys` | Create a journey from `userPhone`, `destinationLatitude`, `destinationLongitude`, `geofenceRadiusMeters`, and optional `notificationConsent` (defaults false). |
| `POST /api/v1/journeys/{journeyId}/locations` | Record `latitude`, `longitude`, and `timestamp`; compute haversine distance and transition to arrival when within the radius. |
| `GET /api/v1/journeys/{journeyId}` | Return journey status. |
| `POST /webhooks/twilio/voice?journeyId={id}` | Return IVR TwiML with a one-digit Gather prompt; no input results in goodbye and hangup. |
| `POST /webhooks/twilio/gather?journeyId={id}` | Process form fields `Digits` and `CallSid`: 1 accepts, 2 schedules retry, 3 declines. Invalid input may be repeated once, then the call ends. |
| `POST /webhooks/twilio/status` | Update call attempt status; no-answer, busy, or failed schedules a retry unless the retry limit is exhausted. |
| `POST /webhooks/twilio/message-status` | Update delivery status monotonically and trigger WhatsApp-to-SMS fallback on failed, undelivered, or timeout. |
| `POST /webhooks/twilio/whatsapp/inbound` | Receive the configured inbound WhatsApp webhook. |
| `POST /internal/tasks/journeys/{id}/retry` | Internal scheduled retry callback; rechecks state and consent before calling. |
| `POST /internal/tasks/journeys/{id}/message-fallback` | Internal delayed callback for WhatsApp delivery fallback. |

Validation errors return 400, missing journeys 404, and invalid state transitions 409. Twilio webhooks validate `X-Twilio-Signature` against the full public URL and form parameters when Twilio is selected; invalid signatures return 403. Signature validation can be disabled only in the fake-provider setup.

## IVR and notification behavior
- IVR prompt: “You have reached your destination. Press 1 to get a ride link, press 2 to be called again in 5 minutes, press 3 to stop calls.” Gather timeout is eight seconds.
- The IVR provider is selected by `ivr.provider=fake|twilio`; fake is the default. Twilio credentials and sender configuration come from environment variables.
- Message channel provider is selected by `notification.provider=fake|twilio`; fake is the default. Strategy is `sms`, `whatsapp`, `whatsapp_then_sms`, or `both`.
- Accepted journeys use the content `Your ride link: {link}. Open it to confirm your ride.` The configured `RIDE_LINK` supplies the link.
- WhatsApp fallback sends SMS on failed/undelivered status or if no delivered status arrives before the configured timeout (60 seconds by default). Messages are limited to one per journey per channel. Status updates cannot downgrade a higher-ranked status.
- Retry and fallback jobs use Spring `TaskScheduler` with the `local` profile and Cloud Tasks with the `gcp` profile.
- Phone numbers must be masked in logs. Never log Twilio credentials or full sensitive provider configuration.

## Configuration and operations
Configuration is in `src/main/resources/application.yml` and is environment driven. Main variables include `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `IVR_PROVIDER`, `IVR_MAX_RETRIES`, `IVR_SIGNATURE_VALIDATION_ENABLED`, `NOTIFICATION_PROVIDER`, `NOTIFICATION_STRATEGY`, `WHATSAPP_FALLBACK_TIMEOUT`, `RIDE_LINK`, `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER`, `TWILIO_SMS_FROM`, `TWILIO_WHATSAPP_FROM`, `TWILIO_WHATSAPP_CONTENT_SID`, `PUBLIC_BASE_URL`, and `GCP_TASKS_*` / `GCP_PROJECT_ID`.

See [Twilio setup](./twilio-setup.md) for provider setup and [deployment guide](./deploy.md) for Cloud Run, Cloud SQL, Secret Manager, and Cloud Tasks configuration.

## Non-goals and deployment boundary
This MVP does not implement user authentication/authorization, Exotel, a production UI, or a guaranteed transactional outbox for provider delivery. Internal task endpoints must be protected by the deployment boundary; Cloud Run task OIDC configuration alone does not protect public API or webhook routes. See the deployment guide before exposing the service publicly.
