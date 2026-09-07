package com.template.api.shared.application.port;

import com.taxai.api.shared.domain.event.DomainEvent;

public interface EventDispatcherPort {
    void dispatch(DomainEvent event);
}
