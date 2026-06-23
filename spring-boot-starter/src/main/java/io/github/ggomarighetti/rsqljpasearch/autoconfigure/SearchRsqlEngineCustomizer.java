package io.github.ggomarighetti.rsqljpasearch.autoconfigure;

import io.github.ggomarighetti.rsqljpasearch.rsql.engine.SearchRsqlEngineBuilder;

/** Customizes the Spring Boot auto-configured RSQL engine builder. */
@FunctionalInterface
public interface SearchRsqlEngineCustomizer {
    /**
     * Applies engine customizations.
     *
     * @param builder builder to customize
     */
    void customize(SearchRsqlEngineBuilder builder);
}
