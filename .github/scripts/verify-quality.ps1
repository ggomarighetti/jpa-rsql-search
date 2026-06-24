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
    & .\mvnw.cmd -B -ntp -nsu -DskipTests org.apache.maven.plugins:maven-dependency-plugin:3.9.0:analyze

    if (-not $SkipDockerChecks) {
        if (Get-Command docker -ErrorAction SilentlyContinue) {
            docker run --rm `
                --volume "$($root.Path):/repo" `
                --workdir /repo `
                rhysd/actionlint@sha256:b1934ee5f1c509618f2508e6eb47ee0d3520686341fec936f3b79331f9315667 `
                -color

            if (Test-Path (Join-Path $root "target\bom.json")) {
                docker run --rm `
                    --volume "$($root.Path):/src" `
                    ghcr.io/google/osv-scanner@sha256:5116601dedc01c1c580eb92371883ec052fc4c13c3fbc109d621a63ac416d475 `
                    scan `
                    -L /src/target/bom.json
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
