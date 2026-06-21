# Dependency Policy

## Selection

Dependencies must have a clear project need, compatible license, maintained
upstream, published source, and a support range compatible with Java 17 and the
supported Spring ecosystem.

Prefer:

- standard Java or Spring capabilities;
- dependencies managed by the Spring Boot BOM;
- small, focused libraries over broad frameworks;
- releases available from Maven Central.

Avoid unmaintained, unpublished, vendored, snapshot, or repository-local
dependencies in released artifacts.

## Tracking and updates

Maven POMs are the authoritative dependency manifests. Dependabot monitors Maven
and GitHub Actions weekly. Pull requests introducing dependencies are reviewed
for vulnerabilities, provenance, maintenance, transitive impact, and licenses.

## Vulnerability thresholds

- Critical and high exploitable vulnerabilities block merge and release.
- Medium findings require triage and a remediation plan within 60 days.
- Low findings are handled according to impact and release cadence.
- A suppression requires evidence, owner, scope, rationale, and review date.
- Non-exploitable findings should be represented in VEX for released software.

## Licenses

MIT, BSD, Apache-2.0, ISC, and similarly permissive licenses are normally
acceptable. Copyleft, source-available, unknown, or conflicting licenses require
explicit review before adoption.

## Release gate

Release builds generate a CycloneDX SBOM and run software composition analysis.
Applicable policy violations must be resolved before publication.
