package com.template.api.shared.infrastructure.adapter;

import com.taxai.api.shared.application.context.ExecutionContext;
import com.taxai.api.shared.application.context.ExecutionContextPort;
import com.taxai.api.shared.infrastructure.context.ExecutionContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ExecutionContextAdapter implements ExecutionContextPort {

    @Override
    public Optional<ExecutionContext> findCurrent() {
        return ExecutionContextHolder.get();
    }
}
