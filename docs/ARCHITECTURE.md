# Manual de Arquitectura — DDD Hexagonal Modular con Spring Modulith

> **Naturaleza del documento.** Especificación técnica normativa y Fuente Única de Verdad para desarrolladores, revisiones de código, linters de arquitectura y generadores automáticos.
>
> **Fuerza normativa.** Las reglas etiquetadas como `MUST` son obligaciones técnicas cuyo incumplimiento bloquea el pipeline de integración continua o la aprobación de Pull Requests. Las reglas `NEVER` son prohibiciones taxativas que solo admiten excepciones mediante un registro de decisión formal (`ADR`) con condición de salida escrita.

---

## 1. Notación y Catálogo de Ámbitos

### 1.1 Identificadores de Regla

La notación sigue la estructura unívoca: **`ÁMB-nn · FUERZA`**.

* **`MUST`**: Obligación técnica estricta.
* **`NEVER`**: Prohibición técnica absoluta.
* **`[A]`**: Verificación automatizada obligatoria en el pipeline de CI (Spring Modulith / ArchUnit / Linters).
* **`[R]`**: Verificación manual obligatoria en Pull Request mediante checklist.

### 1.2 Catálogo Cerrado de Ámbitos

* **`ARC`**: Principios macro, fronteras modulares y verificación estructural con Spring Modulith.
* **`DOM`**: Modelo de Dominio puro, agregados, invariantes, enums de estado y Value Objects.
* **`APP`**: Casos de Uso (`UseCases`), separación Command/Query (CQRS) y orquestación de aplicación.
* **`INP`**: Adaptadores de Entrada (Controladores REST, Workers, Schedulers).
* **`OUT`**: Adaptadores de Salida (Persistencia Híbrida JPA/JDBC, Clientes HTTP, Brokers).
* **`TRX`**: Delimitación transaccional, concurrencia optimista y Transactional Outbox.
* **`SHR`**: Kernel compartido técnico, contexto de ejecución (`ExecutionContext`) y gestión de errores.

---

## 2. Arquitectura Modular y Estructura de Directorios

El sistema se estructura como un **Monolito Modular** gobernado por **Spring Modulith**, donde cada subdominio de negocio representa un Bounded Context cerrado directamente bajo el paquete base `<namespace.base>`. La infraestructura transversal y técnica reside en `shared/`, configurada como módulo abierto (`@ApplicationModule(type = OPEN)`).

