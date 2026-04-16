# EcoRuta - Reporte de Basura y Optimizacion de Rutas

EcoRuta es un sistema web para reportar puntos con basura mediante imagenes, validar esos reportes con un catalogo local por hash exacto y generar rutas optimas de recoleccion en un mapa.

La solucion incluye:
- API backend en Spring Boot
- Frontend publico en React + Vite
- MongoDB, RabbitMQ y Redis (con Docker Compose)

## 1. Que hace el sistema

- Recibe cargas de imagen desde una interfaz publica (`multipart/form-data`).
- Guarda las imagenes localmente (ruta temporal/local).
- Publica un mensaje en RabbitMQ para procesamiento asincrono.
- Un consumer procesa ese mensaje:
  - Ejecuta clasificacion local de imagen por hash SHA-256 exacto contra catalogo local.
  - Si detecta basura, marca el reporte como valido.
  - Si no detecta basura, marca el reporte como rechazado por IA (no basura por defecto).
- Expone endpoints para consultar reportes (todos / del dia).
- Expone endpoint de generacion de ruta optima (nearest neighbor + Haversine).
- Usa cache en Redis para reportes del dia y rutas.

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
- CSS moderno responsive

### Infraestructura
- MongoDB
- RabbitMQ (incluye Management UI)
- Redis
- Docker Compose

## 3. Arquitectura (breve)

Backend por capas:
- `controller`: entrada/salida HTTP
- `service`: logica de negocio
- `repository`: persistencia en Mongo
- `messaging/producer`: publicacion de eventos async
- `messaging/consumer`: procesamiento async
- `cache`: nombres y uso de cache
- `config`: Mongo, Rabbit, Redis, CORS y properties
- `dto`: contratos de API/eventos
- `model`: entidades de dominio

Flujo principal:
1. `POST /api/reports` recibe imagen y metadata.
2. El backend guarda la imagen local y publica `ReportProcessingEvent`.
3. El consumer clasifica la imagen.
4. Si `isTrash=true`, se guarda reporte en MongoDB.
5. Se invalida cache de reportes del dia/rutas.
6. El frontend consulta reportes y rutas por REST.

## 4. Estructura del repositorio

```text
.
|- src/                     # Backend Spring Boot
|- frontend/                # Frontend React + Vite
|- docker-compose.yml       # MongoDB + RabbitMQ + Redis
|- .env.example             # Ejemplo de variables backend
|- README.md
|- README.es.md
```

## 5. Variables de entorno

### Backend
Ver [`/.env.example`](./.env.example).

Importantes:
- `MONGODB_URI`
- `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`
- `TRASH_CATALOG_DIR` (por defecto `storage/catalog/basura`)
- `NON_TRASH_CATALOG_DIR` (por defecto `storage/catalog/no-basura`)
- `CORS_ALLOWED_ORIGINS` (por defecto `http://localhost:5173`)

Nota: Spring Boot no carga `.env` automaticamente por si solo. Exporta variables en shell o configuralas en tu IDE/perfil de ejecucion.

### Frontend
Ver [`/frontend/.env.example`](./frontend/.env.example).

- `VITE_API_BASE_URL`: opcional, puede quedar vacio si usas proxy de Vite.
- `VITE_BACKEND_URL`: target del proxy para `/api` en desarrollo.

## 6. Levantar servicios auxiliares (Docker Compose)

```bash
docker compose up -d
```

Servicios:
- MongoDB: `localhost:27017`
- RabbitMQ AMQP: `localhost:5672`
- RabbitMQ Management UI: `http://localhost:15672` (`guest/guest`)
- Redis: `localhost:6379`

Compose: [`/docker-compose.yml`](./docker-compose.yml)

## 7. Ejecucion local

### 7.1 Backend

Desde la raiz del proyecto:

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw spring-boot:run
```

Base URL backend: `http://localhost:8080`

### 7.2 Frontend

Desde `frontend/`:

```bash
npm install
npm run dev
```

URL frontend: `http://localhost:5173`

## 8. Endpoints API y ejemplos

### `POST /api/reports`
Acepta imagen + metadata y devuelve `202 Accepted` (procesamiento async).

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

Response (`200` ejemplo):

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

La respuesta tiene la misma estructura que `GET /api/reports`, filtrada al dia actual.

### `GET /api/routes/optimal`
Parametros:
- `scope`: `today` o `active`
- `startLat`: latitud inicial del recolector (obligatorio, dentro de Cordoba Capital)
- `startLng`: longitud inicial del recolector (obligatorio, dentro de Cordoba Capital)

Request:

```bash
curl "http://localhost:8080/api/routes/optimal?scope=today&startLat=-31.420083&startLng=-64.188776"
```

Response (`200` ejemplo):

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

## 9. Comportamiento de RabbitMQ

- El producer publica `ReportProcessingEvent` en `report.processing.exchange`.
- Cola principal: `report.processing.queue`.
- El consumer ejecuta clasificacion + decision de persistencia.
- Dead letter configurado:
  - DLX: `report.processing.dlx`
  - DLQ: `report.processing.dlq`

## 10. Comportamiento de cache en Redis

- Cache de `GET /api/reports/today`: `reports:today`
- Cache de `GET /api/routes/optimal`: `routes:optimization`
- La invalidacion ocurre cuando el consumer async persiste un reporte valido.

## 11. Clasificacion con catalogo local

- La clasificacion es totalmente local y exacta por SHA-256 del contenido binario.
- Si el hash de la imagen subida existe en `TRASH_CATALOG_DIR`: se clasifica como basura.
- Si el hash de la imagen subida existe en `NON_TRASH_CATALOG_DIR`: se clasifica como no basura.
- Si no hay coincidencia: se clasifica como no basura por defecto.
- No se usa OCR, no hay IA externa y no hay similitud aproximada.
- El backend crea ambas carpetas de catalogo automaticamente si no existen.

## 12. Pasos de prueba end-to-end

1. Levantar infraestructura:
   - `docker compose up -d`
2. Levantar backend:
   - `./mvnw spring-boot:run`
3. Levantar frontend:
   - `cd frontend && npm install && npm run dev`
4. Abrir UI:
   - `http://localhost:5173`
5. Subir imagen con coordenadas.
6. Esperar procesamiento async (normalmente unos segundos).
7. Actualizar reportes en UI.
8. Confirmar que aparezca marcador en mapa.
9. Generar ruta y verificar polyline.
10. Generar QR y escanear.

## 13. Notas operativas

- El flujo es async: el upload responde `202` y la persistencia ocurre despues.
- Coloca tus imagenes de referencia en `storage/catalog/basura` y `storage/catalog/no-basura` (o cambia rutas por variables de entorno).
- La comparacion es exacta por hash SHA-256; el nombre del archivo no afecta.
- Si frontend/backend corren en hosts distintos, ajustar `CORS_ALLOWED_ORIGINS`.
