# Billy API Customers

API REST CRUD en Java 21 para gestionar dos colecciones: **customers** (clientes) e **items** (artículos). Desarrollada sin frameworks (sin Spring Boot), con servidor HTTP embebido del JDK y base de datos NoSQL embebida MapDB.

## Requisitos previos

- **JDK 21** (Java Development Kit 21).
- **Maven 3.6+** (para compilar y empaquetar).

Comprobar versiones:

```bash
java -version
# Debe indicar version 21 (o superior 21.x)

mvn -version
# Debe indicar Apache Maven 3.6 o superior
```

Sistemas soportados: Windows, Linux, macOS (donde Maven y JDK 21 estén instalados).

## Instalación y compilación

1. **Obtener el código fuente**  
   Clonar el repositorio o descomprimir el código en una carpeta, por ejemplo `api-customers`.

2. **Ir al directorio del proyecto**

   ```bash
   cd api-customers
   ```

3. **Compilar y empaquetar**

   ```bash
   mvn clean package
   ```

   El JAR ejecutable (con todas las dependencias incluidas) se genera en:

   - `target/api-customers-1.0.0-SNAPSHOT.jar`

## Pruebas

Para ejecutar las pruebas de integración:

```bash
mvn test
```

Las pruebas levantan un servidor en un puerto aleatorio, usan una base de datos de test y cubren el CRUD de customers e items, validación (email, nombre obligatorio), 404 para recurso o ruta inexistente, etc.

## Ejecución del servicio

Desde la raíz del proyecto:

```bash
java -jar target/api-customers-1.0.0-SNAPSHOT.jar
```

Debería aparecer en consola algo como:

```
Billy API Customers escuchando en http://localhost:8080
```

La API queda disponible en **http://localhost:8080**. Los datos se persisten en el archivo **`billy-api-customers.db`** en el directorio desde el que se ejecutó el comando (mismo directorio de trabajo actual).

## Estructura del proyecto

```
api-customers/
├── src/
│   ├── main/
│   │   ├── java/com/billy/customers/
│   │   │   ├── config/          # Arranque, servidor HTTP, seed de datos
│   │   │   ├── domain/          # Entidades (Customer, Item)
│   │   │   ├── http/            # Handlers REST, JSON, rate limit, errores
│   │   │   ├── persistence/     # Acceso a MapDB
│   │   │   └── common/          # Validación, sanitización, rate limiter, mapeo de errores
│   │   └── resources/
│   └── test/
│       └── java/com/billy/customers/
│           └── ApiIntegrationTest.java   # Pruebas de integración de los endpoints
├── pom.xml
└── README.md
```

| Carpeta / paquete | Contenido |
|-------------------|-----------|
| `config` | Punto de entrada (`Application`), configuración del servidor y de la BD, datos iniciales (`SeedData`). |
| `domain` | Records de entidades: `Customer`, `Item`. |
| `http` | Handlers REST (`CustomersHandler`, `ItemsHandler`), base común (`BaseHandler`), soporte JSON, `ApiError`. |
| `persistence` | Adaptador MapDB: apertura/cierre de BD, mapas `customers` e `items`, commit con reintentos. |
| `common` | Validación y sanitización (`InputValidator`), límite de peticiones (`RateLimiter`), mapeo de excepciones a HTTP (`ErrorMapping`). |

## Uso de la API

Todos los endpoints devuelven o aceptan JSON (`Content-Type: application/json`). Las respuestas de error tienen la forma `{"message":"descripción del error"}`.

### Colección: customers

| Método   | Ruta              | Descripción                    |
|----------|-------------------|--------------------------------|
| GET      | /customers        | Lista todos los clientes       |
| GET      | /customers/{id}   | Obtiene un cliente por id      |
| POST     | /customers        | Crea un cliente                |
| PATCH    | /customers/{id}   | Actualiza parcialmente un cliente |
| DELETE   | /customers/{id}   | Elimina un cliente             |

Campos de un cliente: `id`, `name`, `lastname`, `gender`, `email`. En POST el `id` es opcional; si no se envía, se genera uno automáticamente.

### Colección: items

| Método   | Ruta        | Descripción                    |
|----------|-------------|--------------------------------|
| GET      | /items      | Lista todos los ítems          |
| GET      | /items/{id} | Obtiene un ítem por id         |
| POST     | /items      | Crea un ítem                   |
| PATCH    | /items/{id} | Actualiza parcialmente un ítem |
| DELETE   | /items/{id} | Elimina un ítem                |

Campos de un ítem: `id`, `name`, `size`, `weight`, `color`. En POST el `id` es opcional; si no se envía, se genera uno automáticamente.

### Ejemplos de endpoints (request y response)

**GET /customers — Listar clientes**

| Request | Response |
|--------|----------|
| `GET http://localhost:8080/customers` | `200 OK` |
| Sin body | `[{"id":"cust-001","name":"Ana","lastname":"García","gender":"F","email":"ana.garcia@example.com"}, ...]` |

---

**POST /customers — Crear cliente**

