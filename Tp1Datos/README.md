# EcoRuta - Garbage Reporting and Route Optimization

EcoRuta is a web system to report garbage points using images, validate those reports with a local exact-hash catalog, and generate optimal collection routes on a map.

The solution includes:
- Spring Boot backend API
- React + Vite public frontend
- MongoDB, RabbitMQ, Redis (in Docker Compose)

## 1. What the system does

- Receives image uploads from a public UI (`multipart/form-data`).
- Stores uploaded images locally (temporary/local path).
- Publishes a message to RabbitMQ for async processing.
- A consumer processes that message:
  - Runs local image classification by exact SHA-256 hash against a local catalog.
  - If garbage is detected, marks the report as valid.
  - If garbage is not detected, marks the report as rejected by IA (default non-garbage).
- Exposes report query endpoints (all reports / today reports).
- Exposes optimal route generation endpoint (nearest neighbor + Haversine).
- Uses Redis cache for today reports and routes.

## 2. Stack

### Backend
- Java 21
- Spring Boot
- Spring Web
- Spring Data MongoDB
- Spring AMQP (RabbitMQ)
- Spring Cache + Redis
- Bean Validation
- Maven

### Frontend
- React 18
- Vite
- TypeScript
- Leaflet + React Leaflet
- qrcode.react
- Modern responsive CSS

### Infrastructure
- MongoDB
- RabbitMQ (management UI included)
- Redis
- Docker Compose

## 3. Architecture (short)

Layered backend:
- `controller`: HTTP input/output
- `service`: business logic
- `repository`: Mongo persistence
- `messaging/producer`: async event publishing
- `messaging/consumer`: async processing
- `cache`: cache names and usage
- `config`: Mongo, Rabbit, Redis, CORS, app properties
- `dto`: API/event contracts
- `model`: domain entities

Main flow:
1. `POST /api/reports` receives image and metadata.
2. Backend stores local image and publishes `ReportProcessingEvent`.
3. Consumer classifies image.
4. If `isTrash=true`, report is saved in MongoDB.
5. Cache for today reports/routes is invalidated.
6. Frontend reads reports and routes from REST endpoints.

## 4. Repository structure

```text
.
|- src/                     # Spring Boot backend
|- frontend/                # React + Vite frontend
|- docker-compose.yml       # MongoDB + RabbitMQ + Redis
|- .env.example             # Backend env example
|- README.md
```

## 5. Environment variables

### Backend
See [`/.env.example`](./.env.example).

Important:
- `MONGODB_URI`
- `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`
- `TRASH_CATALOG_DIR` (default `storage/catalog/basura`)
- `NON_TRASH_CATALOG_DIR` (default `storage/catalog/no-basura`)
- `CORS_ALLOWED_ORIGINS` (default `http://localhost:5173`)

Note: Spring Boot does not load `.env` automatically by itself. Export vars in shell or configure them in your IDE/run profile.

### Frontend
See [`/frontend/.env.example`](./frontend/.env.example).

- `VITE_API_BASE_URL`: optional, can be empty when using Vite proxy.
- `VITE_BACKEND_URL`: proxy target for `/api` in development.

## 6. Start auxiliary services (Docker Compose)

```bash
docker compose up -d
```

Services:
- MongoDB: `localhost:27017`
- RabbitMQ AMQP: `localhost:5672`
- RabbitMQ Management UI: `http://localhost:15672` (`guest/guest`)
- Redis: `localhost:6379`

Compose file: [`/docker-compose.yml`](./docker-compose.yml)

## 7. Run locally

### 7.1 Backend

The backend depends on:
- MongoDB on `localhost:27017`
- RabbitMQ on `localhost:5672`
- Redis on `localhost:6379`

For this local project state, use the helper script so the backend always starts with the same runtime configuration:

```powershell
.\scripts\start-backend-local.ps1
```

That script starts Spring Boot with:
- `SERVER_PORT=8080`
- `MONGODB_URI=mongodb://localhost:27017/test`
- `REDIS_HOST=localhost`
- `RABBITMQ_HOST=localhost`
- `TRASH_CATALOG_DIR=storage/catalog/basura`
- `NON_TRASH_CATALOG_DIR=storage/catalog/no-basura`

If you prefer to run it manually, export the same variables in your shell first and then run:

```powershell
.\mvnw.cmd spring-boot:run
```

Backend base URL: `http://localhost:8080`

### 7.2 Frontend

From `frontend/`:

```bash
npm install
npm run dev
```

Frontend URL: `http://localhost:5173`

## 8. API endpoints and examples

### `POST /api/reports`
Accepts image + metadata and returns `202 Accepted` (async processing).

Request:

