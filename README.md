# Stock Control – Backend

API REST para el control de stock **multisucursal**: catálogo de productos, sucursales, stock por sucursal e historial de movimientos (entradas, salidas, transferencias y ajustes).

## Stack

| Tecnología | Uso |
|---|---|
| Java 21 | Lenguaje |
| Spring Boot 4.1.1 | Framework (Web MVC, Data JPA, Validation) |
| PostgreSQL | Base de datos |
| Lombok | Reducción de código repetitivo |
| Maven | Build y dependencias |

## Requisitos

- JDK 21
- PostgreSQL 14 o superior (pgAdmin opcional, para administrarlo)
- Maven (o el wrapper `mvnw` incluido)

## Puesta en marcha

### 1. Crear la base de datos

En pgAdmin (o `psql`) crea una base llamada `stock_control` y ejecuta el archivo `schema.sql` sobre ella. El script crea las tablas, índices, el tipo enumerado `movement_type` y la vista `v_low_stock`.

> `schema.sql` es para una base **nueva**: crea las tablas desde cero.

### 2. Configurar la conexión

`src/main/resources/application.properties`:

```properties
spring.application.name=stock-control-backend
spring.datasource.url=jdbc:postgresql://localhost:5432/stock_control
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
```

La contraseña se toma de la variable de entorno `DB_PASSWORD`, así no queda escrita en el repositorio.

- **IntelliJ:** *Run → Edit Configurations → Environment variables* → `DB_PASSWORD=tu_clave`
- **Terminal (Linux/macOS):** `export DB_PASSWORD=tu_clave`
- **Terminal (Windows PowerShell):** `$env:DB_PASSWORD="tu_clave"`

`ddl-auto=validate` significa que Hibernate **no modifica** la base: solo comprueba que las entidades coincidan con las tablas. El esquema se gestiona con `schema.sql`.

### 3. Ejecutar

```bash
mvn spring-boot:run
```

La API queda disponible en `http://localhost:8080`.

## Arquitectura

Arquitectura en capas, con una responsabilidad por capa:

```
controller  →  service  →  repository  →  PostgreSQL
    ↑             ↑
   dto        entity
```

| Paquete | Responsabilidad |
|---|---|
| `controller` | Expone los endpoints HTTP. Valida la entrada (`@Valid`) y delega al service. No contiene lógica de negocio. |
| `service` | Reglas de negocio y transacciones (`@Transactional`). |
| `repository` | Acceso a datos con Spring Data JPA. |
| `entity` | Entidades JPA que mapean las tablas. **No se exponen por la API.** |
| `dto` | Objetos de entrada (`*Request`) y salida (`*Response`), implementados como `record`. |
| `exception` | Excepciones de dominio y `GlobalExceptionHandler`, que las traduce a respuestas HTTP. |
| `config` | Configuración (CORS). |

```
src/main/java/com/stock/stockcontrolbackend/
├── config/        CorsConfig
├── controller/    Product, Warehouse, Stock, StockMovement, Category
├── dto/           ProductRequest/Response, WarehouseRequest/Response,
│                  StockItemResponse, StockMovementRequest/Response
├── entity/        Product, Warehouse, StockItem, StockMovement,
│                  Category, MovementType
├── exception/     ResourceNotFound, DuplicateResource, InsufficientStock,
│                  BusinessRule, GlobalExceptionHandler
├── repository/    uno por entidad (+ ProductStockTotal)
└── service/       Product, Warehouse, Stock, StockMovement
```

## Modelo de datos

```
product ──< stock_item >── warehouse
   │                           │
   └────< stock_movement >─────┘   (origen y destino opcional)
```

- **`product`**: catálogo (`code` único, `name`, `unit_price`, `final_price`, `is_active`).
- **`warehouse`**: cada fila es una **sucursal** (`code` único, `name`, `address`, `is_active`).
- **`stock_item`**: stock actual de un producto **en una sucursal** (`quantity`, `min_quantity`). Tiene `UNIQUE (product_id, warehouse_id)` y `CHECK (quantity >= 0)`.
- **`stock_movement`**: historial de auditoría. Cada movimiento queda registrado y no se modifica ni se borra.
- **`category`**: tabla de categorías con CRUD básico. Todavía no está vinculada a los productos.

