# tienda-modulith

Demo de un **monolito modular** con Spring Boot y Spring Modulith: una tienda con cuatro módulos
(pedidos, inventario, notificaciones y métricas) en un solo desplegable.

- Java 21
- Spring Boot 4.1.1
- Spring Modulith 2.1.1
- Micrometer + Actuator (métricas) y, opcionalmente, Prometheus + Grafana vía `docker-compose.yml`
- PostgreSQL (docker-compose; localhost:5433, db/user/password: tienda)

## Cómo correrlo

```bash
./mvnw test              # corre todas las pruebas
./mvnw spring-boot:run   # levanta la app en http://localhost:8080
```

La primera ejecución descarga Maven y las dependencias, así que necesita internet. Después funciona sin conexión.

Las peticiones del demo están en `demo.http` (IntelliJ y VS Code con REST Client las ejecutan con un clic).

## Estructura

```
com.example.store
├── StoreApplication
├── EventPublicationsController     (solo para el demo: expone el outbox)
│
├── orders                          módulo "Pedidos"        -> puede depender de inventory
│   ├── OrderManagement             API: servicio de aplicación (agregado Order + Inventory + eventos)
│   ├── OrderSummary                API: DTO de salida
│   ├── events                      API con nombre ("named interface")
│   │   └── OrderPlaced             evento de dominio
│   └── internal                    Order (agregado), OrderRepository, OrderController
│
├── inventory                       módulo "Inventario"     -> no depende de nadie
│   ├── Inventory                   API
│   ├── InsufficientStockException  API
│   ├── StockLevel                  API
│   └── internal                    StockItem (agregado), StockRepository, StockSeeder, InventoryController
│
├── notifications                   módulo "Notificaciones" -> depende solo de orders::events
│   ├── OrderNotifications          package-private (listener)
│   └── ConfirmationMailer          package-private
│
└── metrics                         módulo "Métricas"       -> depende solo de orders::events
    └── OrderMetrics                package-private (listener, incrementa un contador)
```

Términos de DDD que respeta esta base, sin montar una arquitectura hexagonal encima:

- **Bounded context / módulo**: cada paquete directo bajo `com.example.store` es un contexto
  delimitado con su propio lenguaje (`Order`, `StockItem`, nada de "producto" genérico cruzando límites).
- **Agregado**: `Order` y `StockItem` son los únicos que pueden cambiar su propio estado
  (`StockItem.reserve()` revienta si no hay stock; nadie manipula `available` desde afuera).
- **Repositorio**: `OrderRepository` / `StockRepository` son el único acceso a sus agregados.
- **Evento de dominio**: `OrderPlaced` — algo que ya ocurrió, se publica una vez y no se puede
  "cancelar", solo reaccionar a él.
- **Servicio de aplicación**: `OrderManagement` orquesta el agregado, el inventario y los eventos;
  no tiene reglas de negocio propias, solo coordina.

### Tipos de "carpeta" que reconoce Spring Modulith

Spring Modulith no sabe qué significa `internal` ni `events`; solo mira si un tipo está en el
paquete raíz del módulo o en un subpaquete, y si ese subpaquete tiene nombre:

| Dónde vive el tipo | Quién lo ve | Ejemplo |
|---|---|---|
| Paquete raíz del módulo | Cualquier módulo que dependa del módulo entero | `OrderManagement`, `OrderSummary` |
| Subpaquete sin anotar (convención `internal`) | Solo clases del mismo módulo | `orders.internal.Order`, `orders.internal.OrderRepository` |
| Subpaquete anotado `@NamedInterface("nombre")` | Solo quien declare `allowedDependencies = "modulo::nombre"` | `orders.events.OrderPlaced` |

`notifications` y `metrics` son un cuarto caso: módulos que no dejan **nada** en su paquete raíz
(todo package-private), así que no tienen API en absoluto — solo escuchan eventos. Por eso sus
`package-info.java` declaran `allowedDependencies = "orders::events"` en vez de `"orders"`:
ven `OrderPlaced`, pero ni se enteran de que existen `OrderManagement` o `OrderSummary`. Y entre
ellos tampoco se ven: a ninguno le hace falta saber que el otro existe para hacer su trabajo.

Reglas que aplica Spring Modulith (verificadas por `ModularityTests.verifiesModularStructure`):

1. Cada paquete directo bajo `com.example.store` es un módulo.
2. Lo que está en el paquete base del módulo es su API. Lo que está en un subpaquete sin nombre
   (`internal`) es privado. Lo que está en un subpaquete con `@NamedInterface` es una API con
   nombre, visible solo para quien la pida explícitamente.
