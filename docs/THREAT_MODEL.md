# Threat Model

## Assets

- confidentiality and integrity of persisted application data;
- availability of search endpoints and databases;
- correctness of mandatory application predicates;
- integrity of published Maven artifacts and release metadata;
- repository and release credentials.

## Attackers and inputs

The primary attacker is an unauthenticated or low-privilege API consumer able to
control RSQL filters, query text, sort orders, and paging. Contributors and
compromised dependencies are relevant supply-chain threats.

## Main threats and controls

### Unauthorized data access

Threats include selecting undeclared fields, traversing sensitive relationships,
using unexpected operators, or removing tenant predicates.

Controls:

- explicit `SearchDefinition` allowlists;
- validated JPA paths and topology;
- application predicates combined with `AND`;
- no reflection-based exposure of arbitrary fields.

### Resource exhaustion

Threats include deeply nested ASTs, large `IN` lists, expensive wildcard
patterns, unbounded paging, relation sorting, and excessive joins.

Controls:

- limits for input length, AST depth/nodes, comparisons, arguments, joins,
  wildcard use, sorting, page size, and offset;
- unpaged requests disabled by default;
- to-many filtering and sorting restricted;
- property, regression, integration, and coverage-guided fuzz tests.

### Parser and type confusion

Threats include malformed encodings, unexpected parser exceptions, ambiguous
custom operators, and unsafe conversions.

Controls:

- structured exceptions;
- operator arity and type metadata;
- explicit conversion service;
- parser corpus and fuzz testing;
- backend validation before execution.

### Information disclosure

Raw persistence exceptions or query details may reveal schema or data.
Consumers must map structured validation exceptions at the application boundary
and must not return raw SQL or persistence errors.

### Supply-chain compromise

Threats include vulnerable dependencies, mutable GitHub Actions, over-privileged
tokens, release tampering, and leaked credentials.

Controls:

- Dependabot, dependency review, OSV, CodeQL, and SonarQube;
- Actions pinned to full commit SHAs;
- read-only tokens by default;
- PGP signatures, checksums, SBOMs, and artifact attestations;
- secret scanning and push protection.

## Residual risks

- The project currently has one maintainer, so independent human review and
  continuity guarantees are limited.
- Consumers can create unsafe custom backends or mandatory predicates.
- Database plans and costs depend on the consumer's schema, indexes, and data.
- No finite limit eliminates all denial-of-service risk; applications should
  also apply authentication, rate limits, timeouts, and database protections.

This model is reviewed for every major release and after confirmed security
incidents.