```text
src/main/java/<namespace.base>/
├── Application.java                        # Punto de entrada y raíz de Spring Boot / Modulith
│
├── shared/                                 # Kernel compartido transversal (@ApplicationModule OPEN)
│   ├── domain/
│   │   ├── model/                          # AggregateRoot, BaseEntity
│   │   ├── event/                          # DomainEvent base
│   │   └── exception/                      # Jerarquía base de errores (BaseException, ErrorCode, ErrorCategory)
│   ├── application/
│   │   ├── context/                        # ExecutionContext inmutable y puertos de contexto
│   │   ├── result/                         # Result wrappers transversales (PageResult)
│   │   └── port/                           # Puertos transversales (UuidGeneratorPort, OutboxPublisherPort...)
│   └── infrastructure/
│       ├── adapter/                        # UuidGeneratorAdapter (UUIDv7), ExecutionContextAdapter
│       ├── context/                        # ExecutionContextHolder (ThreadLocal seguro para Virtual Threads)
│       └── web/
│           ├── ApiHeaders.java             # Contrato de cabeceras de Gateway Authentication Offloading
│           ├── ExecutionContextFilter.java # OncePerRequestFilter de reconstrucción de contexto
│           └── error/                      # ErrorResponse, ErrorCategoryHttpMapper, GlobalExceptionHandler
│
└── <subdominio>/                           # Módulo Bounded Context
    │
    ├── package-info.java                   # Declaración del módulo raíz Modulith
    │
    ├── application/                        # API PÚBLICA DEL MÓDULO (@NamedInterface("application"))
    │   ├── package-info.java               # Declara la interfaz pública nombrada para otros módulos
    │   ├── command/                        # Comandos de escritura (Records planos para Web; fail-fast si son multicanal)
    │   ├── query/                          # Consultas de lectura (Records planos de parámetros y filtros)
    │   ├── port/
    │   │   ├── in/                         # Casos de uso / interfaces primarias (*UseCase)
    │   │   └── out/                        # Puertos secundarios (repositorios y gateways externos)
    │   ├── service/                        # Implementaciones de UseCases (*Service transaccionales o de lectura)
    │   ├── result/                         # DTOs de salida de la aplicación (*Result records, PageResult)
    │   └── mapper/                         # Mappers de aplicación
    │
    ├── domain/                             # DETALLE INTERNO PRIVADO (agnóstico a frameworks)
    │   ├── model/                          # Agregados, Entidades, Value Objects
    │   │   └── enums/                      # Enums de estado de negocio
    │   ├── event/                          # Eventos de dominio generados por mutaciones
    │   └── <Subdominio>Error.java          # Catálogo de códigos de error del módulo (implements ErrorCode)
    │
    └── infrastructure/                     # DETALLE INTERNO PRIVADO (tecnología y frameworks)
        └── adapter/
            ├── in/                         # Adaptadores Primarios (Driving)
            │   ├── web/                    # Controladores REST, Request DTOs, Mappers Web
            │   └── worker/                 # Consumidores de eventos, Schedulers
            └── out/                        # Adaptadores Secundarios (Driven)
                ├── persistence/            # Persistencia desacoplada
                │   ├── jpa/                # ORM estándar: Entidades JPA (@Entity), Repositories, Mappers
                │   └── jdbc/               # SQL avanzado directo (NamedParameterJdbcTemplate)
                ├── messaging/              # Publicadores al Message Broker o Relay Worker
                └── client/                 # Clientes HTTP hacia APIs de terceros
```

---

## 3. Matriz de Dependencias y Fronteras Modulares

### 3.1 Regla Direccional y Visibilidad

1. **Flujo Hexagonal (Dentro del Módulo):** Las dependencias fluyen estrictamente desde el exterior hacia el centro: **Adaptadores ➔ Aplicación ➔ Dominio**. El Dominio no conoce tecnologías ni frameworks; la Aplicación solo conoce al Dominio y al Kernel compartido (`shared`).
2. **Fronteras Modulith (Entre Módulos):** Un módulo **solo** puede interactuar con otro invocando los casos de uso o DTOs expuestos en su interfaz pública nombrada `application` (`@NamedInterface("application")`) o suscribiéndose a sus eventos asíncronos. Queda estrictamente prohibido acceder al `domain` o `infrastructure` de otro módulo.

| Capa / Componente | Puede depender de | Prohibido depender de |
| --- | --- | --- |
| **Dominio** (`domain`) | Tipos base de `shared/domain`, tipos nativos del lenguaje | Aplicación, Adaptadores, Frameworks (Spring), ORM (JPA/Hibernate), librerías de serialización (Jackson) |
| **Aplicación** (`application`) | `domain` local, `shared/domain`, `shared/application`, `@NamedInterface("application")` de otros módulos | Adaptadores locales (`infrastructure.adapter.*`), Bases de datos, Tecnologías de transporte (HTTP/REST), paquetes internos de otros módulos |
| **Adaptadores Entrada** (`adapter.in`) | `application` local, `shared`, `domain.model.enums.*` locales (enums puros de estado) | Entidades completas de `domain.model`, Adaptadores de salida (`adapter.out`), cualquier paquete de otro módulo |
| **Adaptadores Salida** (`adapter.out`) | Puertos de `application/port/out` locales, tipos de dominio para mapeo, `shared` | Adaptadores de entrada locales, controladores, cualquier paquete de otro módulo |
| **Kernel Compartido** (`shared`) | Tipos nativos del lenguaje, utilidades agnósticas | Cualquier módulo de negocio específico (`<namespace.base>.<subdominio>.*`) |