| Request | Response |
|--------|----------|
| `POST http://localhost:8080/customers` | `201 Created` |
| `Content-Type: application/json` | |
| `{"name":"Juan","lastname":"Pérez","gender":"M","email":"juan@example.com"}` | `{"id":"<uuid>","name":"Juan","lastname":"Pérez","gender":"M","email":"juan@example.com"}` |

---

**GET /customers/{id} — Obtener cliente por id**

| Request | Response |
|--------|----------|
| `GET http://localhost:8080/customers/cust-001` | `200 OK` |
| Sin body | `{"id":"cust-001","name":"Ana","lastname":"García","gender":"F","email":"ana.garcia@example.com"}` |

---

**PATCH /customers/{id} — Actualizar parcialmente**

| Request | Response |
|--------|----------|
| `PATCH http://localhost:8080/customers/cust-001` | `200 OK` |
| `Content-Type: application/json` | |
| `{"email":"nuevo@example.com"}` | `{"id":"cust-001","name":"Ana","lastname":"García","gender":"F","email":"nuevo@example.com"}` |

---

**DELETE /customers/{id} — Eliminar cliente**

| Request | Response |
|--------|----------|
| `DELETE http://localhost:8080/customers/cust-001` | `204 No Content` |
| Sin body | Sin body |

---

**GET /items — Listar ítems**

| Request | Response |
|--------|----------|
| `GET http://localhost:8080/items` | `200 OK` |
| Sin body | `[{"id":"item-001","name":"Silla ergonómica","size":"65x60x120 cm","weight":"12","color":"negro"}, ...]` |

---

**POST /items — Crear ítem**

| Request | Response |
|--------|----------|
| `POST http://localhost:8080/items` | `201 Created` |
| `Content-Type: application/json` | |
| `{"name":"Mesa","size":"120x60","weight":"15","color":"madera"}` | `{"id":"<uuid>","name":"Mesa","size":"120x60","weight":"15","color":"madera"}` |

---

**GET /items/{id} — Obtener ítem por id**

| Request | Response |
|--------|----------|
| `GET http://localhost:8080/items/item-001` | `200 OK` |
| Sin body | `{"id":"item-001","name":"Silla ergonómica","size":"65x60x120 cm","weight":"12","color":"negro"}` |

---

**PATCH /items/{id} — Actualizar ítem parcialmente**

| Request | Response |
|--------|----------|
| `PATCH http://localhost:8080/items/item-001` | `200 OK` |
| `Content-Type: application/json` | |
| `{"color":"blanco"}` | `{"id":"item-001","name":"Silla ergonómica","size":"65x60x120 cm","weight":"12","color":"blanco"}` |

---

**DELETE /items/{id} — Eliminar ítem**

| Request | Response |
|--------|----------|
| `DELETE http://localhost:8080/items/item-001` | `204 No Content` |
| Sin body | Sin body |

> **Nota:** La API puede probarse con **curl** o importando una **colección en Postman** (base URL: `http://localhost:8080`).

### Respuestas de error

Todas las respuestas de error usan el cuerpo JSON `{"message":"descripción"}`.

| Código | Situación |
|--------|-----------|
| **400** | Validación fallida (payload inválido, email incorrecto, campos obligatorios vacíos). |
| **404** | Recurso no encontrado (id inexistente) o **ruta no existente** (endpoint que no es `/customers` ni `/items`). |
| **413** | Body demasiado grande (límite 1 MB en POST/PATCH). |
| **429** | Límite de peticiones superado (200 req/s). |
| **500** | Error interno del servidor (mensaje genérico; el detalle se registra solo en logs). |
| **503** | Servicio no disponible (por ejemplo, error de I/O o de base de datos). |

**400 — Validación fallida:**

```json
{"message":"email: formato inválido"}
```

**404 — Recurso o endpoint no encontrado:**

```json
{"message":"Customer not found"}
```

Para una ruta que no existe (por ejemplo `GET /ruta-inexistente`):

```json
{"message":"Not found"}
```

**413 — Payload too large:**

```json
{"message":"Payload too large"}
```

**429 — Too Many Requests:**

```json
{"message":"Too Many Requests"}
```

**500 — Internal server error:**

```json
{"message":"Internal server error"}
```

**503 — Service temporarily unavailable:**

```json
{"message":"Service temporarily unavailable"}
```

## Detalles técnicos

- **Servidor HTTP**: `com.sun.net.httpserver.HttpServer` (JDK 21), con executor de virtual threads.
- **Base de datos**: MapDB (NoSQL embebida). Persistencia en el archivo `billy-api-customers.db` en el directorio de trabajo. Los commits usan reintentos con backoff ante fallos transitorios.
- **JSON**: Jackson para serialización y deserialización.
- **Límite de peticiones**: 200 peticiones por segundo por instancia; si se supera se responde **429 Too Many Requests**.
- **Límite de tamaño de body**: 1 MB en POST y PATCH; si se supera (según `Content-Length`) se responde **413 Payload too large**.
- **Paquete base**: `com.billy.customers`.
