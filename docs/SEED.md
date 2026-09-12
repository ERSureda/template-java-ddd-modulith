# SEED SPECIFICATION — Semilla de Arquitectura Modular Hexagonal con Spring Modulith

> **Naturaleza del documento.** Especificación técnica del repositorio base ejecutable. Define el código transversal que se construye una sola vez, la frontera exacta frente a los generadores de negocio, la suite de verificación automatizada y los criterios de aceptación ejecutables para cualquier proyecto que adopte esta arquitectura.
>
> **Documento normativo asociado.** `ARCHITECTURE.md`. Toda regla y convención técnica citada en esta especificación (`ARC-*`, `DOM-*`, `APP-*`, `INP-*`, `OUT-*`, `TRX-*`, `SHR-*`) deriva directamente de dicho manual.

---

## 1. Delimitación de Fronteras: Semilla vs. Generadores

El repositorio semilla contiene exclusivamente **el código transversal, agnóstico a subdominios específicos y de configuración técnica** que compila y se ejecuta en el pipeline de integración continua.

| Componente / Artefacto | Responsable | Justificación Técnica |
| --- | --- | --- |
| **Kernel compartido (`shared/`)** | **Semilla** | Invariable entre módulos; define tipos base de dominio, contexto y jerarquía de errores. |
| **Maquinaria Outbox (Relay, DLQ, Purga)** | **Semilla** | Infraestructura de entrega garantizada idéntica en toda la aplicación. |
| **Filtros de Contexto y Gateway Offloading** | **Semilla** | Reconstrucción transversal de identidad y contexto (`ExecutionContext`). |
| **Guardianes CI (Spring Modulith + ArchUnit)** | **Semilla** | Suite de validación estricta de las reglas del manual normativo que rompen el build ante fallos. |
| **Build y Perfiles (Toolchain, Virtual Threads)** | **Semilla** | Configuración base de empaquetado, Java 25, Dockerfile y perfiles de entorno parametrizables (`rootProject.name = providers.gradleProperty('projectName').getOrElse('<project-name>')`). |
| **Módulo de Referencia (`<subdominio-referencia>`)** | **Semilla** | Slice vertical canónico para que los tests de arquitectura verifiquen código real compilado. |
| **Agregados, Entidades y Value Objects** | Generador / Desarrollador | Específico del modelo de negocio de cada subdominio. |
| **Casos de Uso (`*UseCase`) y Servicios** | Generador / Desarrollador | Uno por cada operación o intención funcional. |
| **Entidades JPA, Repositorios JDBC y Mappers** | Generador / Desarrollador | Persistencia acoplada al esquema relacional de cada Bounded Context. |
| **Controladores REST y DTOs de transporte** | Generador / Desarrollador | Contratos HTTP específicos de cada API de módulo. |
| **Migraciones Flyway por esquema** | Generador / Desarrollador | DDL de evolución del esquema específico del subdominio. |

---

## 2. Inventario Técnico del Kernel Compartido (`shared/`)

Configurado bajo el paquete `<namespace.base>.shared` y declarado formalmente como módulo transversal abierto mediante `@ApplicationModule(type = ApplicationModule.Type.OPEN)` en su archivo `package-info.java`.

### 2.1 Dominio Base (`shared.domain`)

* **`AggregateRoot<ID>`:** Clase base abstracta genérica. Encapsula el campo `version` para concurrencia optimista (`TRX-02`), la colección protegida de `DomainEvent` acumulados durante la mutación de estado (`registerEvent()`) y el método de vaciado atómico para persistencia (`pullDomainEvents()`).
* **`BaseEntity<ID>`:** Clase base genérica con igualdad y cálculo de hash basados estrictamente en el identificador único.
* **`DomainEvent`:** Contrato inmutable que implementan todos los eventos de dominio. Exige:
  * `eventId` (UUIDv7).
  * `occurredAt` (Instant).
  * `aggregateId` (String).
  * `eventType` (String lógico versionado, ej. `<subdominio>.<evento>.v1`).
