package com.template.api.shared.application.port;

import com.taxai.api.shared.domain.event.DomainEvent;

public interface EventSerializerPort {
    String serialize(DomainEvent event);
    DomainEvent deserialize(String payload, String eventType, int schemaVersion);
}
