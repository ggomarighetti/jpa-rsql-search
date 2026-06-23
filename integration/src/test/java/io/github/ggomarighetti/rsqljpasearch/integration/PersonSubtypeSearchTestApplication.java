package io.github.ggomarighetti.rsqljpasearch.integration;

import io.github.ggomarighetti.rsqljpasearch.integration.inheritance.dao.PersonRepository;
import io.github.ggomarighetti.rsqljpasearch.integration.inheritance.domain.Person;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableJpaRepositories(basePackageClasses = PersonRepository.class)
@EntityScan(basePackageClasses = Person.class)
@Import(RsqlJpaSearchTestAutoConfigurationImportSelector.class)
class PersonSubtypeSearchTestApplication {
}