---

## 4. Flujo de Operaciones

### 4.1 Camino de Escritura (Command Flow — Mutación de Estado)

Cuando se produce una modificación en el sistema, **es obligatorio hidratar el Agregado de Dominio** para validar las reglas de negocio e invariantes:

```text
[Cliente HTTP] 
      │
      ▼
1. [Adapter IN (Controller)]  ──► Valida payload sintáctico (@Valid). 
      │                           Mapea Request a Command inmutable (record plano sin validación interna redundante).
      ▼
2. [Application (Service)]    ──► Abre @Transactional. 
      │                           Carga e hidrata el Agregado de Dominio invocando application/port/out.
      ▼
3. [Domain (AggregateRoot)]   ──► Ejecuta método de negocio semántico.
      │                           Valida invariantes y transiciones de estado legales. Encola DomainEvent.
      ▼
4. [Adapter OUT (Repository)] ──► Persiste el nuevo estado del Agregado en BD (JPA o JDBC).
      │                           Inserta DomainEvent en la tabla 'outbox_events' (misma transacción ACID).
      ▼
5. [Application (Service)]    ──► Cierra transacción. Retorna DTO de resultado (*Result).
      │                           (O lanza excepción tipada de coste cero BaseException si falló).
      ▼
6. [Adapter IN (Controller)]  ──► Devuelve HTTP Status correspondiente (200 OK, 201 Created).
                                  Si hay fallo, GlobalExceptionHandler traduce la excepción a ErrorResponse.
```

### 4.2 Camino de Lectura (Query Flow — CQRS Ligero)

Cuando se consultan datos para mostrarlos en pantallas o APIs, **queda prohibido hidratar Agregados de dominio pesados**. Se proyecta directamente desde la base de datos a DTOs de salida para maximizar el rendimiento y la eficiencia de memoria:

```text
[Cliente HTTP]
      │
      ▼
1. [Adapter IN (Controller)]  ──► Mapea parámetros de búsqueda y paginación a un objeto Query plano (sin validación interna).
      │
      ▼
2. [Application (Service)]    ──► Ejecuta caso de uso bajo @Transactional(readOnly = true).
      │                           Invoca el puerto de lectura en application/port/out.
      ▼
3. [Adapter OUT (Proyección)] ──► Ejecuta consulta SQL optimizada (JPA Projection / JDBC / DTO mapping).
      │                           Mapea directamente desde BD a los DTOs de salida (*Result).
      ▼
4. [Adapter IN (Controller)]  ──► Devuelve HTTP 200 con el DTO o PageResult paginado.
```

---

## 5. Reglas Normativas del Sistema

### 5.1 Arquitectura y Modulith (`ARC`)

* **`ARC-01 · MUST` [A]** Las fronteras entre subdominios se verificarán automáticamente en el build de CI mediante Spring Modulith (`ApplicationModules.of(Application.class).verify()`). El paquete `application` de cada subdominio debe declararse explícitamente como API pública mediante `@NamedInterface("application")` en su respectivo archivo `package-info.java`.
* **`ARC-02 · NEVER` [A]** Ningún módulo accederá a las clases de los paquetes `domain` o `infrastructure` de otro módulo. La colaboración entre módulos se realiza exclusivamente mediante eventos de dominio asíncronos o contratos en la interfaz nombrada `application`. Si un enum de estado forma parte del contrato público de un módulo, debe exponerse a través de `application`.
* **`ARC-03 · MUST` [A]** El proyecto generará la documentación técnica y diagramas de arquitectura en compilación mediante el componente `Documenter` de Spring Modulith.
* **`ARC-04 · MUST` [R]** Toda desviación consciente de este manual debe registrarse como un `ADR` formal con contexto, alternativa descartada y condición de salida.
* **`ARC-05 · NEVER` [R]** Se permitirá la adopción de capacidades a medias (*regla de adopción diferida*). Queda prohibido añadir tablas, eventos o infraestructura sin el circuito completo que los consuma y pruebe.

