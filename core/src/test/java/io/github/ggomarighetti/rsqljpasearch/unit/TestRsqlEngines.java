package io.github.ggomarighetti.rsqljpasearch.unit;

import io.github.ggomarighetti.rsqljpasearch.exception.SearchDefinitionValidationException;
import io.github.ggomarighetti.rsqljpasearch.rsql.RsqlCompilationRequest;
import io.github.ggomarighetti.rsqljpasearch.rsql.backend.RsqlBackendAdapter;
import io.github.ggomarighetti.rsqljpasearch.rsql.backend.RsqlBackendValidationContext;
import io.github.ggomarighetti.rsqljpasearch.rsql.engine.SearchRsqlEngine;
import io.github.ggomarighetti.rsqljpasearch.rsql.engine.SearchRsqlEngineBuilder;
import io.github.ggomarighetti.rsqljpasearch.rsql.engine.SearchRsqlEngines;
import io.github.ggomarighetti.rsqljpasearch.rsql.metadata.DefaultRsqlOperatorDescriptors;
import io.github.ggomarighetti.rsqljpasearch.rsql.operator.RsqlOperator;
import io.github.ggomarighetti.rsqljpasearch.rsql.metadata.RsqlOperatorDescriptor;
import org.springframework.data.jpa.domain.Specification;

/** Core-test composition root that does not depend on a production backend module. */
public final class TestRsqlEngines {
    private TestRsqlEngines() {
    }

    public static SearchRsqlEngineBuilder builder() {
        return SearchRsqlEngines.builder(new TestBackend());
    }

    public static SearchRsqlEngine defaults() {
        return builder().build();
    }

    private static final class TestBackend implements RsqlBackendAdapter {
        @Override
        public <T> Specification<T> compile(RsqlCompilationRequest<T> request) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        }

        @Override
        public void validate(RsqlBackendValidationContext context) {
            for (RsqlOperator operator : context.definition().filteringOperators()) {
                RsqlOperatorDescriptor descriptor = context.operators().require(operator);
                boolean custom = context.jpaOperators().predicate(operator).isPresent();
                if (!DefaultRsqlOperatorDescriptors.isDefault(operator) && !custom) {
                    throw new SearchDefinitionValidationException(
                            SearchDefinitionValidationException.RSQL_OPERATOR_NOT_EXECUTABLE,
                            "operator '%s' is registered but has no test JPA predicate".formatted(operator));
                }
                if (custom && descriptor.argumentType().isEmpty()) {
                    throw new SearchDefinitionValidationException(
                            SearchDefinitionValidationException.RSQL_OPERATOR_TYPE_MISMATCH,
                            "custom operator '%s' must declare an argument type".formatted(operator));
                }
                if (custom && !Comparable.class.isAssignableFrom(descriptor.argumentType().orElseThrow())) {
                    throw new SearchDefinitionValidationException(
                            SearchDefinitionValidationException.RSQL_OPERATOR_TYPE_MISMATCH,
                            "custom operator '%s' must declare a Comparable argument type".formatted(operator));
                }
            }
        }
    }
}
