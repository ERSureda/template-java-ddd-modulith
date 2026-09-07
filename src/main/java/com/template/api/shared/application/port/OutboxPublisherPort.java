package com.template.api.shared.application.port;

import com.taxai.api.shared.domain.event.DomainEvent;

public interface OutboxPublisherPort {
    void publish(DomainEvent event);
}