Las cantidades son `NUMERIC(14,3)`: admiten hasta 3 decimales (por ejemplo, productos que se venden por kilo).

El **stock total** de un producto no se guarda: se calcula sumando sus filas de `stock_item` en todas las sucursales.

## Reglas de negocio

### Movimientos de stock

Cada movimiento se registra en `POST /api/stock-movements` y actualiza `stock_item` **en la misma transacción**. Si algo falla, no queda nada a medias.

| Tipo | Efecto sobre el stock |
|---|---|
| `ENTRADA` | Suma la cantidad a la sucursal indicada. |
| `SALIDA` | Resta la cantidad. Responde `409` si no alcanza. |
| `TRANSFERENCIA` | Resta en la sucursal de origen y suma en la de destino. Requiere `warehouseDestinationId`, distinto del origen. |
| `AJUSTE` | **Fija** el stock en la cantidad indicada (conteo físico). |

Validaciones comunes:
- La cantidad debe ser mayor a 0 (también para `AJUSTE`; para dejar el stock en cero se usa una `SALIDA`).
- El producto y las sucursales deben existir y estar activos.
- Si el producto nunca estuvo en la sucursal, su fila de stock se crea automáticamente con cantidad 0.

### Concurrencia

Al modificar el stock se bloquea la fila de `stock_item` (`PESSIMISTIC_WRITE`). Así, dos salidas simultáneas no pueden dejar el stock en negativo ni vender mercadería que no existe.

### Borrado lógico

`DELETE` sobre productos y sucursales **no borra la fila**: la marca como inactiva (`is_active = false`). Esto conserva el historial de movimientos.

- Los listados solo devuelven registros activos.
- Una sucursal **con stock** no se puede desactivar (`409`). Primero hay que transferir o ajustar el stock.
- El `code` de un producto o sucursal desactivado sigue ocupado: no se puede reutilizar.

### Stock bajo

Un ítem está en stock bajo cuando `quantity <= min_quantity` y el producto está activo. El mínimo se define por sucursal en `stock_item.min_quantity` (hoy se carga directamente en la base).

## API

Base URL: `http://localhost:8080/api`

### Productos — `/api/products`

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/products?name=` | Lista productos activos (filtro opcional por nombre), con `totalQuantity`. |
| GET | `/products/{id}` | Detalle de un producto. |
| POST | `/products` | Crea un producto (`201`). |
| PUT | `/products/{id}` | Edita un producto. |
| DELETE | `/products/{id}` | Desactiva el producto (`204`). |

Request (`POST` / `PUT`):
```json
{ "code": "P-001", "name": "Teclado", "unitPrice": 1500.00, "finalPrice": 1815.00 }
```

Response:
```json
{
  "id": 1, "code": "P-001", "name": "Teclado",
  "unitPrice": 1500.00, "finalPrice": 1815.00,
  "totalQuantity": 10.000, "active": true
}
```

### Sucursales — `/api/warehouses`

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/warehouses` | Lista sucursales activas. |
| GET | `/warehouses/{id}` | Detalle. |
| POST | `/warehouses` | Crea una sucursal (`201`). |
| PUT | `/warehouses/{id}` | Edita una sucursal. |
| DELETE | `/warehouses/{id}` | Desactiva la sucursal (`204`). `409` si tiene stock. |

Request: `{ "name": "Sucursal Centro", "code": "CEN", "address": "San Martín 100" }`

### Stock — `/api/stock`

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/stock?warehouseId={id}` | Inventario de una sucursal. |
| GET | `/stock?productId={id}` | Un producto repartido por sucursal. |
| GET | `/stock/low` | Ítems con stock bajo, de todas las sucursales. |

Es obligatorio enviar `warehouseId` **o** `productId` (si no, responde `400`).

Response:
```json
{
  "productId": 1, "productCode": "P-001", "productName": "Teclado",
  "warehouseId": 1, "warehouseName": "Sucursal Centro",
  "quantity": 6.000, "minQuantity": 2.000
}
```

### Movimientos — `/api/stock-movements`

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/stock-movements` | Registra un movimiento y actualiza el stock (`201`). |
| GET | `/stock-movements?productId={id}` | Historial de un producto, del más reciente al más antiguo. |
| GET | `/stock-movements?warehouseId={id}` | Historial de una sucursal. Incluye las transferencias que **llegan** a ella. |