```bash
curl -X POST http://localhost:8080/api/reports \
  -F "image=@/path/to/photo.jpg" \
  -F "user=juan" \
  -F "lat=-34.6037" \
  -F "lng=-58.3816"
```

Response (`202`):

```json
{
  "requestId": "3d1e8d6a-44f1-4e23-92f0-20c6a39fd7c5",
  "status": "QUEUED",
  "message": "Imagen recibida y en procesamiento asincrono",
  "imageUrl": "C:/Proyectos/Tp1Datos/uploads/2d8d1c66-47fc-4c31-8d02-4af96f5b40a2.jpg"
}
```

### `GET /api/reports`

```bash
curl http://localhost:8080/api/reports
```

Response (`200` example):

```json
[
  {
    "id": "67e85c7bb7d4514b1f9d02f1",
    "user": "juan",
    "imageUrl": "C:/Proyectos/Tp1Datos/uploads/2d8d1c66-47fc-4c31-8d02-4af96f5b40a2.jpg",
    "location": { "lat": -34.6037, "lng": -58.3816 },
    "createdAt": "2026-03-29T23:10:18.420Z",
    "status": "PROCESSED",
    "classificationResult": "Coincidencia exacta con catalogo local de basura. El reporte fue clasificado como basura.",
    "isTrash": true
  }
]
```

### `GET /api/reports/today`

```bash
curl http://localhost:8080/api/reports/today
```

Response shape is the same as `GET /api/reports`, filtered to current day.

### `GET /api/routes/optimal`
Query params:
- `scope`: `today` or `active`
- `startLat`: collector start latitude (required, must be inside Cordoba Capital)
- `startLng`: collector start longitude (required, must be inside Cordoba Capital)

Request:

```bash
curl "http://localhost:8080/api/routes/optimal?scope=today&startLat=-31.420083&startLng=-64.188776"
```

Response (`200` example):

```json
{
  "orderedPoints": [
    {
      "sequence": 1,
      "reportId": "67e85c7bb7d4514b1f9d02f1",
      "user": "juan",
      "coordinate": { "lat": -34.6037, "lng": -58.3816 },
      "createdAt": "2026-03-29T23:10:18.420Z"
    }
  ],
  "startCoordinate": { "lat": -31.420083, "lng": -64.188776 },
  "coordinates": [
    { "lat": -34.6037, "lng": -58.3816 }
  ],
  "totalDistanceKm": 0.0,
  "metadata": {
    "algorithm": "nearest-neighbor-haversine",
    "scope": "today",
    "sourceReportCount": 1
  },
  "generatedAt": "2026-03-29T23:14:32.101Z"
}
```

## 9. RabbitMQ behavior

- Producer publishes `ReportProcessingEvent` to `report.processing.exchange`.
- Main queue: `report.processing.queue`.
- Consumer executes classification + persistence decision.
- Dead letter setup:
  - DLX: `report.processing.dlx`
  - DLQ: `report.processing.dlq`

## 10. Redis cache behavior

- `GET /api/reports/today` cache: `reports:today`
- `GET /api/routes/optimal` cache: `routes:optimization`
- Cache invalidation occurs when a new valid report is persisted by async consumer.

## 11. Local catalog classification

- Classification is fully local and exact by SHA-256 of image binary content.
- If uploaded image hash exists in `TRASH_CATALOG_DIR`: report is classified as garbage.
- If uploaded image hash exists in `NON_TRASH_CATALOG_DIR`: report is classified as non-garbage.
- If hash does not match any catalog file: report is classified as non-garbage by default.
- No OCR, no external AI calls, no approximate similarity.
- The backend creates both catalog folders automatically if they are missing.

## 12. End-to-end test steps

1. Start infra:
   - `docker compose up -d`
2. Start backend:
   - `./mvnw spring-boot:run`
3. Start frontend:
   - `cd frontend && npm install && npm run dev`
4. Open UI:
   - `http://localhost:5173`
5. Upload an image with coordinates.
6. Wait for async processing (usually a few seconds).
7. Refresh reports in UI.
8. Confirm marker appears in map.
9. Generate route and verify polyline.
10. Generate QR and scan it.

## 13. Operational notes

- Async behavior is expected: upload response is `202` and persistence happens later.
- Put your reference files in `storage/catalog/basura` and `storage/catalog/no-basura` (or custom dirs via env vars).
- Matching is exact by SHA-256 hash; filename does not affect classification.
- If frontend/backend run on different hosts, update `CORS_ALLOWED_ORIGINS`.
- If the frontend starts returning `500` even on login, first verify that the backend is still running on `http://localhost:8080`. In local development the most common cause is closing the terminal that was running `.\scripts\start-backend-local.ps1`.