* **Jerarquía de Excepciones de Coste Cero (Java 25):**
  * `BaseException`: Excepción de negocio no comprobada con `writableStackTrace = false`. Elimina la penalización de CPU y memoria de rellenar la traza de ejecución al lanzarse por validación o reglas de negocio.
  * `ErrorCode`: Interfaz que obliga a exponer `getCode()` (código alfanumérico estable, ej. `RESOURCE_NOT_FOUND`) y `getCategory()`.
  * `ErrorCategory`: Enum transversal (`VALIDATION`, `NOT_FOUND`, `CONFLICT`, `UNAUTHORIZED`, `FORBIDDEN`, `INTERNAL`).

### 2.2 Aplicación Base (`shared.application`)

* **`ExecutionContext`:** Record inmutable que transporta el contexto de la petición: `tenantId` (UUID), `userId` (UUID) y `roles` (Set).
* **`ExecutionContextHolder`:** Portador estático desacoplado, seguro frente a la concurrencia masiva de Virtual Threads en Java 25.
* **Comandos y Consultas Híbridos:** Comandos exclusivos Web y Consultas (`Query`) definidos como records planos sin validación interna (apoyados en `@Valid` web); comandos multicanal (colas Kafka/RabbitMQ, eventos, schedulers) con validación defensiva inmediata fail-fast (`Objects.requireNonNull`).
* **`PageResult<T>`:** Record inmutable transversal para respuestas paginadas: `items` (List), `page` (int), `size` (int), `totalElements` (long) y `totalPages` (int).
* **Puertos Secundarios Transversales (`shared.application.port`):**
  * `UuidGeneratorPort`: Contrato para la generación de identificadores basados en tiempo UUIDv7.
  * `OutboxPublisherPort`: Contrato para encolar eventos en la tabla `outbox_events` dentro de la transacción activa.

### 2.3 Infraestructura y Web Base (`shared.infrastructure`)

* **`UuidGeneratorAdapter`:** Implementación de `UuidGeneratorPort` basada en RFC 9562 / UUIDv7.
* **`ApiHeaders`:** Contrato de cabeceras HTTP provenientes del API Gateway o proxies perimetrales: `X-Tenant-Id`, `X-User-Id`, `X-Roles`.
* **`ExecutionContextFilter`:** `OncePerRequestFilter` que extrae las cabeceras HTTP, inicializa el `ExecutionContext` en el holder y garantiza su limpieza en el bloque `finally`.
* **`GlobalExceptionHandler`:** `@RestControllerAdvice` que captura `BaseException`, fallos de validación de Spring y excepciones no controladas. Traduce cualquier fallo al payload estándar ultraligero `ErrorResponse`:

```json
{
  "status": 400,
  "code": "VALIDATION_FAILED",
  "detail": "Descripción del fallo funcional o de contrato",
  "errors": []
}
```

En entornos productivos, el campo `detail` se enmascara ante errores de servidor (500) para evitar la fuga de información técnica.

---

## 3. Maquinaria del Transactional Outbox

El semilla implementa el ciclo de vida completo de entrega asíncrona fiable para dar soporte a `TRX-03`, `TRX-06` y cumplir la regla de adopción diferida `ARC-05`.

### 3.1 Esquema de Persistencia Unificado (`outbox_events`)

Cada módulo que emita eventos cuenta con su propia tabla de outbox en su respectivo esquema relacional:

```sql
CREATE TABLE outbox_events (
    event_id UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    locked_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_processing 
ON outbox_events (status, created_at) 
WHERE status IN ('PENDING', 'PROCESSING');
```

### 3.2 Componentes del Circuito de Relay

1. **Reserva Atómica de Lotes (`OutboxRelay`):**
   * Proceso periódico programado (`@Scheduled(fixedDelayString = "${outbox.poll-interval:1000}")`).
   * Bloqueo pesimista sin contención concurrente mediante SQL nativo con `SKIP LOCKED`:
     ```sql
     SELECT * FROM outbox_events 
     WHERE status = 'PENDING' 
     ORDER BY created_at ASC 
     LIMIT :batchSize 
     FOR UPDATE SKIP LOCKED;
     ```
   * Transición inmediata del lote a estado `PROCESSING` registrando la marca temporal en `locked_at`.