Request:
```json
{
  "type": "TRANSFERENCIA",
  "productId": 1,
  "warehouseId": 1,
  "warehouseDestinationId": 2,
  "quantity": 4,
  "reason": "Reposición",
  "referenceDocument": "REM-0001",
  "createdBy": "Carla"
}
```
`warehouseDestinationId` solo se usa en `TRANSFERENCIA`. `reason`, `referenceDocument` y `createdBy` son opcionales.

### Categorías — `/api/categories`

`GET`, `POST` y `DELETE`. Hoy no están relacionadas con los productos.

## Manejo de errores

Todas las respuestas de error tienen el mismo formato:

```json
{
  "timestamp": "2026-11-20T22:52:10.123",
  "status": 409,
  "error": "Conflict",
  "message": "Stock insuficiente de P-001 en la sucursal Sucursal Norte (disponible: 4.000, solicitado: 99)"
}
```

| Código | Cuándo ocurre |
|---|---|
| `400 Bad Request` | Datos inválidos (campos vacíos, precios negativos, cantidad ≤ 0), falta un filtro obligatorio o el destino de una transferencia es inválido. |
| `404 Not Found` | El producto, la sucursal o la categoría no existe (o está inactivo). |
| `409 Conflict` | Código duplicado, stock insuficiente, o regla de negocio (sucursal con stock). |

## CORS

Permitido para `/api/**` desde `http://localhost:5173` (Vite) y `http://localhost:3000`. Para otro origen, edita `config/CorsConfig.java`.

## Probar rápido

```bash
# Dos sucursales
curl -X POST localhost:8080/api/warehouses -H "Content-Type: application/json" \
  -d '{"name":"Sucursal Centro","code":"CEN"}'
curl -X POST localhost:8080/api/warehouses -H "Content-Type: application/json" \
  -d '{"name":"Sucursal Norte","code":"NOR"}'

# Un producto
curl -X POST localhost:8080/api/products -H "Content-Type: application/json" \
  -d '{"code":"P-001","name":"Teclado","unitPrice":1500,"finalPrice":1815}'

# Entrada de 10 unidades en Centro
curl -X POST localhost:8080/api/stock-movements -H "Content-Type: application/json" \
  -d '{"productId":1,"warehouseId":1,"type":"ENTRADA","quantity":10}'

# Transferir 4 a Norte
curl -X POST localhost:8080/api/stock-movements -H "Content-Type: application/json" \
  -d '{"productId":1,"warehouseId":1,"warehouseDestinationId":2,"type":"TRANSFERENCIA","quantity":4}'

# Verificar: totalQuantity = 10, Centro = 6, Norte = 4
curl localhost:8080/api/products
curl "localhost:8080/api/stock?productId=1"
```

## Decisiones de diseño

- **La lógica de stock vive en la aplicación, no en la base.** Se eliminó el trigger que actualizaba `stock_item`: no validaba stock insuficiente (daba un error 500 en vez de un 409 claro), sumaba los ajustes en vez de fijarlos y no bloqueaba filas. Con la lógica en el service, es más fácil de probar y de mantener.
- **Las entidades nunca salen por la API.** Los DTOs desacoplan el contrato HTTP del modelo de datos y evitan problemas con las relaciones lazy.
- **El `finalPrice` lo carga el usuario**, no se calcula a partir del precio unitario.
- **Consultas sin N+1:** los totales de stock por producto se calculan con una sola consulta agrupada, y los listados de stock y movimientos traen sus relaciones con `@EntityGraph` / `JOIN FETCH`.

## Pendiente / próximos pasos

- Endpoint para editar el stock mínimo (`min_quantity`) por sucursal.
- Tests automatizados (hoy solo existe el test de arranque del contexto).
- Autenticación y usuarios (hoy `createdBy` es un texto libre).
- Paginación en los listados.
- Vincular los productos con las categorías.