---

### 5.2 Dominio (`DOM`)

* **`DOM-01 · NEVER` [A]** El dominio importará clases de frameworks o librerías de terceros (estrictamente prohibido `org.springframework.*`, `jakarta.persistence.*`, `com.fasterxml.jackson.*`, `lombok.*`).
* **`DOM-02 · MUST` [A]** Las entidades y agregados tendrán constructores protegidos o privados, instanciándose únicamente mediante métodos factoría estáticos explícitos (`create` para creación inicial con valores por defecto, `reconstruct` para hidratación desde persistencia).
* **`DOM-03 · MUST` [R]** Los conceptos del dominio con reglas de invariante o formato complejas deben encapsularse en **Value Objects** inmutables que autovaliden su integridad en el constructor.
* **`DOM-04 · NEVER` [A]** Los agregados expondrán métodos mutadores genéricos (`setters`). La mutación de estado se realiza exclusivamente mediante métodos de negocio semánticos explícitos.
* **`DOM-05 · MUST` [A]** Cada mutación exitosa en un agregado que afecte a otros contextos debe registrar un `DomainEvent` inmutable en su lista interna de eventos pendientes (`registerEvent`).
* **`DOM-06 · MUST` [A]** Las interfaces de repositorio de salida declaradas en `application/port/out` operarán exclusivamente con agregados y tipos de dominio en los métodos de escritura, nunca con entidades ORM.

---

### 5.3 Aplicación (`APP`)

* **`APP-01 · MUST` [A]** Cada caso de uso debe representarse como una interfaz en `application/port/in` con el sufijo `UseCase` y un único método público de ejecución (`execute`), implementada en una clase `@Service` en `application/service` con el sufijo `Service`.
* **`APP-02 · NEVER` [A]** La capa de aplicación contendrá lógica de cálculo de negocio o validación de invariantes de dominio; su función se limita estrictamente a la orquestación técnica del flujo.
* **`APP-03 · MUST` [R]** La entrada a un caso de uso debe ser un Comando o Query inmutable bajo el siguiente modelo híbrido:
  * **Comandos exclusivamente Web:** Si el comando se consume únicamente a través de una petición HTTP, debe definirse como un `record` plano sin lógica de validación interna, delegando el control previo en las anotaciones `@Valid` del `HttpRequest` en la capa Web.
  * **Comandos no exclusivos de Web (multicanal):** Si el comando puede ser invocado desde orígenes no exclusivos de HTTP (colas de mensajería como Kafka/RabbitMQ, eventos de dominio o tareas programadas/schedulers), debe implementar comprobaciones defensivas de no-nulidad de forma inmediata (*fail-fast* con *zero-allocation* mediante `Objects.requireNonNull` en su constructor compacto nativo).
  * **Queries (lectura):** Se definen siempre como `record` planos sin lógica de validación interna.
  La salida de un caso de uso debe ser un DTO plano de aplicación (`*Result`) o el lanzamiento de una excepción tipada de coste cero (`BaseException`).
* **`APP-04 · NEVER` [A]** Un caso de uso devolverá agregados de dominio o entidades de base de datos hacia los adaptadores primarios o hacia otros módulos.

---

### 5.4 Adaptadores de Entrada (`INP`)

* **`INP-01 · MUST` [A]** Los controladores REST delegan inmediatamente a la capa de aplicación tras mapear las peticiones HTTP a Comandos/Queries. Se autoriza la importación y uso directo de `domain.model.enums.*` locales en controladores y mappers web del propio módulo para evitar duplicación de tipos de estado inmutables.
* **`INP-02 · NEVER` [A]** Ningún controlador REST ni adaptador primario contendrá lógica de negocio ni abrirá o cerrará transacciones de base de datos.
* **`INP-03 · MUST` [A]** Todas las excepciones de la API deben traducirse obligatoriamente mediante `GlobalExceptionHandler` al contrato estandarizado ultraligero `ErrorResponse` (`status`, `code`, `detail`, `errors`), omitiendo detalles internos en producción para evitar fugas y optimizar ancho de banda.
* **`INP-04 · MUST` [R]** Los procesos en background (Workers, Schedulers) deben inicializar su propio `ExecutionContext` antes de invocar cualquier caso de uso.

