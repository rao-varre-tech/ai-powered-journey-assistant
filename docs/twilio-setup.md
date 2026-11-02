# Twilio trial setup and manual IVR and messaging test

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

## WhatsApp sandbox

1. In the Twilio Console, open **Messaging → Try it out → Send a WhatsApp message** (the exact menu label can vary) and activate the sandbox.
2. Join the sandbox from your phone by scanning its QR code or sending the displayed `join <your sandbox code>` message to the sandbox WhatsApp number. The phone must be WhatsApp-enabled. Trial restrictions apply.
3. In the sandbox configuration, set **When a message comes in** to `https://<your-ngrok-host>/webhooks/twilio/whatsapp/inbound` using POST. Set the message status callback to `https://<your-ngrok-host>/webhooks/twilio/message-status` if the Console exposes that field for the sandbox sender.
4. Configure `NOTIFICATION_PROVIDER=twilio`, `NOTIFICATION_STRATEGY=whatsapp` (or `whatsapp_then_sms` / `both`), `TWILIO_WHATSAPP_FROM` to the sandbox sender number, and `TWILIO_SMS_FROM` to your Twilio SMS-capable number. Use the `whatsapp:` prefix in `TWILIO_WHATSAPP_FROM`, for example `whatsapp:+14155238886`. Set `RIDE_LINK` to the ride URL. Keep the account SID and auth token in environment variables.
5. Create a journey with `notificationConsent: true`; messaging and calling are blocked when consent is false. For `whatsapp_then_sms`, set `WHATSAPP_FALLBACK_TIMEOUT` to a duration such as `60s` if you want to change the default.

The sandbox uses its own sender and does not support your own approved templates. For production, register a WhatsApp Business sender, submit the message in Twilio's Content Template Builder, wait for approval, then set `TWILIO_WHATSAPP_CONTENT_SID`. The template must include a numbered `{{1}}` variable for the ride link, such as `Your ride link: {{1}}. Open it to confirm your ride.`

See [Twilio's WhatsApp quickstart](https://www.twilio.com/docs/whatsapp/quickstart), [sandbox guide](https://www.twilio.com/docs/whatsapp/sandbox), and [template messaging guide](https://www.twilio.com/docs/whatsapp/tutorial/send-whatsapp-notification-messages-templates).

## Manual test checklist

- Start Postgres and the application with Flyway migrations enabled.
- Confirm the ngrok HTTPS URL points to the running application and is the exact `PUBLIC_BASE_URL` value.
- Create a journey with your verified phone number, destination coordinates, a geofence radius, and `notificationConsent: true`.
- Send a location update inside the destination radius and confirm Twilio calls the verified phone.
- Press **1** and confirm the journey becomes `ACCEPTED` and the configured notification strategy sends the ride link. Check Twilio's message status callback and verify WhatsApp-first sends SMS only after failure, undelivered status, or the configured timeout.
- Repeat with a new journey and press **2**; check the state becomes `RETRY_SCHEDULED`, then verify a follow-up call is placed after five minutes by the active scheduler profile.
- Repeat with a new journey and press **3**; check the state becomes `DECLINED` and no later retry can place a call.
- Let a call time out, then inspect the status callback and call attempt record. Confirm no-answer is recorded and a retry is scheduled only while under `IVR_MAX_RETRIES`; after three retries the journey becomes `NO_RESPONSE`.
- Send one invalid digit and confirm the prompt repeats once; send another invalid digit and confirm the call says goodbye and hangs up.

Set `NOTIFICATION_PROVIDER=fake` for local messaging without Twilio. To use Twilio, set `NOTIFICATION_PROVIDER=twilio`, `NOTIFICATION_STRATEGY=sms|whatsapp|whatsapp_then_sms|both`, `TWILIO_SMS_FROM`, and `TWILIO_WHATSAPP_FROM` as needed. WhatsApp free-form messages have a customer-service window; outside it, use an approved Content SID. The implementation supplies the ride link as template variable `{{1}}`.

## Implementation assumptions

The retry limit is three retries per journey (up to four calls including the first call). `IVR_MAX_RETRIES` overrides the default. The `local` Spring profile schedules five-minute retries in process; the `gcp` profile creates Cloud Tasks HTTP tasks. For GCP set `GCP_PROJECT_ID`, `GCP_TASKS_LOCATION`, `GCP_TASKS_QUEUE`, and `GCP_TASKS_INVOKER_SERVICE_ACCOUNT`, plus `PUBLIC_BASE_URL`, and deploy Cloud Run with unauthenticated access disabled. Grant the configured service account permission to invoke the service; Cloud Tasks uses an OIDC token for `/internal/tasks/journeys/{id}/retry`. Both schedulers re-check the journey state before placing another call. Tests mock the Cloud Tasks and Twilio clients and make no external calls.

The Twilio Java 13.1.0 API was checked against its source and examples: `Call.creator(...)` returns top-level `CallCreator`; TwiML builders use `VoiceResponse`, `Gather`, `Say`, and `Hangup`; and `Message.creator(...)` supports a null body with `setContentSid(...)`, `setContentVariables(...)`, and `setStatusCallback(...)`. The implementation calls `create(TwilioRestClient)` so its client can be mocked in tests. Template configuration assumes the approved content uses numbered variable `{{1}}` for the ride link.
