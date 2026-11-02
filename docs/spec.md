I'm a Java/Spring Boot developer. I'm building an MVP called "Journey Assistant".

Flow: a traveller's bus journey is tracked via their phone's location. When they
reach the destination (geofence), the system places an automated IVR call. The
person presses 1 = accept (send an SMS with a Rapido deep link), 2 = call again
after 5 minutes, 3 = reject and never call again for this journey.

Stack: Java 21, Spring Boot 3, PostgreSQL, Flyway, Docker, deployed on GCP Cloud Run.
Cloud Tasks for the 5-minute retry. IVR provider: Exotel (India).

Produce:
1. A one-page spec with requirements and non-goals
2. The data model (tables, columns, types)
3. The journey state machine (states, events, transitions) as a table
4. REST endpoints and webhook contracts
5. A package structure for a modular monolith
Keep it concise and implementation-ready.