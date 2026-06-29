param(
    [switch] $SkipDockerChecks,
    [switch] $SkipReleaseProfile
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Push-Location $root

try {
    & .\mvnw.cmd -B -ntp -nsu "-Pcoverage,sbom" verify

    if (-not $SkipReleaseProfile) {
        & .\mvnw.cmd -B -ntp -nsu "-Pcoverage,release,sbom,reproducible" verify
    }

    & .\mvnw.cmd -B -ntp -nsu -pl integration -am "-Pconsumer-tests" install
    & .\mvnw.cmd -B -ntp -nsu -DskipTests dependency:analyze

    if (-not $SkipDockerChecks) {
        if (Get-Command docker -ErrorAction SilentlyContinue) {
            docker compose --file .github/tools/compose.yml run --rm actionlint -color

            if (Test-Path (Join-Path $root "target\bom.json")) {
                docker compose --file .github/tools/compose.yml run --rm `
                    osv-scanner `
                    scan `
                    -L /workspace/target/bom.json
            } else {
                Write-Warning "Skipping OSV scan because target\bom.json was not generated."
            }
        } else {
            Write-Warning "Docker was not found; skipping actionlint and OSV checks."
        }
    }
} finally {
    Pop-Location
}
