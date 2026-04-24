# ♻️ Sistema Inteligente de Reporte y Recolección de Residuos

## 📌 Descripción del Proyecto

Este proyecto consiste en el desarrollo de un sistema inteligente orientado a mejorar la gestión de residuos urbanos mediante el uso de bases de datos no relacionales e inteligencia artificial.

La aplicación permite a los usuarios reportar residuos mal desechados a través de una interfaz simple, incluyendo:

* Subida de imágenes
* Dirección manual o selección de ubicación en un mapa

Estos reportes son procesados y analizados para optimizar posteriormente las rutas de recolección.

---

## 🎯 Objetivos

* Afianzar el uso de bases de datos NoSQL (MongoDB y Redis)
* Implementar procesamiento de imágenes mediante IA
* Diseñar un flujo de validación de datos eficiente
* Generar rutas óptimas de recolección en base a reportes reales
* Aplicar arquitectura basada en eventos y desacoplamiento de servicios

---

## 🧠 Funcionamiento General

1. **Creación de Reportes**

   * Los usuarios cargan una imagen del residuo
   * Se asocia una ubicación (dirección o punto en mapa)

2. **Validación con IA**

   * Las imágenes son procesadas por un modelo de IA que determina si contienen residuos
   * Mientras están en validación, los datos se almacenan temporalmente en Redis

3. **Persistencia de Datos**

   * Si el reporte es válido, se guarda definitivamente en MongoDB
   * Si no, se descarta

4. **Panel de Administración**

   * Visualización de reportes
   * Análisis de datos
   * Generación de reportes por período configurable
   * Planificación de rutas de recolección

5. **Optimización de Rutas**

   * A partir de un punto de origen definido
   * Se calcula la ruta más eficiente para recolectar los residuos reportados

---

## 🏗️ Arquitectura

El sistema está diseñado bajo un enfoque desacoplado y orientado a eventos:

* **Frontend:** Interfaz de usuario simple para carga de reportes
* **Backend:** Servicios encargados de procesamiento, validación y lógica de negocio
* **Cola de Mensajes:** Comunicación asincrónica mediante RabbitMQ
* **Procesamiento IA:** Análisis de imágenes para validación automática

---

## 🛠️ Tecnologías Utilizadas

* **Lenguajes:** (Java + Node.js)
* **Bases de Datos:**

  * Redis (almacenamiento temporal y procesamiento rápido)
  * MongoDB (persistencia de datos estructurados)
* **Mensajería:**

  * RabbitMQ (manejo de eventos y colas)
* **IA:**

  * Modelos de clasificación de imágenes
* **Agentes de Desarrollo:**

  * OpenCode
  * Codex
* **Otras herramientas:**

  * APIs de mapas (para geolocalización)
  * Docker 
  * Express 

---

## 🔄 Flujo de Datos

```text
Usuario → Subida de Reporte → Redis → IA (validación)
        → (válido) → MongoDB → Panel Admin → Optimización de rutas
        → (inválido) → Descarte
```

---

## 📊 Funcionalidades Clave

* 📷 Carga de imágenes de residuos
* 📍 Selección de ubicación en mapa
* 🤖 Validación automática con IA
* ⚡ Procesamiento rápido con Redis
* 🗄️ Persistencia en MongoDB
* 📈 Generación de reportes dinámicos
* 🗺️ Cálculo de rutas óptimas de recolección
* 👨‍💼 Panel de administración

---

## 🚀 Posibles Mejoras Futuras

* Entrenamiento de modelos de IA más precisos
* Integración con sistemas municipales reales
* Notificaciones en tiempo real
* Aplicación móvil
* Sistema de reputación para usuarios
* Predicción de zonas con alta acumulación de residuos

---

## 📚 Aprendizajes

* Uso práctico de bases de datos NoSQL en escenarios reales
* Integración de IA en pipelines de datos
* Arquitecturas basadas en eventos
* Manejo de datos geoespaciales
* Optimización de rutas (problemas tipo TSP)

---

## 👥 Autores

Proyecto desarrollado como práctica para el aprendizaje y consolidación de tecnologías modernas de backend, bases de datos y sistemas inteligentes.

---

## 📌 Notas Finales

Este proyecto simula un escenario real de gestión urbana, combinando múltiples tecnologías para resolver un problema concreto de forma escalable y eficiente.
