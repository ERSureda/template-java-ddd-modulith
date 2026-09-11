package com.template.api.shared.application.port.in;

@FunctionalInterface
public interface Query<I, O> {

    O execute(I query);
}