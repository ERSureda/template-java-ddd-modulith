# Template Java DDD Modulith

Plantilla base y semilla para el desarrollo de servicios backend en Java utilizando **Domain-Driven Design (DDD)**, **Arquitectura Hexagonal (Ports & Adapters)** y **Monolitos Modulares** gobernados por **Spring Modulith**.

---

## 🚀 Tecnologías Principales

* **Lenguaje:** Java 25 (Virtual Threads activados por defecto)
* **Framework:** Spring Boot & Spring Modulith
* **Persistencia:** Persistencia Híbrida (Spring Data JPA + PostgreSQL 17 / JDBC nativo)
* **Migraciones de BD:** Flyway
* **Caché / Mensajería In-Memory:** AWS Valkey / Redis
* **Cloud SDK:** AWS SDK v2 (DynamoDB, S3, KMS)
* **Mapeo:** MapStruct
* **Validación Arquitectural & Tests:** Spring Modulith Verification, ArchUnit, Testcontainers, JUnit 5

---

## 📁 Estructura del Repositorio

```text
.
├── docs/
│   ├── ARCHITECTURE.md          # Manual de Arquitectura y Reglas Normativas (MUST / NEVER)
│   └── SEED.md                  # Especificación de la semilla y criterios de aceptación
├── src/
│   ├── main/
│   │   ├── java/com/template/api/
│   │   │   ├── ApiApplication.java
│   │   │   └── shared/          # Kernel técnico transversal (@ApplicationModule OPEN)
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── application-dev.yaml
│   │       └── application-prod.yaml
│   └── test/
│       ├── java/                # Tests de contexto, arquitectura y suite de verificación
│       └── resources/
├── build.gradle
├── gradle.properties
└── settings.gradle
```

---

## 📖 Documentación Normativa

Toda la arquitectura, convenciones de paquetes, reglas de diseño y decisiones arquitectónicas están formalmente documentadas en la carpeta `docs/`:

1. **[Manual de Arquitectura (`docs/ARCHITECTURE.md`)](docs/ARCHITECTURE.md)**:
   * Principios DDD, reglas direccionales de capas y catálogo de ámbitos (`ARC`, `DOM`, `APP`, `INP`, `OUT`, `TRX`, `SHR`).
   * Flujos de escritura (Command con Transactional Outbox) y flujos de lectura (Query con proyecciones DTO).
   * Matriz de automatización con Spring Modulith y ArchUnit.
   * Registro de Decisiones de Arquitectura (ADRs).

2. **[Especificación de la Semilla (`docs/SEED.md`)](docs/SEED.md)**:
   * Inventario técnico del módulo `shared/`.
   * Circuito completo del Transactional Outbox (Relay con `SKIP LOCKED`, reintentos, DLQ y purga).
   * Criterios de aceptación ejecutables (*Definition of Done*).

---

## 🛠️ Comandos de Construcción y Verificación

Compilar el proyecto:
```bash
./gradlew build
```

Ejecutar tests unitarios y verificación de arquitectura (Modulith + ArchUnit):
```bash
./gradlew check
```

Arrancar en entorno local:
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```