3. No se permiten ciclos entre módulos.
4. Si un módulo declara `allowedDependencies` en su `package-info.java`, solo puede depender de
   eso — un módulo entero (`"inventory"`) o una API con nombre puntual (`"orders::events"`).

Las dos formas de comunicación entre módulos que muestra el repo:

| Forma | Dónde | Qué garantiza |
|-------|-------|---------------|
| Llamada directa a la API | `OrderManagement.place` llama a `Inventory.reserve` | Misma transacción: si no hay stock, el pedido no se guarda |
| Evento de dominio | `OrderManagement` publica `OrderPlaced`, `OrderNotifications` lo escucha | El pedido no depende de notificaciones; el listener corre después del commit, en otro hilo |

## El outbox

Cuando `OrderManagement.place` publica `OrderPlaced`, Spring Modulith no entrega el evento en
memoria y ya: guarda una fila en la tabla `EVENT_PUBLICATION` — una por cada listener interesado
— **en la misma transacción local que el pedido**. Si el pedido se revierte, esa fila tampoco se
guarda. Después del commit, el listener corre en otro hilo; si termina bien, la fila se marca
completa; si falla, se queda pendiente. Eso es el patrón **Outbox**: la garantía de que "guardé
el pedido" y "voy a notificar que lo guardé" viven en la misma transacción, sin necesitar un
message broker ni una saga.

- `GET /demo/outbox/pendientes` — eventos cuya entrega a algún listener no se completó.
- `spring.modulith.events.republish-outstanding-events-on-restart=true` (en `application.properties`)
  — al reiniciar la app, cada publicación pendiente se reintenta automáticamente.

### Reintentos con tiempo y límite

Spring Modulith (2.1.1) da las piezas para controlar los reintentos, pero **no reintenta solo**:

- Cada publicación guarda cuántas veces se intentó (`getCompletionAttempts()`) y cuándo fue el último reintento.
- `FailedEventPublications.resubmit(...)` acepta una edad mínima (`withMinAge`) y un filtro, que es donde se pone el límite de intentos.

No existe una propiedad tipo "reintenta cada 30 s, máximo 5 veces"; ese job lo escribes tú (aquí ya está hecho):

```java
@Component
class RetryFailedEvents {

    private final FailedEventPublications failed;

    RetryFailedEvents(FailedEventPublications failed) {
        this.failed = failed;
    }

    @Scheduled(fixedDelay = 30_000)   // cada 30 segundos
    void retry() {
        failed.resubmit(ResubmissionOptions.defaults()
                .withMinAge(Duration.ofSeconds(30))                    // espera mínima antes de reintentar
                .withFilter(p -> p.getCompletionAttempts() < 5));      // deja de insistir al quinto intento
    }
}
```

Está implementado en `RetryFailedEvents` (con `@EnableScheduling` en `StoreApplication`). Con el correo
`@falla.test` el reintento falla cada 30 segundos y deja errores en la consola; no se profundiza en esto en la charla.

## Métricas: Prometheus y Grafana

Mismo patrón que `notifications`: un módulo nuevo (`metrics`) que solo escucha `OrderPlaced` y no
toca nada de `orders`. `OrderMetrics` incrementa un `Counter` de Micrometer por cada pedido,
taggeado por `sku`. Spring Boot Actuator lo expone en formato texto en `/actuator/prometheus`
como `pedidos_recibidos_total`.

```bash
./mvnw spring-boot:run     # la app, con /actuator/prometheus ya expuesto
docker compose up -d       # Prometheus (scrapea la app) + Grafana (ya con el dashboard listo)
```

- Prometheus: http://localhost:9090 — pestaña "Status > Targets" para ver el scrape de
  `tienda-modulith` en verde, o la pestaña "Graph" con la consulta `pedidos_recibidos_total`.
- Grafana: http://localhost:3000 (`admin` / `admin`, o entra como anónimo) — el datasource
  "Prometheus" y el dashboard "Tienda Modulith - Pedidos" ya están provisionados, no hay que
  configurar nada a mano. Tiene tres paneles: total de pedidos, pedidos por minuto y pedidos por SKU.
- Todo lo que levanta `docker compose` vive en `observability/` (config de Prometheus,
  datasource y dashboard de Grafana) y en el propio `docker-compose.yml`.
- Nota: el app corre en el host (`./mvnw spring-boot:run`), no dentro de Docker; Prometheus le
  apunta a `host.docker.internal:8080` para poder scrapearlo desde el contenedor.
- `docker compose down` cuando termines, para no dejar nada corriendo de más.

## Guion del demo (~17 min)

### 1. Estructura (2 min)

- Abre el árbol de paquetes, `orders/package-info.java` y `orders/events/package-info.java`.
- Corre `ModularityTests.printsModules`: imprime los módulos detectados, sus beans y la named
  interface `events` de `orders`.

