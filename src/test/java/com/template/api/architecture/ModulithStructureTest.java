package com.template.api.architecture;

import com.template.api.ApiApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Spring Modulith Architectural Verification")
class ModulithStructureTest {

    @Test
    @DisplayName("Debe verificar que la estructura modular y fronteras de Modulith son válidas")
    void verifyModulithStructure() {
        ApplicationModules modules = ApplicationModules.of(ApiApplication.class);
        modules.verify();

        ApplicationModule sharedModule = modules.getModuleByName("shared")
                .orElseThrow(() -> new AssertionError("El módulo 'shared' debe ser detectado por Spring Modulith"));

        assertThat(sharedModule.getDisplayName()).isEqualTo("Shared Kernel");
    }
}
