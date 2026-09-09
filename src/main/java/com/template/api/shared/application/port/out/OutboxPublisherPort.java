package com.template.api.shared.application.port.out;

import com.template.api.shared.domain.event.DomainEvent;

public interface OutboxPublisherPort {
    void publish(DomainEvent event);
}