2. **Despacho y Reintentos:**
   * El evento se serializa a JSON y se entrega al `EventDispatcher`.
   * En caso de entrega confirmada, pasa a estado `DELIVERED`.
   * En caso de error, incrementa `retry_count` y aplica backoff exponencial con tope de reintentos (máximo 5).
   * Superado el límite de fallos, pasa a estado definitivo `DEAD_LETTER` registrando la causa en `last_error`.

3. **Endpoint de Operación y Replay:**
   * `POST /internal/outbox/replay`: Endpoint administrativo restringido que recibe identificadores de eventos en `DEAD_LETTER` y los reinserta en estado `PENDING` para su reintento.

4. **Purga Automática de Entregados (`TRX-06`):**
   * Scheduler configurable (`@Scheduled(cron = "${outbox.purge-cron:0 0 3 * * ?}")`) que elimina registros con estado `DELIVERED` cuya antigüedad supere la retención definida (por defecto: 7 días).

---

## 4. Guardianes Automatizados de Arquitectura (Tests de CI)

La suite de validación reside en `src/test/java/<namespace.base>/architecture/` y se ejecuta obligatoriamente en cada compilación. Si alguna aserción falla, el pipeline aborta de inmediato.

```text
src/test/java/<namespace.base>/architecture/
├── ModulithStructureTest.java              # Verificación de fronteras Spring Modulith
├── DomainRulesArchTest.java                # Aislamiento DOM-01 y ausencia de setters DOM-04
├── ApplicationRulesArchTest.java           # Nomenclatura y transacciones APP-* y TRX-01
├── PersistenceRulesArchTest.java           # Confinamiento OUT-01 y OUT-05
└── WebRulesArchTest.java                   # Fronteras de adaptadores primarios INP-*
```

### 4.1 `ModulithStructureTest`

```java
@Test
void verifyModulithStructure() {
    ApplicationModules modules = ApplicationModules.of(Application.class);
    modules.verify(); // Detecta acoplamientos ilegales y valida @NamedInterface
}

@Test
void generateArchitectureDocumentation() {
    new Documenter(ApplicationModules.of(Application.class))
        .writeDocumentation()
        .writeIndividualFilesAsPlantUml();
}
```

### 4.2 Reglas ArchUnit Centralizadas

* **`DOM-01` (Aislamiento Total del Dominio):**
  ```java
  noClasses().that().resideInAPackage("..domain..")
      .should().dependOnClassesThat().resideInAnyPackage(
          "org.springframework..", "jakarta.persistence..", 
          "com.fasterxml.jackson..", "lombok.."
      ).check(classes);
  ```

* **`DOM-04` (Inmutabilidad y Ausencia de Setters en Agregados):**
  ```java
  methods().that().areDeclaredInClassesThat().resideInAPackage("..domain.model..")
      .and().arePublic()
      .should().notHaveNameStartingWith("set")
      .as("DOM-04: Los agregados y entidades de dominio no deben exponer setters públicos")
      .check(classes);
  ```

* **`APP-01` (Estructura y Nomenclatura de Casos de Uso):**
  ```java
  classes().that().resideInAPackage("..application.service..")
      .should().haveSimpleNameEndingWith("Service")
      .andShould().implement(
          described("exactamente una interfaz UseCase",
          javaClass -> javaClass.getInterfaces().size() == 1 &&
          javaClass.getInterfaces().get(0).getSimpleName().endsWith("UseCase"))
          ).check(classes);
  ```

* **`OUT-01` (Confinamiento de Entidades ORM):**
  ```java
  classes().that().areAnnotatedWith(jakarta.persistence.Entity.class)
      .should().resideInAPackage("..infrastructure.adapter.out.persistence.jpa..")
      .check(classes);
  ```

* **`OUT-05` (Confinamiento de JDBC Nativo):**
  ```java
  classes().that().dependOnClassesThat().haveFullyQualifiedName("org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate")
      .should().resideInAPackage("..infrastructure.adapter.out.persistence.jdbc..")
      .check(classes);
  ```

* **`TRX-01` (Delimitación Transaccional en Aplicación):**
  ```java
  methods().that().areAnnotatedWith(org.springframework.transaction.annotation.Transactional.class)
      .should().beDeclaredInClassesThat().resideInAPackage("..application..")
      .check(classes);
  ```

