# FoodMarket — Plataforma de Delivery con Microservicios

> **DSY1103 Desarrollo FullStack I · Evaluación Parcial 3**

Plataforma de pedidos de comida a domicilio construida sobre una arquitectura de microservicios con Spring Boot. Conecta clientes, restaurantes y repartidores a través de 10 servicios independientes que se comunican de forma síncrona (OpenFeign) y asíncrona (Apache Kafka).

---

## Integrantes

| Integrante | Servicios a cargo |
|---|---|
| **Felipe Echeverría** | eureka-server, api-gateway, auth-service, user-service, restaurant-service, order-service |
| **Daniel Parada** | payment-service, delivery-service, notification-service, review-service, search-service, report-service |

---

## Stack tecnológico

| Tecnología | Uso |
|---|---|
| Spring Boot 3 | Framework base de cada microservicio |
| Spring Cloud Gateway | API Gateway con validación JWT |
| Netflix Eureka | Registro y descubrimiento de servicios |
| OpenFeign | Comunicación síncrona entre servicios |
| Apache Kafka | Mensajería asíncrona (eventos de dominio) |
| MySQL 8 | Base de datos por microservicio |
| Docker / Docker Compose | Contenedorización y orquestación |
| JWT | Autenticación sin estado |
| SpringDoc OpenAPI (Swagger) | Documentación interactiva de la API |
| Logback | Sistema de logs por archivo y consola |
| JUnit 5 + Mockito | Pruebas unitarias con cobertura ≥ 80% |

---

## Arquitectura general

```
Cliente (Postman / Swagger UI)
         │
         ▼
  ┌─────────────────┐
  │   API Gateway   │  :9090  — valida JWT, enruta requests
  └────────┬────────┘
           │
  ┌────────▼────────┐
  │  Eureka Server  │  :9761  — registro y descubrimiento de servicios
  └────────┬────────┘
           │
  ┌────────┴──────────────────────────────────────────┐
  │                   MICROSERVICIOS                  │
  │                                                   │
  │  auth-service         :9081   user-service  :9082 │
  │  restaurant-service   :9083   order-service :9084 │
  │  payment-service      :9085   delivery      :9086 │
  │  notification-service :9087   review        :9088 │
  │  search-service       :9089   report        :9095 │
  └───────────────────────────────────────────────────┘
           │
  ┌────────▼────────┐
  │  Apache Kafka   │  :9092 — mensajería asíncrona entre servicios
  │  + Zookeeper    │  :2181 — coordinador interno de Kafka
  └─────────────────┘
           │
  ┌────────▼────────┐
  │   MySQL :3307   │  — una base de datos por microservicio
  └─────────────────┘
```

---

## Documentación Swagger / OpenAPI

Cada microservicio expone su documentación interactiva en `/swagger-ui.html`. Cada endpoint está anotado con `@Tag`, `@Operation` y `@ApiResponse` para generar documentación automática.

| Servicio | Swagger UI | API Docs JSON |
|---|---|---|
| auth-service | http://localhost:9081/swagger-ui.html | http://localhost:9081/api-docs |
| user-service | http://localhost:9082/swagger-ui.html | http://localhost:9082/api-docs |
| restaurant-service | http://localhost:9083/swagger-ui.html | http://localhost:9083/api-docs |
| order-service | http://localhost:9084/swagger-ui.html | http://localhost:9084/api-docs |
| payment-service | http://localhost:9085/swagger-ui.html | http://localhost:9085/api-docs |
| delivery-service | http://localhost:9086/swagger-ui.html | http://localhost:9086/api-docs |
| notification-service | http://localhost:9087/swagger-ui.html | http://localhost:9087/api-docs |
| review-service | http://localhost:9088/swagger-ui.html | http://localhost:9088/api-docs |
| search-service | http://localhost:9089/swagger-ui.html | http://localhost:9089/api-docs |
| report-service | http://localhost:9095/swagger-ui.html | http://localhost:9095/api-docs |

---

## Sistema de Logs

Cada microservicio utiliza **Logback** con la anotación `@Slf4j` de Lombok. Los logs se escriben simultáneamente en consola y archivo.

**Niveles utilizados:**
- `INFO` — operaciones normales (login exitoso, pedido creado)
- `WARN` — situaciones anómalas no fatales
- `ERROR` — excepciones que interrumpen el flujo

