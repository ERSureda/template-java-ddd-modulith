package com.template.api.shared.application.port.in;

@FunctionalInterface
public interface UseCase<I, O> {

    O execute(I input);
}