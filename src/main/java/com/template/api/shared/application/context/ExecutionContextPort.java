package com.template.api.shared.application.context;

import java.util.Optional;

public interface ExecutionContextPort {
    Optional<ExecutionContext> findCurrent();
}
