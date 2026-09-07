package com.template.api.shared.application.port;

import java.util.UUID;

/** Source of time-ordered UUIDv7 identifiers, used for aggregate and event identity. */
public interface UuidGeneratorPort {

    UUID generateId();
}