---

### 5.5 Adaptadores de Salida y Persistencia Híbrida (`OUT`)

* **`OUT-01 · NEVER` [A]** Las entidades anotadas para el ORM (`@Entity`) saldrán del adaptador de persistencia hacia la aplicación o el dominio.
* **`OUT-02 · MUST` [A]** El mapeo entre las entidades de base de datos y los agregados de dominio se realizará de forma explícita mediante mappers dedicados (MapStruct o factorías `reconstruct`), prohibiéndose la reflexión implícita.
* **`OUT-03 · NEVER` [R]** Se permitirá la carga perezosa (*lazy loading*) fuera del adaptador de persistencia; los agregados se cargan completos y consistentes.
* **`OUT-04 · MUST` [R]** Las consultas de lectura y listados **no deben hidratar Agregados de dominio**; deben mapear desde base de datos directamente a DTOs de proyección optimizados (`*Result`).
* **`OUT-05 · MUST` [A]** La persistencia por defecto es relacional con JPA/Hibernate (`infrastructure/adapter/out/persistence/jpa`). Se autoriza el uso de adaptadores JDBC directos (`infrastructure/adapter/out/persistence/jdbc`) mediante `NamedParameterJdbcTemplate` cuando se requieran tipos nativos avanzados o extensiones relacionales que JPA no gestione de forma transparente y segura.

---

### 5.6 Transacciones, Consistencia y Outbox (`TRX`)

* **`TRX-01 · MUST` [A]** La demarcación transaccional residirá en la capa de aplicación: `@Transactional` en casos de uso de escritura y `@Transactional(readOnly = true)` en casos de uso de lectura.
* **`TRX-02 · MUST` [A]** Los agregados implementarán control de concurrencia optimista mediante un campo de versión opaco (`version`).
* **`TRX-03 · MUST` [A]** Todo evento de dominio generado por un agregado se persistirá en la misma transacción local de base de datos en la tabla `outbox_events` del esquema correspondiente (**Transactional Outbox Pattern**).
* **`TRX-04 · NEVER` [A]** Se realizarán publicaciones síncronas por red a brokers de mensajería dentro de la transacción de negocio; la publicación la realiza un worker desacoplado que lee del outbox.
* **`TRX-05 · MUST` [R]** Todo consumidor de eventos debe implementar control de **idempotencia**, verificando si el identificador del evento ya ha sido procesado.
* **`TRX-06 · MUST` [A]** Las tablas de outbox deben disponer de un proceso de purga periódica programada para eliminar registros en estado `DELIVERED` que superen el periodo de retención configurado.

---

### 5.7 Kernel Compartido y Preocupaciones Transversales (`SHR`)

* **`SHR-01 · NEVER` [A]** El paquete `shared` contendrá reglas de negocio ligadas a subdominios específicos.
* **`SHR-02 · MUST` [A]** Todos los identificadores únicos del sistema se generarán con formato **UUIDv7** secuencial en el tiempo mediante `UuidGeneratorPort`.
* **`SHR-03 · MUST` [A]** Toda petición debe propagar el contexto de seguridad e inquilino (`tenantId`, `userId`, `roles`) a través de un `ExecutionContext` inmutable.
* **`SHR-04 · MUST` [R]** Los errores de negocio deben asociarse a un código alfanumérico inmutable y tipado (ej. `RESOURCE_NOT_FOUND`), separando el código técnico del mensaje descriptivo.

---

## 6. Matriz de Automatización (Spring Modulith & ArchUnit)