* **`INP-01` (Desacoplamiento de Adaptadores Web):**
  ```java
  noClasses().that().resideInAPackage("..adapter.in.web..")
      .should().dependOnClassesThat().resideInAPackage("..domain.model..")
      .andShould().dependOnClassesThat().resideNotInPackage("..domain.model.enums..")
      .check(classes);
  ```

---

## 5. Módulo de Referencia Canónico (`<subdominio-referencia>`)

El repositorio semilla incluye un Bounded Context funcional mínimo de referencia. Este módulo no contiene lógica corporativa propietaria, sino que sirve para que las pruebas de arquitectura compilen y verifiquen un slice vertical completo.

### 5.1 Estructura del Slice Vertical

* **Dominio (`<namespace.base>.<subdominio>.domain`):**
  * Agregado raíz con métodos factoría `create(...)` y `reconstruct(...)`. Protege sus invariantes mediante Value Objects inmutables y estados gestionados por enums puros.
  * Evento de dominio emitido al mutar el estado (`registerEvent`).
  * Catálogo de errores que implementa `ErrorCode` con códigos estables.

* **Aplicación (`<namespace.base>.<subdominio>.application`):**
  * Archivo `package-info.java` anotado con `@NamedInterface("application")`.
  * Comando inmutable de escritura (record plano para casos de uso exclusivos Web; con validación fail-fast `Objects.requireNonNull` si es multicanal).
  * Interfaz de caso de uso (`*UseCase`) implementada en un `@Service` con demarcación `@Transactional`.
  * Puerto de salida para persistencia (`*RepositoryPort`).
  * Consulta inmutable de lectura (`*Query`) definida como record plano de una sola línea optimizada con `@Transactional(readOnly = true)`.

* **Infraestructura (`<namespace.base>.<subdominio>.infrastructure`):**
  * Adaptador JPA: `@Entity` dedicada, repositorio Spring Data y mapper explícito bidireccional.
  * Adaptador JDBC: Implementación con `NamedParameterJdbcTemplate` para consultas optimizadas directas a DTO o tipos SQL nativos avanzados.
  * Adaptador Web: Controlador REST que valida payloads sintácticos (`@Valid`), invoca el caso de uso y devuelve DTOs de salida (*Result).
  * Adaptador Worker: Consumidor de eventos con control de idempotencia (`TRX-05`) que verifica el `eventId` antes de procesar el mensaje.

---

## 6. Criterios de Aceptación Ejecutables (*Definition of Done*)

El repositorio semilla se declara completo y apto para ser clonado como base de nuevos proyectos cuando estos 6 escenarios pasan en verde en el entorno de CI:

1. **Compilación y Guardianes Verdes:**
   * `./gradlew check` (o `./mvnw verify`) compila sin advertencias en Java 25 y pasa la totalidad de los tests unitarios, de integración, la verificación de Spring Modulith y las aserciones de ArchUnit.

2. **Camino de Escritura y Outbox Transaccional:**
   * Una petición HTTP de creación devuelve `201 Created`.
   * La entidad se almacena en la base de datos y se registra simultáneamente el evento en `outbox_events` con estado `PENDING` dentro de la misma transacción ACID.

3. **Procesamiento y Entrega Asíncrona:**
   * El relay en segundo plano toma el evento pendiente mediante `SKIP LOCKED`, lo despacha con éxito y transiciona la fila a estado `DELIVERED`.

4. **Resiliencia ante Fallos y Dead-Letter Queue:**
   * Al simular un fallo permanente en el consumidor, el evento agota los reintentos con backoff exponencial, pasa a estado `DEAD_LETTER` y puede recuperarse reactivándolo a `PENDING` mediante el endpoint de replay.

5. **Captura Estructurada de Excepciones sin Traza:**
   * Una invocación inválida aborta la transacción y devuelve el payload JSON estructurado bajo `ErrorResponse` con su código alfanumérico estable, sin escribir trazas de pila en los logs de producción.

6. **Parada Limpia (*Graceful Shutdown*):**
   * Ante una señal `SIGTERM` enviada a la aplicación durante la ejecución de un lote de outbox, el proceso finaliza el evento en curso, rechaza nuevos lotes y libera las conexiones de base de datos de manera ordenada.