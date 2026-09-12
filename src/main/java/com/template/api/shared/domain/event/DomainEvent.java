package com.template.api.shared.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato base para Eventos de Dominio en la arquitectura DDD.
 * <p>
 * En Java 25, las implementaciones de este contrato deben modelarse obligatoriamente
 * como {@code record} inmutables, cuyos componentes coinciden de forma natural con los accesores definidos.
 * Todo evento registrado representa un hecho inmutable que ya ha ocurrido en el modelo de dominio (DOM-05).
 */
public interface DomainEvent {

    /** Identificador único del evento (generado idealmente mediante UUIDv7 o aleatorio). */
    UUID eventId();

    /** Identificador de la entidad o agregado raíz que originó el evento. */
    String aggregateId();

    /** Instante temporal exacto en UTC en el que se produjo el evento. */
    Instant occurredAt();

    /** Nombre semántico del tipo de evento (ej: 'order.created.v1'). */
    String eventType();
}