La integridad de este manual se respalda mediante pruebas automáticas que bloquean el pipeline de CI:

| Regla | Mecanismo | Condición de Fallo Automatizada |
| --- | --- | --- |
| **`ARC-01`** | Spring Modulith | `ApplicationModules.of(Application.class).verify()` detecta dependencias cíclicas, módulos mal estructurados o falta de `@NamedInterface`. |
| **`ARC-02`** | Spring Modulith / ArchUnit | Clases de un módulo importan clases de paquetes `domain..` o `infrastructure..` de otro módulo. |
| **`DOM-01`** | ArchUnit | Clases en `..domain..` tienen dependencias hacia `org.springframework..`, `jakarta.persistence..` o `com.fasterxml.jackson..`. |
| **`DOM-04`** | ArchUnit | Clases en `..domain.model..` exponen métodos públicos que comiencen por `set`. |
| **`APP-01`** | ArchUnit | Clases en `..application.service..` no implementan exactamente una interfaz en `..application.port.in..` o no terminan con el sufijo `Service`. |
| **`INP-01`** | ArchUnit | Clases en `..adapter.in.web..` dependen de `..domain.model..` (excluyendo el paquete de enums puros `..domain.model.enums..`). |
| **`OUT-01`** | ArchUnit | Clases anotadas con `@Entity` residen fuera del paquete `..infrastructure.adapter.out.persistence.jpa..`. |
| **`OUT-05`** | ArchUnit | Clases que importen `NamedParameterJdbcTemplate` residen fuera de `..infrastructure.adapter.out.persistence.jdbc..`. |
| **`TRX-01`** | ArchUnit | Anotación `@Transactional` presente en clases fuera de `..application..`. |

---

## 7. Matriz de Anti-Patrones Comunes

| ❌ Anti-Patrón | 💥 Por qué falla | ✅ Solución Normativa | Regla |
| --- | --- | --- | --- |
| **Acoplamiento Directo entre Módulos** | Un módulo inyecta el repositorio o accede a entidades de otro módulo, destruyendo la modularidad. | Comunicar módulos exclusivamente mediante eventos asíncronos o contratos en `application` expuestos vía `@NamedInterface`. | `ARC-01`, `ARC-02` |
| **Hidratar Agregados para Consultas / Listados** | Degrada el rendimiento con saturación de memoria, sobrecarga del GC e hidratación de objetos innecesarios. | Consultas de lectura directas a proyecciones SQL/DTOs optimizadas (CQRS ligero). | `OUT-04` |
| **Doble Escritura (Dual-Write) sin Outbox** | Si la BD guarda pero la red falla al avisar al broker, el sistema entra en inconsistencia irrecuperable. | Transactional Outbox Pattern: guardar en BD local y publicar vía worker en segundo plano. | `TRX-03`, `TRX-04` |
| **Agregado Anémico con Setters** | El agregado pierde el control de sus invariantes y cualquier servicio puede corromper su estado. | Métodos semánticos de negocio y validación estricta en constructores o Value Objects. | `DOM-03`, `DOM-04` |
| **Entidad JPA usada como Agregado** | El ORM invade el dominio, obligando a constructores por defecto y acoplando el negocio a la tabla SQL. | Separar Agregado de Dominio de la entidad JPA mediante un Mapper explícito bidireccional. | `DOM-01`, `OUT-01` |
| **Lógica de negocio en el Caso de Uso** | El caso de uso se transforma en un procedimiento monolítico y el dominio queda desprovisto de lógica. | Mover las reglas de cálculo e invariantes al interior del Agregado de dominio. | `APP-02` |
| **Duplicación redundante de Enums en Web** | Crear enums idénticos en la web solo para aislar el dominio multiplica el boilerplate sin aportar valor. | Permitir el uso directo de enums de estado inmutables del dominio en controladores REST locales. | `INP-01` |
| **Doble validación en Comandos exclusivos Web** | Revalidar en el Command datos que ya garantizó Bean Validation (@Valid) en el HttpRequest genera duplicación innecesaria de código. | Definir los comandos exclusivos de HTTP como records planos sin validación interna. | `APP-03`, `ADR-06` |
| **Comando Multicanal sin validación fail-fast** | Permitir que peticiones defectuosas desde colas (Kafka) o schedulers lleguen al dominio o abran transacciones sin haber sido validadas. | Implementar validación fail-fast nativa (Objects.requireNonNull) en el constructor compacto de comandos multicanal. | `APP-03`, `ADR-06` |