**Configuración de rotación** (logback-spring.xml):
- Máximo **10 MB** por archivo
- Retención de **7 días**
- Límite total de **100 MB**

**Archivos de log generados:**

| Servicio | Archivo de log |
|---|---|
| ms-auth | `logs/ms-auth-YYYY-MM-DD.log` |
| ms-notification | `logs/ms-notification-YYYY-MM-DD.log` |
| ms-order | `logs/ms-order-YYYY-MM-DD.log` |
| ms-report | `logs/ms-report-YYYY-MM-DD.log` |
| ms-restaurant | `logs/ms-restaurant-YYYY-MM-DD.log` |

```bash
# Ver logs en tiempo real de un servicio
docker logs foodmarket-order -f
docker logs foodmarket-notification -f
docker logs foodmarket-report -f
```

---

## Pruebas unitarias

Cada uno de los 10 microservicios de negocio cuenta con su clase de test unitario sobre la **capa de servicio** (lógica de negocio), usando **JUnit 5 + Mockito** con convención Given–When–Then:

| Servicio | Clase de test |
|---|---|
| ms-auth | `com.foodmarket.auth.service.AuthServiceTest` |
| ms-user | `com.foodmarket.user.service.UserServiceTest` |
| ms-restaurant | `com.foodmarket.restaurant.service.RestaurantServiceTest` |
| ms-order | `com.foodmarket.order.service.OrderServiceTest` |
| payment-service | `com.foodmarket.payment.service.PaymentServiceTest` |
| ms-delivery | `com.foodmarket.delivery.service.DeliveryServiceTest` |
| ms-notification | `com.foodmarket.notification.service.NotificationServiceTest` |
| ms-review | `com.foodmarket.review.service.ReviewServiceTest` |
| search-service | `com.foodmarket.search.service.SearchServiceTest` |
| report-service | `com.foodmarket.report.service.ReportServiceTest` |

### Cómo ejecutar los tests y medir cobertura (IntelliJ IDEA)

1. En el panel de proyecto, navegar hasta la clase de test del microservicio (`src/test/java/...`).
2. Click derecho sobre la clase → **Run 'XxxServiceTest' with Coverage** (ícono con escudo).
3. Los resultados aparecen en el panel inferior (✅ pasó / ❌ falló) y a la derecha se abre el panel **Coverage** con el porcentaje por paquete, clase y línea.
4. Para verificar la **cobertura ≥ 80% sobre la lógica de negocio**, expandir el paquete `service` del microservicio en el panel Coverage y revisar la columna *Line %*.
5. (Opcional) Exportar evidencia: en el panel Coverage → ícono de exportar → **Generate Coverage Report** genera un reporte HTML navegable.

> La cobertura se mide sobre la **capa de servicio** (paquete `service`), que es donde reside la lógica de negocio y las reglas del dominio. Clases de configuración, DTOs y la clase `Application` no forman parte de la métrica.

### Cobertura por servicio (capa service)

| Servicio | Cobertura línea (paquete `service`) |
|---|---|
| ms-auth | _completar tras ejecutar with Coverage_ |
| ms-user | _completar_ |
| ms-restaurant | _completar_ |
| ms-order | _completar_ |
| payment-service | _completar_ |
| ms-delivery | _completar_ |
| ms-notification | _completar_ |
| ms-review | _completar_ |
| search-service | _completar_ |
| report-service | _completar_ |



---

## Eventos Kafka implementados

| Tópico | Evento | Publicado por | Consumido por | Descripción |
|---|---|---|---|---|
| `order-events` | `ORDER_CREATED` | order-service | notification-service | Pedido creado |
| `order-events` | `ORDER_STATUS_UPDATED` | order-service | notification-service | Cambio de estado intermedio |
| `order-delivered` | `ORDER_DELIVERED` | order-service | notification-service, report-service | Pedido entregado |
| `payment.completed` | `PAYMENT_COMPLETED` | payment-service | notification-service | Pago exitoso |
| `payment.failed` | `PAYMENT_FAILED` | payment-service | notification-service | Pago fallido |
| `stock.low` | `STOCK_LOW` | restaurant-service | notification-service | Stock de ítem en 0 |
| `order.placed` | `ORDER_PLACED` | order-service | report-service | Pedido generado para reporte |
| `delivery.completed` | `DELIVERY_COMPLETED` | delivery-service | — | Entrega completada |

