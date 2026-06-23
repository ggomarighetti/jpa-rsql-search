package io.github.ggomarighetti.rsqljpasearch.autoconfigure;

import io.github.ggomarighetti.rsqljpasearch.rsql.backend.perplexhub.PerplexhubRsqlEngines;
import io.github.ggomarighetti.rsqljpasearch.compile.SearchCompiler;
import io.github.ggomarighetti.rsqljpasearch.definition.SearchDefinition;
import io.github.ggomarighetti.rsqljpasearch.protection.SearchProtectionException;
import io.github.ggomarighetti.rsqljpasearch.rsql.engine.SearchRsqlEngine;
import io.github.ggomarighetti.rsqljpasearch.unit.TestTypes;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static io.github.ggomarighetti.rsqljpasearch.rsql.operator.RsqlOperators.IN;

class RsqlJpaSearchAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RsqlJpaSearchEngineAutoConfiguration.class,
                    RsqlJpaSearchAutoConfiguration.class));

    @Test
    void createsSearchCompilerBean() {
        contextRunner.run(context -> assertNotNull(context.getBean(SearchCompiler.class)));
    }

    @Test
    void backsOffCompilerWhenDefaultRsqlEngineIsDisabled() {
        contextRunner
                .withPropertyValues("rsql.jpa.search.rsql.enabled=false")
                .run(context -> {
                    assertTrue(context.containsBean("searchDefinitionFactory"));
                    assertFalse(context.containsBean("searchRsqlEngine"));
                    assertFalse(context.containsBean("searchCompiler"));
                });
    }

    @Test
    void ignoresTheRemovedJpaFirstPropertyPrefix() {
        contextRunner
                .withPropertyValues("jpa.rsql.search.rsql.enabled=false")
                .run(context -> {
                    assertTrue(context.containsBean("searchRsqlEngine"));
                    assertTrue(context.containsBean("searchCompiler"));
                });
    }

    @Test
    void createsCompilerWithUserProvidedEngineWhenDefaultRsqlEngineIsDisabled() {
        SearchRsqlEngine engine = PerplexhubRsqlEngines.defaults();

        contextRunner
                .withPropertyValues("rsql.jpa.search.rsql.enabled=false")
                .withBean(SearchRsqlEngine.class, () -> engine)
                .run(context -> {
                    assertTrue(context.containsBean("searchCompiler"));
                    assertNotNull(context.getBean(SearchCompiler.class));
                });
    }

    @Test
    void doesNotExposePostgresQueryExecutionConfiguration() throws IOException {
        assertFalse(Arrays.stream(RsqlJpaSearchProperties.class.getMethods())
                .anyMatch(method -> method.getName().equals("getPostgres")));

        String resource = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";
        try (var imports = RsqlJpaSearchAutoConfigurationTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(imports);
            String autoConfigurations = new String(imports.readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(autoConfigurations.contains("PostgresSearchAutoConfiguration"));
        }
    }

    @Test
    void createsSearchDefinitionFactoryWithGlobalPathLimits() {
        contextRunner
                .withPropertyValues("rsql.jpa.search.paths.max-depth=4")
                .run(context -> {
                    SearchDefinition.Factory factory = context.getBean(SearchDefinition.Factory.class);

                    SearchDefinition<TestTypes.Product> definition = factory.builder()
                            .entity(TestTypes.Product.class)
                            .fields(fields -> fields.add("countryCode", String.class)
                                    .path("customer.region.country.code")
                                    .sortable())
                            .build();

                    assertTrue(definition.sortingPaths().containsKey("countryCode"));
                });
    }

    @Test
    void bindsGlobalProtectionProperties() {
        contextRunner
                .withPropertyValues("rsql.jpa.search.filter.max-in-values=1")
                .run(context -> {
                    SearchDefinition.Factory factory = context.getBean(SearchDefinition.Factory.class);
                    SearchCompiler compiler = context.getBean(SearchCompiler.class);
                    SearchDefinition<TestTypes.Product> definition = factory.builder()
                            .entity(TestTypes.Product.class)
                            .fields(fields -> fields.add("taxId", String.class)
                                    .path("owner.taxIdentifier")
                                    .filterable(filter -> filter.allow(IN)))
                            .paging()
                            .build();

                    var pageRequest = org.springframework.data.domain.PageRequest.of(0, 25);
                    assertThrows(
                            SearchProtectionException.class,
                            () -> compiler.compile(
                                    "taxId=in=(1,2)",
                                    null,
                                    pageRequest,
                                    definition));
                });
    }

    @Test
    void doesNotRequireSearchDefinitionBeans() {
        contextRunner
                .run(context -> {
                    SearchDefinition.Factory factory = context.getBean(SearchDefinition.Factory.class);
                    SearchDefinition<TestTypes.Product> definition = factory.builder()
                            .entity(TestTypes.Product.class)
                            .fields(fields -> fields.add("name", String.class))
                            .build();

                    assertFalse(context.getBeansOfType(SearchDefinition.class).containsValue(definition));
                    assertTrue(definition.fields().containsKey("name"));
                });
    }
}
