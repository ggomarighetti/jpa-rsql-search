# Release Security and Verification

## Published evidence

A release created by the project workflow includes:

- Maven POM, binary JAR, sources JAR, and Javadocs JAR;
- PGP signatures required by Maven Central;
- SHA-256 checksums;
- CycloneDX JSON SBOMs;
- GitHub/Sigstore artifact-attestation bundles;
- release notes identifying security changes.

## Verify Maven Central signatures

Download an artifact and its `.asc` signature from Maven Central, import the
maintainer's published key, and run:

```bash
gpg --verify artifact.jar.asc artifact.jar
```

Confirm the key fingerprint through an independent maintainer identity channel.

## Verify GitHub provenance

With GitHub CLI:

```bash
gh attestation verify artifact.jar \
  --repo ggomarighetti/rsql-jpa-search
```

Verification must identify this repository and the release workflow. Also
compare the artifact's SHA-256 digest with the release checksum manifest.

## Build from source

Use the source tag associated with the release:

```bash
./mvnw -Pcoverage,release verify
./mvnw -Ppublication,sbom -DskipTests deploy
```

The release tag must be signed and point to the commit identified by the
provenance.
