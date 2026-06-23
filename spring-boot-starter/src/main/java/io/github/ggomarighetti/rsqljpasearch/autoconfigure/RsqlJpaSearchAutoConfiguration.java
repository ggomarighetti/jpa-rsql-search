package io.github.ggomarighetti.rsqljpasearch.autoconfigure;

import io.github.ggomarighetti.rsqljpasearch.definition.SearchDefinition;
import io.github.ggomarighetti.rsqljpasearch.compile.SearchCompiler;
import io.github.ggomarighetti.rsqljpasearch.jpa.JpaSearchDefinitionValidator;
import io.github.ggomarighetti.rsqljpasearch.rsql.engine.SearchRsqlEngine;
import io.github.ggomarighetti.rsqljpasearch.definition.validation.SearchDefinitionValidator;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** Core Spring Boot auto-configuration for search definitions and compilation. */
@AutoConfiguration(afterName = {
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
}, after = RsqlJpaSearchEngineAutoConfiguration.class)
@EnableConfigurationProperties(RsqlJpaSearchProperties.class)
class RsqlJpaSearchAutoConfiguration {
    /** Creates the auto-configuration. */
    RsqlJpaSearchAutoConfiguration() {
    }

    /**
     * Creates the policy-aware search definition factory.
     *
     * @param properties configured search limits
     * @return search definition factory
     */
    @Bean
    @ConditionalOnMissingBean
    public SearchDefinition.Factory searchDefinitionFactory(RsqlJpaSearchProperties properties) {
        return new SearchDefinition.Factory(properties.toPolicy());
    }

    /**
     * Creates the JPA metamodel validator when an entity manager factory is available.
     *
     * @param entityManagerFactory application entity manager factory
     * @return JPA search definition validator
     */
    @Bean
    @ConditionalOnBean(EntityManagerFactory.class)
    @ConditionalOnMissingBean
    public JpaSearchDefinitionValidator jpaSearchDefinitionValidator(EntityManagerFactory entityManagerFactory) {
        return new JpaSearchDefinitionValidator(entityManagerFactory);
    }

    /**
     * Creates the search compiler once an RSQL engine is available.
     *
     * @param engine configured RSQL engine
     * @param definitionValidators additional definition validators
     * @param properties configured search limits
     * @return search compiler
     */
    @Bean
    @ConditionalOnBean(SearchRsqlEngine.class)
    @ConditionalOnMissingBean
    public SearchCompiler searchCompiler(
            SearchRsqlEngine engine,
            ObjectProvider<SearchDefinitionValidator> definitionValidators,
            RsqlJpaSearchProperties properties) {
        return new SearchCompiler(
                engine,
                properties.toPolicy(),
                definitionValidators.orderedStream().toList());
    }
}
