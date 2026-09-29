# Deploy the Backend to Render

This guide deploys the Spring Boot API as a Docker web service with Supabase PostgreSQL and Upstash Redis. Kafka is optional for the initial deployment and can be enabled later with Confluent Cloud. Keep connected services in nearby regions to reduce latency.

## Before You Start

1. Rotate any credentials that have been pasted into chat or committed accidentally. Do not reuse exposed database, Redis, JWT, provider, or API-key values.
2. Confirm that Supabase PostgreSQL and Upstash Redis are available and that you have their current connection details. A Kafka cluster is not needed for the initial deployment.
3. Push the backend changes to the Git provider connected to Render. The backend `.env` file is excluded from the Docker build; Render will receive configuration through its Environment settings.

## Create the Render Service

1. In Render, choose **New** → **Web Service**, then connect the repository containing this project.
2. Set **Root Directory** to `backend`.
3. Select **Docker** as the runtime. With `backend` as the root directory, Render uses the `Dockerfile` there (repository path `backend/Dockerfile`); leave the build and start commands blank.
4. Choose a region near the database, Redis, and Kafka services. The current Supabase project is in Tokyo; compare the available Render regions and provider locations before selecting one.
5. Choose a paid instance for a persistent production API. Free instances can sleep and have cold starts.
6. Set the health-check path to `/actuator/health`.

## Add Environment Variables

In the Render service's **Environment** page, add the following values. Use rotated/current values from each provider; never paste secrets into source files.

For the initial deployment without Kafka, set `KAFKA_ENABLED=false` and omit all Kafka broker and credential variables. The `prod` profile also defaults Kafka to disabled if this flag is omitted.

| Key | Value |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | Supabase JDBC URL using the session pooler and SSL, e.g. `jdbc:postgresql://<host>:5432/postgres?sslmode=require` |
| `DB_USERNAME` | Supabase database username |
| `DB_PASSWORD` | Supabase database password |
| `REDIS_URL` | Upstash TLS URL beginning with `rediss://` |
| `JWT_SECRET` | New random secret, at least 32 bytes |
| `CORS_ALLOWED_ORIGINS` | Comma-separated deployed frontend origins, with no trailing slash |
Render provides `PORT` automatically; do not set it manually. If the frontend sites are not deployed yet, temporarily set `CORS_ALLOWED_ORIGINS` to the local origins you use for testing, then replace them with the deployed frontend origins before launch. Add other integration keys (Razorpay, Cloudinary, SMS, WhatsApp, and Maps) as needed for the features you enable. Do not copy old values from the local `.env` without rotating them.

### Supabase

Use the Supabase **session pooler** connection details and port shown for your project. Keep the JDBC URL, username, and password in their separate variables above. Back up the database before the first deployment: the `prod` profile runs Flyway migrations during startup.

### Upstash Redis

Set `REDIS_URL` to the provider's TLS connection URL (`rediss://...`). The application uses this URL directly; do not configure localhost Redis values in Render.

### Confluent Cloud Kafka

Kafka is disabled by default in the `prod` profile. The outbox publisher and catalog listener do not run while it is disabled, and Kafka is excluded from the health check. Pending outbox events will not be published until Kafka is enabled.

To enable it later, create a Kafka cluster and a cluster-scoped API key in Confluent Cloud, then add these Render variables:

| Key | Value |
| --- | --- |
| `KAFKA_ENABLED` | `true` |
| `KAFKA_BOOTSTRAP_SERVERS` | Confluent cluster bootstrap server, including its port |
| `KAFKA_SECURITY_PROTOCOL` | `SASL_SSL` |
| `KAFKA_SASL_MECHANISM` | `PLAIN` |
| `KAFKA_SASL_JAAS_CONFIG` | `org.apache.kafka.common.security.plain.PlainLoginModule required username="<API_KEY>" password="<API_SECRET>";` with placeholders replaced by the Confluent API key and secret |

Confluent displays the secret when it is created, so save it directly in Render's environment settings. The application uses SASL over TLS for this connection.

## Deploy and Verify

1. Save the environment variables and start the first deploy from Render.
2. Open the deploy logs. Confirm Spring Boot starts, Flyway completes, and the service reports healthy. Resolve database, Redis, or Kafka authentication errors before directing users to the API.
3. Visit `https://<your-render-service>.onrender.com/actuator/health`; expect a healthy status. This endpoint is public for Render's health check; other actuator endpoints are not exposed in the `prod` profile.
4. Swagger/OpenAPI is disabled in production by default. If temporarily required, set `SPRINGDOC_ENABLED=true`, then turn it off again when finished.
5. Update `CORS_ALLOWED_ORIGINS` with the final frontend URLs and redeploy. CORS does not replace authentication; keep API authorization enabled.

## Notes

- The `prod` profile requires `JWT_SECRET` and `CORS_ALLOWED_ORIGINS`; startup fails rather than silently using development defaults when they are missing.
- Hibernate uses `ddl-auto=validate` in production. Flyway owns schema changes; do not switch production to `update`.
- `backend/.env` is for local development only. Render does not read it; configure every deployment value in Render.
- Kafka defaults off in production. Set `KAFKA_ENABLED=true` only after the broker and authentication variables are present.
- Do not expose `/actuator/prometheus`, `/actuator/metrics`, or Swagger endpoints publicly in production.