---

## 8. Checklist para Revisión de Pull Requests

* [ ] ¿Las fronteras del módulo se respetan sin invadir paquetes internos de otros módulos? (`ARC-02`)
* [ ] ¿El paquete `application` expone su contrato mediante `package-info.java` con `@NamedInterface("application")`? (`ARC-01`)
* [ ] ¿Los Comandos exclusivos de HTTP son records planos sin validación interna, y los Comandos multicanal (colas, schedulers) tienen validación fail-fast (`Objects.requireNonNull`)? (`APP-03`, `ADR-06`)
* [ ] ¿El Agregado protege sus invariantes sin exponer métodos mutadores (`setters`) públicos? (`DOM-04`)
* [ ] ¿El caso de uso orquesta el flujo sin absorber reglas de cálculo que corresponden al Agregado? (`APP-02`)
* [ ] ¿Cada implementación en `application/service` implementa exactamente un `*UseCase` y termina en `Service`? (`APP-01`)
* [ ] ¿Las entidades ORM (`@Entity`) están estrictamente confinadas al adaptador de persistencia JPA? (`OUT-01`)
* [ ] ¿Las búsquedas y listados van directos a proyecciones DTO sin hidratar Agregados de dominio? (`OUT-04`)
* [ ] ¿Se utiliza JDBC directo con `NamedParameterJdbcTemplate` cuando se requieren optimizaciones o tipos SQL nativos avanzados? (`OUT-05`)
* [ ] ¿Toda mutación de negocio encola su correspondiente `DomainEvent` para el outbox transaccional? (`DOM-05`, `TRX-03`)
* [ ] ¿Los errores se traducen a través de `GlobalExceptionHandler` al modelo ultraligero `ErrorResponse`? (`INP-03`)

---

## 9. Registro de Decisiones de Arquitectura (ADR Base)

### `ADR-01`: Desacoplamiento entre Dominio y Persistencia Híbrida (JPA + JDBC)

* **Decisión:** Mantener modelos separados para Dominio (`AggregateRoot`) y Persistencia, implementando persistencia híbrida bajo `infrastructure/adapter/out/persistence`: JPA para agregados estándar y JDBC directo (`NamedParameterJdbcTemplate`) para operaciones de consulta avanzada, tipos relacionales complejos o alto rendimiento.
* **Alternativa descartada:** Mapear modelos complejos en JPA forzando anotaciones del ORM en el dominio o validar restricciones relacionales en memoria de la JVM.
* **Motivo técnico:** Proteger la pureza del dominio y aprovechar la potencia nativa de la base de datos relacional con rendimiento óptimo y consistencia concurrente.

### `ADR-02`: Publicación Asíncrona mediante Transactional Outbox Propio

* **Decisión:** Persistir los eventos de dominio en una tabla relacional `outbox_events` por esquema en la misma transacción local de base de datos; un proceso worker en background despacha los eventos al broker o módulos destino.
* **Alternativa descartada:** Publicación síncrona por red durante la transacción HTTP o doble escritura directa.
* **Motivo técnico:** Garantizar consistencia eventual atómica (*guaranteed delivery*) sin riesgo de desincronización por caídas de red o fallos de infraestructura.

### `ADR-03`: Estandarización de Errores mediante ErrorResponse Ultraligero y Excepciones sin Traza en Java 25

