# Deploy to Cloud Run

This repository deploys from `main` with GitHub Actions. The workflow runs Maven verification, builds and pushes the Docker image to Artifact Registry, then deploys it to Cloud Run using GitHub OIDC and Workload Identity Federation (WIF). It uses no service-account key file.

The first workflow run creates a bootstrap revision with the `local` profile so Cloud Run can assign the service URL. It then deploys a second revision with the `gcp` profile and that URL as `PUBLIC_BASE_URL`. Both revisions use fake IVR and messaging providers during bootstrap; the final providers are configured from GitHub repository variables.

## 1. Set project values

Choose one Google Cloud project and region for Cloud Run, Cloud SQL, Artifact Registry, and Cloud Tasks. Enable the APIs:

```bash
gcloud services enable \
  artifactregistry.googleapis.com \
  cloudresourcemanager.googleapis.com \
  cloudsqladmin.googleapis.com \
  cloudtasks.googleapis.com \
  iam.googleapis.com \
  iamcredentials.googleapis.com \
  run.googleapis.com \
  secretmanager.googleapis.com \
  sts.googleapis.com \
  serviceusage.googleapis.com \
  --project="$PROJECT_ID"
```

Create the Artifact Registry Docker repository:

```bash
gcloud artifacts repositories create journey-assistant \
  --repository-format=docker \
  --location="$REGION" \
  --project="$PROJECT_ID"
```

## 2. Create Cloud SQL for PostgreSQL

Create a PostgreSQL instance in the same region as Cloud Run. Cloud SQL can be created in the Console or with `gcloud sql instances create`; choose the machine tier, availability, backups, and maintenance settings that fit your environment. Create the database and a dedicated application user:

```bash
gcloud sql databases create journey_assistant \
  --instance="$CLOUD_SQL_INSTANCE_ID" \
  --project="$PROJECT_ID"

read -rsp 'Database user password: ' DATABASE_PASSWORD
printf '\n'
gcloud sql users create journey_app \
  --instance="$CLOUD_SQL_INSTANCE_ID" \
  --password="$DATABASE_PASSWORD" \
  --project="$PROJECT_ID"
```

`CLOUD_SQL_INSTANCE_ID` is the short instance name used by `gcloud sql`; `CLOUD_SQL_INSTANCE` below is the full connection name `PROJECT_ID:REGION:INSTANCE_ID`. The app connects using the Cloud SQL Java Connector via the PostgreSQL JDBC socket factory. The connection URL is:

```text
jdbc:postgresql:///journey_assistant?cloudSqlInstance=PROJECT_ID:REGION:INSTANCE_ID&socketFactory=com.google.cloud.sql.postgres.SocketFactory&cloudSqlRefreshStrategy=lazy
```

The workflow attaches the Cloud SQL instance to the Cloud Run service. Flyway applies migrations on application startup, and Hibernate validates the schema. Grant the Cloud Run runtime service account `roles/cloudsql.client` on the project:

```bash
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:$RUNTIME_SERVICE_ACCOUNT" \
  --role="roles/cloudsql.client"
```

The connector uses Application Default Credentials from the runtime service account; do not put a service-account key in the container or GitHub.

## 3. Store credentials in Secret Manager

Create secrets for the database password and Twilio credentials. The workflow pins Cloud Run environment variable references to secret version `1` by default; after rotation, update the matching GitHub repository variable to the new version number and deploy a new revision.

```bash
gcloud secrets create journey-db-password --replication-policy=automatic --project="$PROJECT_ID"
gcloud secrets create twilio-account-sid --replication-policy=automatic --project="$PROJECT_ID"
gcloud secrets create twilio-auth-token --replication-policy=automatic --project="$PROJECT_ID"

printf '%s' "$DATABASE_PASSWORD" | gcloud secrets versions add journey-db-password --data-file=- --project="$PROJECT_ID"
unset DATABASE_PASSWORD
```

Add Twilio values without putting them in source control:

```bash
read -rsp 'Twilio Account SID: ' TWILIO_ACCOUNT_SID
printf '\n'
printf '%s' "$TWILIO_ACCOUNT_SID" | gcloud secrets versions add twilio-account-sid --data-file=- --project="$PROJECT_ID"
unset TWILIO_ACCOUNT_SID

read -rsp 'Twilio Auth Token: ' TWILIO_AUTH_TOKEN
printf '\n'
printf '%s' "$TWILIO_AUTH_TOKEN" | gcloud secrets versions add twilio-auth-token --data-file=- --project="$PROJECT_ID"
unset TWILIO_AUTH_TOKEN
```

Grant the runtime service account access to each secret:

