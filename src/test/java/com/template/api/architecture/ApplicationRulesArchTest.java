package com.template.api.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

@AnalyzeClasses(packages = "com.template.api", importOptions = ImportOption.DoNotIncludeTests.class)
class ApplicationRulesArchTest {

    private static final DescribedPredicate<JavaClass> IMPLEMENT_EXACTLY_ONE_USE_CASE =
            describe("implementar exactamente una interfaz UseCase",
                    javaClass -> javaClass.getRawInterfaces().size() == 1 &&
                            javaClass.getRawInterfaces().iterator().next().getSimpleName().endsWith("UseCase"));

    @ArchTest
    static final ArchRule application_services_should_implement_one_use_case =
            classes().that().resideInAPackage("..application.service..")
                    .should().haveSimpleNameEndingWith("Service")
                    .andShould(ArchCondition.from(IMPLEMENT_EXACTLY_ONE_USE_CASE))
                    .as("APP-01: Cada servicio de aplicación debe implementar exactamente una interfaz UseCase y terminar en Service");

    @ArchTest
    static final ArchRule transactional_should_only_be_in_application =
            methods().that().areAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                    .should().beDeclaredInClassesThat().resideInAPackage("..application..")
                    .as("TRX-01: La demarcación transaccional debe residir exclusivamente en la capa de aplicación");
}