### 2. Verificación (3 min)

- Corre `ModularityTests.verifiesModularStructure`: pasa.
- En `notifications/OrderNotifications.java` descomenta la línea marcada con `DEMO (romper el límite)`.
- El proyecto compila: `OrderRepository` es `public`, el compilador no ve ningún problema.
- Corre de nuevo `verifiesModularStructure`: falla con un mensaje como este:

```
Module 'notifications' depends on non-exposed type com.example.store.orders.internal.OrderRepository within module 'orders'!
```

- Vuelve a comentar la línea.
- Opcional: en `inventory/Inventory.java` hay otra línea marcada. Al descomentarla, inventario depende de pedidos: viola `allowedDependencies` y además crea un ciclo.
- Opcional (named interface): en `notifications/package-info.java` cambia `"orders::events"` por
  `"orders"` — ahora `notifications` vería el módulo completo. Vuelve a dejarlo en `"orders::events"`
  y muestra que, aunque lo cambiaras al revés (dejar `"orders"` y agregar un campo de tipo
  `OrderManagement` en `OrderNotifications`), seguiría compilando: la named interface no bloquea
  nada por sí sola, es `allowedDependencies` quien decide qué puede pedirse.

### 3. Eventos y transacciones (4 min)

Levanta la app y usa `demo.http`:

- Petición 2 (pedido normal): en consola, el pedido y la reserva de stock salen en el hilo del request; el correo sale después en un hilo `task-N`.
- Peticiones 3 y 4 (sin stock): responde 409 y `GET /orders` no muestra ese pedido. Abre `OrderManagement.place` para mostrar por qué: guardar el pedido y reservar stock ocurren en una sola transacción local.
- Peticiones 5 y 6 (el correo falla): el pedido se crea con 201 aunque notificaciones falle, y `GET /demo/outbox/pendientes` muestra el evento que quedó sin completar — es el outbox en acción.
- Opcional: reinicia la app (`Ctrl+C` y `./mvnw spring-boot:run` otra vez) y mira en consola cómo, gracias a `republish-outstanding-events-on-restart`, el evento pendiente se reintenta solo.

### 4. Un módulo nuevo sin tocar pedidos: métricas (2 min)

- `docker compose up -d` (si no lo tenías arriba) y abre `orders/events/OrderPlaced.java` junto a
  `metrics/OrderMetrics.java`: es el mismo evento que ya escuchaba `notifications`, con un segundo
  listener que nadie tuvo que coordinar con `OrderManagement`.
- Dispara un par de pedidos más desde `demo.http` y muestra el panel "Pedidos por minuto" en
  Grafana (http://localhost:3000) moviéndose en vivo.
- El punto para la charla: en un monolito modular, un módulo nuevo que reacciona a algo que ya
  pasó es un `@Component` más. En microservicios sería un nuevo desplegable, su propio pipeline,
  y probablemente el mismo evento viajando por un broker.

### 5. Test de un módulo aislado (3 min)

- Abre `orders/OrdersModuleTests`. `@ApplicationModuleTest` levanta solo el módulo de pedidos; inventario se sustituye con `@MockitoBean`.
- Abre `notifications/NotificationsModuleTests`: no necesita crear un pedido, publica el evento y espera el efecto.
- Corre ambos y compara el tiempo y los beans cargados contra `StoreIntegrationTests` (`@SpringBootTest`, aplicación completa).

### 6. Documentación generada (3 min)

- Corre `ModularityTests.writesDocumentation`.
- Abre `target/spring-modulith-docs`: `components.puml` (diagrama de todos los módulos), `module-<nombre>.puml` y `module-<nombre>.adoc` (un canvas por módulo con su API, sus named interfaces, eventos publicados y eventos escuchados).
- Los `.puml` necesitan un visor de PlantUML (plugin del IDE). Genera las imágenes antes de la charla y tenlas en una slide por si el visor falla.

## Antes de la charla

- [ ] `./mvnw test` pasa en la laptop que vas a usar.
- [ ] Corriste las 7 peticiones de `demo.http` al menos una vez.
- [ ] Probaste descomentar y volver a comentar la línea de `OrderNotifications` (y la de `Inventory`).
- [ ] Probaste reiniciar la app con un evento pendiente y viste el reintento automático.
- [ ] `docker compose up -d` levanta Prometheus y Grafana sin internet (las imágenes ya están en caché local).
- [ ] Abriste Grafana al menos una vez y viste el dashboard "Tienda Modulith - Pedidos" con datos.
- [ ] Tienes los diagramas ya renderizados como imagen.
- [ ] Fuente del IDE y de la consola en tamaño grande.
- [ ] Capturas de pantalla de cada paso, por si no hay red o algo no arranca.
