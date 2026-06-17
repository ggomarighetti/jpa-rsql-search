# Changelog

All notable changes to this project are documented here. Releases follow
Semantic Versioning and release notes are maintained from Conventional Commits.

## [1.0.0](https://github.com/ggomarighetti/jpa-rsql-search/releases/tag/v1.0.0) (2026-06-17)

### Features

- Define a stable Spring-friendly contract for guarded RSQL to JPA
  `Specification` compilation.
- Provide `SearchDefinition<T>` as the public boundary for exposed selectors,
  entity paths, filtering, sorting, paging, query text, validation rules, and
  protection limits.
- Include bounded RSQL parsing, operator whitelisting, value conversion,
  structured validation errors, and mandatory application-owned predicates.
- Ship Spring Boot auto-configuration and configuration metadata for
  `jpa.rsql.search.*` properties.
- Support Java 17+, Spring Boot 4.x, Spring Data JPA, Hibernate Validator, and
  PostgreSQL-backed integration coverage.

### Release Infrastructure

- Publish `io.github.ggomarighetti:jpa-rsql-search:1.0.0` to Maven Central via
  Central Portal and JReleaser.
- Generate and validate release Javadocs, source artifacts, PGP signatures, and
  Maven Central checksums.
- Manage future release PRs with Release Please and Conventional Commits.
