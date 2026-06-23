# Changelog

All notable changes to this project are documented here. Releases follow
Semantic Versioning and release notes are maintained from Conventional Commits.

## [Unreleased]

### Breaking Changes

- Rename the project and repository from `jpa-rsql-search` to
  `rsql-jpa-search`.
- Rename published Maven modules from `jpa-rsql-search-*` to
  `rsql-jpa-search-*`.
- Move the Java namespace from
  `io.github.ggomarighetti.jparsqlsearch` to
  `io.github.ggomarighetti.rsqljpasearch`.
- Rename the Spring Boot property prefix from `jpa.rsql.search` to
  `rsql.jpa.search`.
- Rename Spring Boot auto-configuration types to use the `RsqlJpaSearch`
  prefix.

## [1.0.1](https://github.com/ggomarighetti/rsql-jpa-search/releases/tag/v1.0.1) (2026-06-18)

### Release Metadata

- Correct the published developer name to Guillermo Orue Marighetti.
- Publish release artifacts to GitHub Packages in addition to Maven Central.

## [1.0.0](https://github.com/ggomarighetti/rsql-jpa-search/releases/tag/v1.0.0) (2026-06-17)

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
