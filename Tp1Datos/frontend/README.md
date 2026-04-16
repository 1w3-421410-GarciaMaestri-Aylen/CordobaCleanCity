# Frontend - EcoRuta

## Estructura

```text
frontend/
|- index.html
|- package.json
|- tsconfig.json
|- vite.config.ts
|- .env.example
|- src/
|  |- App.tsx
|  |- main.tsx
|  |- components/
|  |  |- UploadPanel.tsx
|  |  |- ReportMap.tsx
|  |  |- StatusBanner.tsx
|  |  |- QrModal.tsx
|  |- services/
|  |  |- api.ts
|  |- styles/
|  |  |- variables.css
|  |  |- app.css
|  |- types/
|  |  |- api.ts
```

## Comandos

```bash
npm install
npm run dev
```

Por defecto consume `/api/*` y Vite hace proxy a `http://localhost:8080`.
