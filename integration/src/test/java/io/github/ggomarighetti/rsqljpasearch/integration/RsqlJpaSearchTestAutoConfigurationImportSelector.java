package io.github.ggomarighetti.rsqljpasearch.integration;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

final class RsqlJpaSearchTestAutoConfigurationImportSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[] {
            "io.github.ggomarighetti.rsqljpasearch.autoconfigure.RsqlJpaSearchEngineAutoConfiguration",
            "io.github.ggomarighetti.rsqljpasearch.autoconfigure.RsqlJpaSearchAutoConfiguration"
        };
    }
}
