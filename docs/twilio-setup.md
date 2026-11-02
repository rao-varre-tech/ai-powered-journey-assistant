# Twilio trial setup and manual IVR test

## Configure a trial account

1. Create a Twilio account and confirm the account email and owner phone. Copy the Account SID and Auth Token from the Console project dashboard into your local environment; do not commit them.
2. Add your personal phone under **Phone Numbers → Manage → Verified Caller IDs** and complete Twilio's verification. Trial accounts can call only verified destination numbers.
3. In the Console, open **Phone Numbers → Manage → Buy a number** (or the trial Console's **Get a number** flow), choose a voice-capable number in a supported country, and provision it. Trial account rules and availability vary by country; the Console shows eligible numbers and any trial restrictions.
4. Set `TWILIO_FROM_NUMBER` to that Twilio number in E.164 format and `TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` to the account credentials.

See [Twilio's trial account guide](https://www.twilio.com/docs/usage/tutorials/how-to-use-your-free-trial-account), [verified phone instructions](https://help.twilio.com/hc/en-us/articles/223180048-How-to-Add-and-Remove-a-Verified-Phone-Number-or-Caller-ID-with-Twilio), and [trial limitations](https://help.twilio.com/articles/360036052753-Twilio-Free-Trial-Limitations).

## Expose localhost

Run the application locally, then expose its HTTP port with ngrok:

```powershell
ngrok http 8080
```

Copy the HTTPS forwarding origin (for example `https://example.ngrok-free.app`) and set `PUBLIC_BASE_URL` to that origin with no trailing slash. Twilio needs the public URL to reach the local webhook and to validate request signatures. Also set `IVR_PROVIDER=twilio` and `IVR_SIGNATURE_VALIDATION_ENABLED=true`. For local-only fake calls, leave the defaults `IVR_PROVIDER=fake` and signature validation disabled.

## Manual test checklist

- Start Postgres and the application with Flyway migrations enabled.
- Confirm the ngrok HTTPS URL points to the running application and is the exact `PUBLIC_BASE_URL` value.
- Create a journey with your verified phone number, destination coordinates, and a geofence radius.
- Send a location update inside the destination radius and confirm Twilio calls the verified phone.
- Press **1** and confirm the call says a ride link SMS will follow and hangs up; check the journey becomes `ACCEPTED`.
- Repeat with a new journey and press **2**; check the state becomes `RETRY_SCHEDULED`. A scheduler must trigger the due retry for the follow-up call; this IVR change does not add Cloud Tasks dispatch.
- Repeat with a new journey and press **3**; check the state becomes `DECLINED` and no later retry can place a call.
- Let a call time out, then inspect the status callback and call attempt record. Confirm no-answer is recorded and a retry is scheduled only while under `IVR_MAX_ATTEMPTS`.
- Send one invalid digit and confirm the prompt repeats once; send another invalid digit and confirm the call says goodbye and hangs up.

## Implementation assumptions

`docs/spec.md` does not define a retry limit, so `IVR_MAX_ATTEMPTS` defaults to 3 total outbound calls and can be changed by environment configuration. The five-minute retry scheduler is outside this change; option 2 records `RETRY_SCHEDULED`, but a Cloud Tasks adapter must dispatch the later call. The Twilio Java 13.1.0 API was checked against its source: `Call.creator(...)` returns top-level `CallCreator`, and TwiML builders use `VoiceResponse`, `Gather`, `Say`, and `Hangup`. The implementation calls `create(TwilioRestClient)` so its client can be mocked in tests; it makes no network call in tests.
