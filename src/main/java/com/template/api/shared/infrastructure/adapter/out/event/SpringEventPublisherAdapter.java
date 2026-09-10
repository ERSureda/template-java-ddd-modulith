package com.template.api.shared.infrastructure.adapter.out.event;

import com.template.api.shared.application.port.out.EventPublisherPort;
import com.template.api.shared.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SpringEventPublisherAdapter implements EventPublisherPort {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publish(DomainEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publishAll(List<DomainEvent> events) {
        events.forEach(eventPublisher::publishEvent);
    }
}

/**
 * 1. Migración de Base de Datos (Tabla de Eventos)
 * Spring Modulith no inventa una tabla en el aire; necesita persistir los eventos en su tabla técnica event_publication. Si utilizas Flyway o Liquibase sobre PostgreSQL, se debe incluir el script DDL oficial:
 *
 * SQL
 * CREATE TABLE IF NOT EXISTS event_publication (
 *     id UUID NOT NULL,
 *     listener_id VARCHAR(512) NOT NULL,
 *     event_type VARCHAR(512) NOT NULL,
 *     serialized_event TEXT NOT NULL,
 *     publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
 *     completion_date TIMESTAMP WITH TIME ZONE,
 *     PRIMARY KEY (id, listener_id)
 * );
 *
 * CREATE INDEX IF NOT EXISTS idx_event_publication_completion_date
 * ON event_publication (completion_date);
 *
 * 2. Consumo Asíncrono y Transaccional (@ApplicationModuleListener)
 * Para que otro módulo reaccione al evento sin acoplarse ni romper la transacción del módulo emisor, se utiliza @ApplicationModuleListener:
 *
 * Java
 * package com.template.api.notification.infrastructure.adapter.in.event;
 *
 * import com.template.api.order.domain.event.OrderCreatedEvent;
 * import org.springframework.modulith.events.ApplicationModuleListener;
 * import org.springframework.stereotype.Component;
 *
 * @Component
 * public class OrderNotificationListener {
 *
 *     @ApplicationModuleListener
 *     public void on(OrderCreatedEvent event) {
 *         // Ejecución en transacción separada y asíncrona.
 *         // Si este método falla, Modulith mantiene el evento pendiente en BD.
 *     }
 * }
 *
 * 3. Reintento de Fallos (IncompleteEventPublications)
 * Si el consumidor falla (por indisponibilidad de un servicio, caída temporal, etc.), Modulith deja el registro en la tabla con completion_date = NULL. Se requiere un componente programado mínimo en shared para reintentar los eventos no completados:
 *
 * Java
 * package com.template.api.shared.infrastructure.adapter.out.event;
 *
 * import lombok.RequiredArgsConstructor;
 * import org.springframework.modulith.events.IncompleteEventPublications;
 * import org.springframework.scheduling.annotation.Scheduled;
 * import org.springframework.stereotype.Component;
 *
 * import java.time.Duration;
 *
 * @Component
 * @RequiredArgsConstructor
 * public class EventPublicationRepublisher {
 *
 *     private final IncompleteEventPublications incompleteEvents;
 *
 *     @Scheduled(fixedDelayString = "${application.events.resubmit-interval:10000}")
 *     public void resubmitFailedEvents() {
 *         // Reintenta eventos no completados que tengan más de 1 minuto de antigüedad
 *         incompleteEvents.resubmitIncompletePublicationsOlderThan(Duration.ofMinutes(1));
 *     }
 * }
 *
 * 4. Preparación para Microservicios (@Externalized)
 * Cuando extraigas un módulo a un microservicio independiente y requieras publicar en un broker (como Apache Kafka o RabbitMQ), únicamente anotas el DomainEvent sin alterar el dominio ni los casos de uso:
 *
 * Java
 * @Externalized("orders.v1.events::#{#this.orderId()}")
 * public record OrderCreatedEvent(UUID eventId, Instant occurredOn, UUID orderId) implements DomainEvent {
 * }
 */