* **Decisión:** Devolver todos los errores de la API REST bajo el modelo lean `ErrorResponse` (`status`, `code`, `detail`, `errors`), suprimiendo detalles en producción y resolviendo i18n en el cliente a partir del código de error. En el backend, las excepciones de negocio heredan de `BaseException` con `writableStackTrace = false`.
* **Alternativa descartada:** Uso de la mónada `Result<T, E>` en casos de uso o cuerpos verbosos de error bajo RFC 7807/9457 con trazas completas.
* **Motivo técnico:** Minimizar el consumo de CPU y memoria en Java 25 (excepciones de coste cero), mantener la compatibilidad nativa con `@Transactional` de Spring, reducir drásticamente el tamaño del payload en redes móviles y evitar fugas de información.

### `ADR-04`: Gobernanza Modular con Spring Modulith

* **Decisión:** Organizar los módulos de negocio directamente bajo la raíz `<namespace.base>.<subdominio>` gobernados por Spring Modulith, declarando la API pública de cada módulo mediante `@NamedInterface("application")` y verificando sus fronteras en tiempo de compilación/test con `ApplicationModules.verify()`.
* **Alternativa descartada:** Crear capas de paquetes artificiales intermedias o desplegar múltiples microservicios independientes prematuros.
* **Motivo técnico:** Simplicidad operativa de despliegue en un único artefacto manteniendo separación estricta de Bounded Contexts verificada por tests.

### `ADR-05`: Consumo Directo de Enums de Estado en Adaptadores Primarios

* **Decisión:** Permitir que los adaptadores de entrada (controladores REST y mappers web) importen y consuman directamente los enums inmutables de estado definidos en `domain.model.enums.*` del propio módulo. Si el enum trasciende las fronteras del módulo, debe exponerse en el paquete `application` de la `@NamedInterface`.
* **Alternativa descartada:** Crear enums duplicados idénticos en la capa web con mappers intermedios en cada endpoint.
* **Motivo técnico:** Evitar la proliferación de código redundante (*boilerplate*) sin valor funcional, garantizando al mismo tiempo que Modulith verifique limpiamente las referencias intermodulares.

### `ADR-06`: Validación Híbrida en Comandos (Exclusivos Web vs. Multicanal)

* **Decisión:** 
  1. **Comandos exclusivamente Web:** Si un comando se utiliza únicamente desde peticiones HTTP, se define como un `record` plano sin lógica de validación interna, ya que los datos son validados previamente en el controlador web mediante Bean Validation (`@Valid`).
  2. **Comandos no exclusivos de Web (Multicanal):** Si un comando no tiene acceso exclusivo por HTTP y puede ser invocado desde otros canales (colas de mensajería como Kafka/RabbitMQ, eventos de dominio o tareas programadas/schedulers), debe implementar validación defensiva inmediata (*fail-fast*) de no-nulidad en su constructor compacto nativo mediante `Objects.requireNonNull`.
  3. **Consultas (`Query`):** Se definen siempre como `record` planos sin lógica de validación interna.
* **Alternativa descartada:** Aplicar validación defensiva duplicada en comandos que únicamente se consumen desde la web, o permitir comandos sin validación en canales asíncronos que no pasan por validación HTTP.
* **Motivo técnico:**
  1. **Eliminación de validación redundante:** En endpoints exclusivamente Web, la petición ya ha superado el filtro `@Valid` en el controlador; revalidar en el Command genera duplicación innecesaria de código.
  2. **Protección fail-fast en canales asíncronos:** Los mensajes provenientes de colas (Kafka/RabbitMQ), eventos o schedulers no cuentan con la validación de la capa Web; la comprobación inmediata en el constructor compacto (`Objects.requireNonNull`) asegura que peticiones defectuosas se detengan antes de alcanzar el dominio o abrir transacciones de base de datos.
  3. **Eficiencia (Zero-Allocation) y simplicidad:** Utiliza métodos nativos de la JVM (`Objects.requireNonNull`) sin crear objetos auxiliares en memoria y mantiene los records exclusivamente web como estructuras mínimas y legibles.