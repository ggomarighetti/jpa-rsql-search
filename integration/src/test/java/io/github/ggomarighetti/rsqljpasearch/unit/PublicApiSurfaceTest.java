package io.github.ggomarighetti.rsqljpasearch.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.ggomarighetti.rsqljpasearch.policy.SearchPolicy;
import io.github.ggomarighetti.rsqljpasearch.rsql.RsqlCompilationRequest;
import io.github.ggomarighetti.rsqljpasearch.compile.SearchCompiler;
import io.github.ggomarighetti.rsqljpasearch.rsql.engine.SearchRsqlEngine;
import io.github.ggomarighetti.rsqljpasearch.rsql.engine.SearchRsqlEngineBuilder;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class PublicApiSurfaceTest {
    @Test
    void compilationPipelineTypesArePackagePrivate() throws Exception {
        for (String type : List.of(
                "RsqlRulesValidator",
                "RsqlSearchGuard",
                "SearchProtectionContext")) {
            Class<?> implementation =
                    Class.forName("io.github.ggomarighetti.rsqljpasearch.compile." + type);
            assertFalse(Modifier.isPublic(implementation.getModifiers()), type);
        }
        for (Class<?> implementation : SearchCompiler.class.getDeclaredClasses()) {
            assertFalse(Modifier.isPublic(implementation.getModifiers()), implementation.getSimpleName());
        }
        for (String type : List.of(
                "RsqlJpaSearchAutoConfiguration",
                "RsqlJpaSearchProperties",
                "RsqlJpaSearchEngineAutoConfiguration")) {
            Class<?> implementation =
                    Class.forName("io.github.ggomarighetti.rsqljpasearch.autoconfigure." + type);
            assertFalse(Modifier.isPublic(implementation.getModifiers()), type);
        }
    }

    @Test
    void internalApiMarkerNoLongerExists() {
        assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("io.github.ggomarighetti.rsqljpasearch.InternalApi"));
    }

    @Test
    void jpaFirstNamespaceIsRemoved() {
        for (String type : List.of(
                "io.github.ggomarighetti.jparsqlsearch.compile.SearchCompiler",
                "io.github.ggomarighetti.jparsqlsearch.autoconfigure.JpaRsqlSearchAutoConfiguration",
                "io.github.ggomarighetti.jparsqlsearch.autoconfigure.JpaRsqlSearchProperties")) {
            assertThrows(ClassNotFoundException.class, () -> Class.forName(type), type);
        }
    }

    @Test
    void lowLevelRsqlContractsRemainAnExplicitExtensionSpi() throws Exception {
        assertTrue(Modifier.isPublic(
                SearchRsqlEngine.class.getMethod("parse", String.class).getModifiers()));
        assertTrue(Modifier.isPublic(
                SearchRsqlEngine.class.getMethod("compile", RsqlCompilationRequest.class).getModifiers()));
        assertTrue(Modifier.isPublic(
                SearchRsqlEngineBuilder.class.getMethod("withoutDefaultOperators").getModifiers()));
        assertTrue(Modifier.isPublic(
                SearchPolicy.Builder.class.getMethod("buildOverlay").getModifiers()));
        assertFalse(Arrays.stream(SearchRsqlEngine.class.getDeclaredMethods())
                .anyMatch(method -> Modifier.isStatic(method.getModifiers())
                        && ("builder".equals(method.getName()) || "defaults".equals(method.getName()))));
    }

    @Test
    void legacyV1PackagesAreRemoved() {
        for (String type : List.of(
                "io.github.ggomarighetti.rsqljpasearch.definition.SearchPath",
                "io.github.ggomarighetti.rsqljpasearch.validation.SearchDefinitionValidator",
                "io.github.ggomarighetti.rsqljpasearch.exception.SearchProtectionException",
                "io.github.ggomarighetti.rsqljpasearch.exception.RsqlFilterValidationException",
                "io.github.ggomarighetti.rsqljpasearch.exception.RsqlValidationError",
                "io.github.ggomarighetti.rsqljpasearch.exception.SearchPageableValidationException",
                "io.github.ggomarighetti.rsqljpasearch.exception.SearchQueryValidationException",
                "io.github.ggomarighetti.rsqljpasearch.rsql.SearchRsqlEngine",
                "io.github.ggomarighetti.rsqljpasearch.rsql.operator.DefaultRsqlOperatorDescriptors",
                "io.github.ggomarighetti.rsqljpasearch.rsql.operator.RsqlOperatorArity",
                "io.github.ggomarighetti.rsqljpasearch.rsql.operator.RsqlOperatorDescriptor",
                "io.github.ggomarighetti.rsqljpasearch.rsql.operator.RsqlOperatorRegistry",
                "io.github.ggomarighetti.rsqljpasearch.rsql.backend.RsqlJpaPredicateFactory",
                "io.github.ggomarighetti.rsqljpasearch.rsql.backend.RsqlJpaPredicateContext",
                "io.github.ggomarighetti.rsqljpasearch.definition.SearchDefinitionFactory",
                "io.github.ggomarighetti.rsqljpasearch.filter.DefaultFilterOperators",
                "io.github.ggomarighetti.rsqljpasearch.filter.FilterValidationError",
                "io.github.ggomarighetti.rsqljpasearch.query.SearchQuerySpecification",
                "io.github.ggomarighetti.rsqljpasearch.validation.HibernateRuleValidator",
                "io.github.ggomarighetti.rsqljpasearch.rsql.parser.DefaultRsqlParserFactory",
                "io.github.ggomarighetti.rsqljpasearch.rsql.engine.SearchRsqlEngineCustomizer",
                "io.github.ggomarighetti.rsqljpasearch.compile.SearchCompilationMode",
                "io.github.ggomarighetti.rsqljpasearch.compile.SearchPageableGuard",
                "io.github.ggomarighetti.rsqljpasearch.compile.SearchQueryGuard",
                "io.github.ggomarighetti.rsqljpasearch.compile.SearchSpecificationSorting")) {
            assertThrows(ClassNotFoundException.class, () -> Class.forName(type), type);
        }
    }
}
