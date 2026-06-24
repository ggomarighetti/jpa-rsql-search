#!/usr/bin/env bash
set -euo pipefail

skip_docker=false
skip_release=false

for arg in "$@"; do
  case "${arg}" in
    --skip-docker-checks)
      skip_docker=true
      ;;
    --skip-release-profile)
      skip_release=true
      ;;
    *)
      echo "Unknown argument: ${arg}" >&2
      exit 2
      ;;
  esac
done

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${root}"

./mvnw -B -ntp -nsu -Pcoverage,sbom verify

if [[ "${skip_release}" != "true" ]]; then
  ./mvnw -B -ntp -nsu -Pcoverage,release,sbom,reproducible verify
fi

./mvnw -B -ntp -nsu -pl integration -am -Pconsumer-tests install
./mvnw -B -ntp -nsu -DskipTests org.apache.maven.plugins:maven-dependency-plugin:3.9.0:analyze

if [[ "${skip_docker}" != "true" ]]; then
  if command -v docker >/dev/null 2>&1; then
    docker run --rm \
      --volume "${root}:/repo" \
      --workdir /repo \
      rhysd/actionlint@sha256:b1934ee5f1c509618f2508e6eb47ee0d3520686341fec936f3b79331f9315667 \
      -color

    if [[ -f target/bom.json ]]; then
      docker run --rm \
        --volume "${root}:/src" \
        ghcr.io/google/osv-scanner@sha256:5116601dedc01c1c580eb92371883ec052fc4c13c3fbc109d621a63ac416d475 \
        scan \
        -L /src/target/bom.json
    else
      echo "Skipping OSV scan because target/bom.json was not generated." >&2
    fi
  else
    echo "Docker was not found; skipping actionlint and OSV checks." >&2
  fi
fi
