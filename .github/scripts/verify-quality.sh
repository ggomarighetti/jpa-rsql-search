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
./mvnw -B -ntp -nsu -DskipTests dependency:analyze

if [[ "${skip_docker}" != "true" ]]; then
  if command -v docker >/dev/null 2>&1; then
    docker compose --file .github/tools/compose.yml run --rm actionlint -color

    if [[ -f target/bom.json ]]; then
      docker compose --file .github/tools/compose.yml run --rm \
        osv-scanner \
        scan \
        -L /workspace/target/bom.json
    else
      echo "Skipping OSV scan because target/bom.json was not generated." >&2
    fi
  else
    echo "Docker was not found; skipping actionlint and OSV checks." >&2
  fi
fi
