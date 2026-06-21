# OpenSSF Adoption

The project uses OpenSSF OSPS Baseline 2026.02.19 as its security-control
catalog and OpenSSF Scorecard as continuous automated feedback.

## Implemented evidence

- public source, history, issue tracker, and discussions;
- MIT licensing in source and release artifacts;
- protected primary branch and required CI;
- security, contribution, governance, support, dependency, and secrets policy;
- private vulnerability reporting and coordinated disclosure;
- Dependabot, dependency review, OSV, CodeQL, SonarQube, and secret scanning;
- full-SHA GitHub Action references and least-privilege tokens;
- CycloneDX SBOMs, PGP signatures, checksums, and artifact attestations;
- architecture, threat model, property testing, and coverage-guided fuzzing.

## Known maturity limitation

The project currently has one maintainer. It does not claim controls that
require independent human approval, a bus factor of two, or multiple
unassociated contributors.

Scorecard findings are treated as risk signals, not as a target to game.
