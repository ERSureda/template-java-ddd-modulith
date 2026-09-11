package com.template.api.shared.application.port.in;

@FunctionalInterface
public interface SupplierUseCase<O> {

    O execute();
}