```bash
for SECRET in journey-db-password twilio-account-sid twilio-auth-token; do
  gcloud secrets add-iam-policy-binding "$SECRET" \
    --member="serviceAccount:$RUNTIME_SERVICE_ACCOUNT" \
    --role="roles/secretmanager.secretAccessor" \
    --project="$PROJECT_ID"
done
```

The deployment guide uses environment variable secret references, which Cloud Run resolves when each revision starts. Pin versions as shown rather than using `latest`; deploy a new revision after updating a secret version. See [Cloud Run secrets configuration](https://cloud.google.com/run/docs/configuring/services/secrets).

## 4. Create the Cloud Tasks queue and identities

Create the queue in the same project and location configured on the Cloud Run service:

```bash
gcloud tasks queues create journey-retries \
  --location="$REGION" \
  --project="$PROJECT_ID" \
  --max-attempts=5 \
  --max-retry-duration=3600s \
  --min-backoff=5s \
  --max-backoff=300s \
  --max-dispatches-per-second=10 \
  --max-concurrent-dispatches=10
```

The app service account enqueues tasks. Cloud Tasks uses a separate service account to sign OIDC tokens for the `/internal/tasks/**` target URLs:

```bash
gcloud iam service-accounts create journey-runtime \
  --display-name="Journey Assistant Cloud Run runtime" \
  --project="$PROJECT_ID"

gcloud iam service-accounts create journey-task-invoker \
  --display-name="Journey Assistant Cloud Tasks invoker" \
  --project="$PROJECT_ID"

RUNTIME_SERVICE_ACCOUNT="journey-runtime@${PROJECT_ID}.iam.gserviceaccount.com"
TASK_INVOKER_SERVICE_ACCOUNT="journey-task-invoker@${PROJECT_ID}.iam.gserviceaccount.com"

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:$RUNTIME_SERVICE_ACCOUNT" \
  --role="roles/cloudtasks.enqueuer"

gcloud iam service-accounts add-iam-policy-binding "$TASK_INVOKER_SERVICE_ACCOUNT" \
  --member="serviceAccount:$RUNTIME_SERVICE_ACCOUNT" \
  --role="roles/iam.serviceAccountUser" \
  --project="$PROJECT_ID"
```

After the initial Cloud Run service has been created, allow the task identity to invoke it. Cloud Tasks also needs permission to use the task identity when minting OIDC tokens:

```bash
gcloud run services add-iam-policy-binding "$CLOUD_RUN_SERVICE" \
  --region="$REGION" \
  --project="$PROJECT_ID" \
  --member="serviceAccount:$TASK_INVOKER_SERVICE_ACCOUNT" \
  --role="roles/run.invoker"

PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"
gcloud iam service-accounts add-iam-policy-binding "$TASK_INVOKER_SERVICE_ACCOUNT" \
  --member="serviceAccount:service-${PROJECT_NUMBER}@gcp-sa-cloudtasks.iam.gserviceaccount.com" \
  --role="roles/iam.serviceAccountUser" \
  --project="$PROJECT_ID"
```

Enable the Cloud Tasks API before creating the queue so its service agent is provisioned. The implementation sets an OIDC audience equal to `PUBLIC_BASE_URL`; the task target uses the Cloud Run service URL. See [Cloud Tasks HTTP targets and OIDC](https://cloud.google.com/tasks/docs/creating-http-target-tasks) and [queue configuration](https://cloud.google.com/tasks/docs/configuring-queues).

## 5. Configure GitHub and WIF

Create a deployer service account and a dedicated Workload Identity Pool and GitHub OIDC provider. Replace `OWNER/REPOSITORY` with the exact GitHub repository path. The provider condition restricts deployments to pushes on `main` from that repository.

```bash
gcloud iam service-accounts create journey-github-deployer \
  --display-name="Journey Assistant GitHub deployer" \
  --project="$PROJECT_ID"

gcloud iam workload-identity-pools create github-pool \
  --location=global \
  --display-name="GitHub Actions pool" \
  --project="$PROJECT_ID"

gcloud iam workload-identity-pools providers create-oidc github-provider \
  --location=global \
  --workload-identity-pool=github-pool \
  --issuer-uri="https://token.actions.githubusercontent.com/" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.ref=assertion.ref" \
  --attribute-condition="assertion.repository == 'OWNER/REPOSITORY' && assertion.ref == 'refs/heads/main'" \
  --project="$PROJECT_ID"

PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"
DEPLOYER="journey-github-deployer@${PROJECT_ID}.iam.gserviceaccount.com"
gcloud iam service-accounts add-iam-policy-binding "$DEPLOYER" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github-pool/attribute.repository/OWNER/REPOSITORY" \
  --project="$PROJECT_ID"

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:$DEPLOYER" --role="roles/run.admin"
gcloud artifacts repositories add-iam-policy-binding journey-assistant \
  --location="$REGION" --project="$PROJECT_ID" \
  --member="serviceAccount:$DEPLOYER" --role="roles/artifactregistry.writer"
gcloud iam service-accounts add-iam-policy-binding "$RUNTIME_SERVICE_ACCOUNT" \
  --project="$PROJECT_ID" --member="serviceAccount:$DEPLOYER" \
  --role="roles/iam.serviceAccountUser"
gcloud iam service-accounts add-iam-policy-binding "$TASK_INVOKER_SERVICE_ACCOUNT" \
  --project="$PROJECT_ID" --member="serviceAccount:$DEPLOYER" \
  --role="roles/iam.serviceAccountUser"
```

The deployer also needs `roles/iam.serviceAccountUser` on the Cloud Tasks invoker service account because the workflow configures the Cloud Run service identity and task identity. Add that binding using the same command above with `$TASK_INVOKER_SERVICE_ACCOUNT` as the target. These roles are for deployment only; the Cloud Run runtime identity has separate, narrower permissions.

In **GitHub → Settings → Secrets and variables → Actions → Variables**, define:

| Variable | Example / meaning |
| --- | --- |
| `GCP_PROJECT_ID` | Google Cloud project ID |
| `GCP_REGION` | Shared Cloud Run / Artifact Registry / Cloud Tasks region |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | `projects/PROJECT_NUMBER/locations/global/workloadIdentityPools/github-pool/providers/github-provider` |
| `GCP_DEPLOY_SERVICE_ACCOUNT` | Deployer service account email |
| `CLOUD_RUN_SERVICE` | `journey-assistant` |
| `ARTIFACT_REGISTRY_REPOSITORY` | `journey-assistant` |
| `CLOUD_RUN_RUNTIME_SERVICE_ACCOUNT` | Runtime service account email |
| `CLOUD_SQL_INSTANCE` | Full `PROJECT_ID:REGION:INSTANCE_ID` connection name |
| `DATABASE_NAME` | `journey_assistant` |
| `DATABASE_USERNAME` | `journey_app` |
| `CLOUD_TASKS_LOCATION` | Cloud Tasks queue region |
| `CLOUD_TASKS_QUEUE` | `journey-retries` |
| `CLOUD_TASKS_INVOKER_SERVICE_ACCOUNT` | Task invoker service account email |
| `DB_PASSWORD_SECRET_VERSION` | Secret version number, initially `1` |
| `TWILIO_SID_SECRET_VERSION` | Secret version number, initially `1` |
| `TWILIO_TOKEN_SECRET_VERSION` | Secret version number, initially `1` |
| `IVR_PROVIDER` | `fake` or `twilio` |
| `NOTIFICATION_PROVIDER` | `fake` or `twilio` |
| `NOTIFICATION_STRATEGY` | `sms`, `whatsapp`, `whatsapp_then_sms`, or `both` |
| `RIDE_LINK` | Ride confirmation URL |
| `TWILIO_FROM_NUMBER` | Twilio voice caller ID, if IVR provider is Twilio |
| `TWILIO_SMS_FROM` | Twilio SMS sender, if messaging provider is Twilio |
| `TWILIO_WHATSAPP_FROM` | WhatsApp sender, if WhatsApp is configured |
| `TWILIO_WHATSAPP_CONTENT_SID` | Optional approved WhatsApp template SID |

The `PUBLIC_BASE_URL` is discovered by the workflow after its bootstrap deploy, then supplied to the final revision. Configure Twilio signature validation as enabled; the app rejects disabled validation when either provider is Twilio.

The WIF commands and action permissions follow [Google's deployment pipeline setup](https://cloud.google.com/iam/docs/workload-identity-federation-with-deployment-pipelines) and the [Google auth action instructions](https://github.com/google-github-actions/auth).

## 6. Deploy and verify

Push to `main` or use **Actions → Test, build, and deploy → Run workflow** on `main`. The deploy job runs only after `mvn verify` passes. It pushes an image tagged with the commit SHA and deploys it to Cloud Run.

After deployment, check:

```bash
gcloud run services describe "$CLOUD_RUN_SERVICE" \
  --region="$REGION" --project="$PROJECT_ID" \
  --format='value(status.url)'

gcloud run services logs read "$CLOUD_RUN_SERVICE" \
  --region="$REGION" --project="$PROJECT_ID" --limit=100
```

The workflow currently deploys with `--allow-unauthenticated` so Twilio can reach its signed webhooks. Twilio webhooks are protected by signature validation, but this application does not yet enforce authentication on the general REST API or the internal Cloud Tasks routes. Cloud Tasks includes OIDC tokens, but those tokens are not checked by the application while Cloud Run allows unauthenticated access. Add application-level authentication or a separate protected task service before using sensitive production data.

