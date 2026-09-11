package com.template.api.shared.application.port.in;

@FunctionalInterface
public interface VoidUseCase<I> {

    void execute(I input);
}