---

## Rutas del API Gateway

Todas las peticiones entran por `http://localhost:9090`. El gateway valida el token JWT y propaga los headers `X-User-Email` y `X-User-Role` a cada microservicio.

| Prefijo | Microservicio | Puerto directo |
|---|---|---|
| `/auth/**` | ms-auth | 9081 |
| `/users/**` | ms-user | 9082 |
| `/restaurants/**` | ms-restaurant | 9083 |
| `/orders/**` | ms-order | 9084 |
| `/payments/**` | ms-payment | 9085 |
| `/deliveries/**` | ms-delivery | 9086 |
| `/notifications/**` | ms-notification | 9087 |
| `/reviews/**` | ms-review | 9088 |
| `/search/**` | ms-search | 9089 |
| `/reports/**` | ms-report | 9095 |

**Rutas públicas** (sin token JWT requerido):
- `POST /auth/register`
- `POST /auth/login`
- `GET /search/restaurants`

**Rutas que requieren rol ADMIN:**
- `GET /reports/all`
- `GET /reports/restaurant/{id}`
- `POST /payments/{id}/refund`

---

## Requisitos previos

- **Docker Desktop** (con Docker Compose v2) — [descargar aquí](https://www.docker.com/products/docker-desktop/)
- **Java 21** y **Maven 3.8+** — solo si se quiere ejecutar algún servicio de forma local
- Puerto **3307** libre en el host (MySQL se expone en este puerto para no colisionar con instalaciones locales)
- Puerto **9092** libre en el host (Kafka)
- Puerto **2181** libre en el host (Zookeeper)

---

## Cómo ejecutar el proyecto

### Con Docker Compose (recomendado)

```bash
# 1. Clonar el repositorio
git clone <url-del-repositorio>
cd EvaluacionN3-main

# 2. Construir y levantar todos los servicios
docker compose up --build
```

El primer arranque puede demorar **2-3 minutos** porque Docker construye las imágenes y MySQL inicializa todas las bases de datos.

Para reconstruir desde cero (sin caché):

```bash
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

**Verificar que todo esté funcionando:**

| Servicio | URL |
|---|---|
| Eureka Dashboard | http://localhost:9761 |
| API Gateway | http://localhost:9090/webjars/swagger-ui/index.html |

> **Tip:** El API Gateway enruta todas las peticiones. Una vez que Eureka muestra los servicios como `UP`, el sistema está listo para recibir requests. El tiempo de estabilización completo es de aproximadamente 60–90 segundos.

### Comandos útiles

```bash
# Ver logs de un servicio específico
docker logs foodmarket-order -f
docker logs foodmarket-notification -f
docker logs foodmarket-report -f

# Detener todos los servicios
docker-compose down

# Detener y eliminar volúmenes (base de datos limpia)
docker-compose down -v
```

---

## Microservicios

### 1. auth-service — Puerto 9081 · BD: `db_auth`

Gestiona la autenticación del sistema mediante JWT. Los usuarios de prueba se persisten en la base de datos `db_auth`. Genera tokens con roles (`CUSTOMER`, `RESTAURANT_OWNER`, `ADMIN`) que el API Gateway propaga a todos los servicios mediante los headers `X-User-Email` y `X-User-Role`.

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/auth/register` | Registrar cuenta nueva |
| `POST` | `/auth/login` | Iniciar sesión y obtener token JWT |

---

### 2. user-service — Puerto 9082 · BD: `db_user`

Gestiona los perfiles de usuario y sus direcciones de entrega. Implementa relación `OneToMany` entre perfil y direcciones, y valida que no existan perfiles duplicados para el mismo `userId`.

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/users/{userId}/profile` | Crear perfil de usuario |
| `GET` | `/users/{userId}/profile` | Obtener perfil |
| `POST` | `/users/{userId}/addresses` | Agregar dirección |
| `GET` | `/users/{userId}/addresses` | Listar direcciones activas |
| `DELETE` | `/users/{userId}/addresses/{addressId}` | Desactivar dirección |

---

### 3. restaurant-service — Puerto 9083 · BD: `db_restaurant`

Administra el catálogo de restaurantes y sus menús. Regla de negocio: cuando un ítem llega a `stock = 0`, se marca automáticamente como `available = false` y Kafka publica el evento `stock.low`. Implementa relación `ManyToOne` entre `MenuItem` y `Restaurant`.

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/restaurants` | Crear restaurante (rol: `RESTAURANT_OWNER` / `ADMIN`) |
| `GET` | `/restaurants/{id}` | Obtener restaurante por ID |
| `GET` | `/restaurants/zone/{zone}` | Listar restaurantes abiertos por zona |
| `POST` | `/restaurants/{id}/menu` | Agregar ítem al menú |
| `GET` | `/restaurants/{id}/menu` | Ver menú disponible |
| `GET` | `/restaurants/{id}/menu/{itemId}` | Obtener ítem específico del menú |
| `PATCH` | `/restaurants/{id}/menu/{itemId}/stock` | Actualizar stock de ítem |
| `PATCH` | `/restaurants/{id}/status` | Abrir / cerrar restaurante |

---

### 4. order-service — Puerto 9084 · BD: `db_order`

Servicio central del flujo de negocio. Al crear un pedido, valida en tiempo real (vía OpenFeign) que el restaurante esté abierto, que la zona coincida y que los ítems tengan stock. Gestiona el ciclo de vida del pedido con transiciones validadas:

```
PENDING → CONFIRMED → PREPARING → READY → IN_DELIVERY → DELIVERED
         └─ CANCELLED (desde PENDING o CONFIRMED)
```

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/orders` | Crear pedido |
| `GET` | `/orders/{id}` | Obtener pedido por ID |
| `GET` | `/orders/customer/{id}` | Historial de pedidos del cliente |
| `PATCH` | `/orders/{id}/status` | Cambiar estado del pedido |

---

### 5. payment-service — Puerto 9085 · BD: `db_payment`

Simula el procesamiento de pagos con 90% de probabilidad de éxito. Valida que no exista un pago completado para la misma orden. Publica `payment.completed` o `payment.failed` según el resultado. Solo el rol `ADMIN` puede emitir reembolsos.

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/payments` | Procesar pago |
| `GET` | `/payments/order/{id}` | Obtener pago por orden |
| `GET` | `/payments/customer/{id}` | Historial de pagos del cliente |
| `POST` | `/payments/{id}/refund` | Emitir reembolso (solo `ADMIN`) |

---

### 6. delivery-service — Puerto 9086 · BD: `db_delivery`

Asigna automáticamente el primer repartidor disponible en la zona del pedido. Al completar la entrega (`DELIVERED`), libera al repartidor y publica los eventos `delivery.completed` y `order.delivered`.

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/deliveries/agents` | Registrar repartidor |
| `POST` | `/deliveries/assign` | Asignar repartidor a pedido |
| `PATCH` | `/deliveries/{id}/status` | Actualizar estado de la entrega |
| `GET` | `/deliveries/order/{id}` | Obtener entrega por pedido |

---

### 7. notification-service — Puerto 9087 · BD: `db_notification`

Consumer Kafka puro: escucha 6 tópicos y persiste las notificaciones en base de datos. No produce eventos, solo reacciona a los de otros servicios.

**Tópicos consumidos:**

| Tópico | Mensaje generado |
|---|---|
| `order.placed` | "Nuevo pedido recibido" |
| `order.confirmed` | "Pedido confirmado" |
| `payment.completed` | "Pago exitoso" |
| `payment.failed` | "Pago fallido, reintenta" |
| `order.delivered` | "Pedido entregado, califícalo" |
| `stock.low` | "Alerta de stock bajo" |

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/notifications/{userId}` | Todas las notificaciones del usuario |
| `GET` | `/notifications/{userId}/unread` | Solo las no leídas |
| `PATCH` | `/notifications/{id}/read` | Marcar como leída |

---

### 8. review-service — Puerto 9088 · BD: `db_review`

Permite calificar restaurantes y repartidores. Valida vía OpenFeign que el pedido esté en estado `DELIVERED` antes de permitir la reseña. Previene reseñas duplicadas del mismo tipo para el mismo pedido.

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/reviews` | Crear reseña |
| `GET` | `/reviews/restaurant/{id}` | Reseñas de un restaurante |
| `GET` | `/reviews/agent/{id}` | Reseñas de un repartidor |
| `GET` | `/reviews/restaurant/{id}/average` | Rating promedio del restaurante |

---

### 9. search-service — Puerto 9089 · BD: `db_search`

Permite buscar restaurantes por nombre (búsqueda `LIKE`), zona o categoría. Mantiene un índice local (`RestaurantIndex`) ordenado por rating promedio. **Es la única ruta pública que no requiere token JWT.**

El índice se puebla llamando a `POST /search/restaurants/index` cada vez que se crea un restaurante.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/search/restaurants` | Listar todos los restaurantes |
| `GET` | `/search/restaurants?name=X` | Buscar por nombre (parcial) |
| `GET` | `/search/restaurants?zone=X` | Buscar por zona (orden por rating) |
| `GET` | `/search/restaurants?category=X` | Buscar por categoría |
| `POST` | `/search/restaurants/index` | Indexar o actualizar restaurante |

---

### 10. report-service — Puerto 9095 · BD: `db_report`

Consume eventos Kafka de `order.placed` y `order.delivered` para registrar un historial de pedidos. Expone reportes de uso accesibles solo para el rol `ADMIN`.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/reports/all` | Reporte global de pedidos (solo `ADMIN`) |
| `GET` | `/reports/restaurant/{id}` | Reporte por restaurante (solo `ADMIN`) |

---

## Flujo completo de un pedido

```
1. POST /auth/login
   └─ Obtener token JWT

2. GET  /search/restaurants?zone=X        (sin token requerido)
   └─ Buscar restaurante y ver menú

3. POST /orders
   ├─ Feign → verifica que el restaurante esté abierto
   ├─ Feign → verifica stock de cada ítem
   └─ Kafka → publica "order.placed"

4. POST /payments
   └─ Kafka → publica "payment.completed"

5. PATCH /orders/{id}/status              (CONFIRMED → PREPARING → READY)

6. POST /deliveries/assign
   └─ Asigna el primer repartidor disponible en la zona

7. PATCH /deliveries/{id}/status          (IN_DELIVERY → DELIVERED)
   └─ Kafka → publica "order.delivered"

8. POST /reviews                          (solo si el pedido está DELIVERED)
   └─ Feign → verifica estado del pedido
```

---

## Flujo completo con eventos Kafka

```
1. POST /auth/login                     → obtener token JWT

2. POST /restaurants                    → crear restaurante
   POST /search/restaurants/index       → indexar en search-service

3. POST /restaurants/{id}/menu          → agregar ítems al menú

4. POST /orders                         → crear pedido
   └─ Feign → valida restaurante abierto y stock
   └─ Kafka → publica ORDER_CREATED en topic "order-events"
              └─ notification-service crea notificación al cliente

5. PATCH /orders/{id}/status (CONFIRMED → PREPARING → READY → IN_DELIVERY)
   └─ Kafka → publica ORDER_STATUS_UPDATED en topic "order-events"
              └─ notification-service notifica cada cambio

6. PATCH /orders/{id}/status?newStatus=DELIVERED
   └─ Kafka → publica ORDER_DELIVERED en topic "order-delivered"
              ├─ notification-service → notificación de entrega
              └─ report-service       → guarda OrderSummary con totalAmount

7. POST /payments                       → procesar pago de la orden

8. POST /deliveries/agents              → registrar agente
   POST /deliveries/assign              → asignar agente al pedido
   PATCH /deliveries/{id}/status        → actualizar estado de entrega

9. POST /reviews                        → reseña (valida via Feign que esté DELIVERED)

10. GET /reports/all                    → ver reportes (requiere token ADMIN)
    GET /notifications/{userId}         → ver todas las notificaciones Kafka
```

---

## Notas de implementación

- El **servicio de pagos** simula una pasarela real: 90% de probabilidad de `COMPLETED` y 10% de `FAILED`. Si resulta `FAILED`, el reembolso no estará disponible para ese pago.
- El **índice de búsqueda** se puebla manualmente con `POST /search/restaurants/index` después de crear cada restaurante.
- Los **reportes** requieren token ADMIN en el header `Authorization`.
- Los **logs** se persisten en la carpeta `logs/` montada como volumen Docker compartido entre todos los microservicios.
- Las **pruebas unitarias** usan Mockito para simular repositorios y JwtUtil, aislando completamente la lógica de negocio.
- Todos los pedidos quedan asociados al `customerId = 1` (primer usuario registrado en la base de datos).
