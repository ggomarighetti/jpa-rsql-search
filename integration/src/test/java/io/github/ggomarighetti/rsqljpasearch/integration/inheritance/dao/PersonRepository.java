package io.github.ggomarighetti.rsqljpasearch.integration.inheritance.dao;

import io.github.ggomarighetti.rsqljpasearch.integration.inheritance.domain.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PersonRepository
        extends JpaRepository<Person, Long>, JpaSpecificationExecutor<Person> {
}
