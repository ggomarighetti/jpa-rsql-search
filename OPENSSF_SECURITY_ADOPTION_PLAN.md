# OpenSSF: investigación, auditoría y plan de adopción

Fecha de evaluación: 2026-06-21

Repositorio: `ggomarighetti/jpa-rsql-search`

Rama de implementación: `codex/openssf-hardening`

Base: `origin/master` en `207bd2e2307aba2b1d70f67169017f4bf888c97e`

## Resumen ejecutivo

OpenSSF no es un único paquete que se instala. Es un ecosistema de estándares,
evaluaciones y herramientas para seguridad de proyectos open source. Para este
repositorio se adoptan cuatro capas complementarias:

1. OpenSSF OSPS Baseline 2026.02.19 como catálogo de controles.
2. OpenSSF Scorecard como evaluación automatizada continua.
3. OpenSSF Best Practices como autoevaluación pública manual.
4. SBOM, firmas, attestations y builds reproducibles para la cadena de
   suministro.

El código ya partía de una base fuerte: Maven multi-módulo, Java 17+, CI en
Java 17/21/25, Dependabot, CodeQL, SonarQube, Testcontainers, tags GPG y
publicación automatizada. Las brechas principales estaban en documentación,
gobernanza, permisos de Actions, dependencias de workflows sin pin, SBOM,
provenance, reproducibilidad y fuzzing coverage-guided.

## Alcance investigado

- OSPS Baseline 2026.02.19 y sus 64 requisitos.
- OpenSSF Scorecard y sus checks vigentes.
- OpenSSF Best Practices Passing, Silver y Gold.
- Security Insights 2.2.0.
- SLSA 1.2, GitHub artifact attestations y Sigstore.
- CycloneDX Maven Plugin, OSV-Scanner y GitHub Dependency Review.
- Jazzer y ClusterFuzzLite para JVM.
- Configuración remota de Actions, rulesets y seguridad de GitHub.
- POMs, workflows, JReleaser, tests y artefactos del reactor.

## Diagnóstico inicial

| Área | Estado inicial | Riesgo |
| --- | --- | --- |
| Política de seguridad | Sin `SECURITY.md` | Reportes públicos o sin SLA |
| Gobernanza | Sin roles, maintainers ni continuidad | Bus factor 1 no documentado |
| Contribución | Sin guía, DCO ni templates | Cambios inconsistentes |
| Actions | Tags móviles y token write por defecto | Compromiso de supply chain |
| SCA | Dependabot sin gate de PR secundario | Vulnerabilidades introducidas |
| Release | Sin SBOM, checksums ni provenance pública | Artefacto no trazable |
| Reproducibilidad | No medida | Build no verificable |
| Fuzzing | Property tests sin coverage guidance | Casos límite no explorados |
| Licencia en JAR | Ausente | Distribución incompleta |

El Scorecard público observado sobre `master` era 4,9/10. Parte de los
resultados eran falsos negativos, pero confirmó brechas reales en
`Security-Policy`, `Pinned-Dependencies`, `Token-Permissions`,
`Signed-Releases`, `Fuzzing`, `CII-Best-Practices` y revisión humana.

## Estrategia por control

### OSPS nivel 1

Objetivo: cumplir todos los controles aplicables.

- Fuente e historial públicos: ya cumplido.
- Licencia OSI y metadata Maven: ya cumplido.
- Licencia dentro de artefactos: implementado como `META-INF/LICENSE`.
- Proceso de contribución y soporte: implementado.
- Reporte privado de vulnerabilidades: documentado.
- Protección de rama y CI: ya existían; se endurecen.
- Secret scanning y push protection: ya habilitados.

### OSPS nivel 2 y 3

Se implementan los controles técnicos razonables, pero no se declara
cumplimiento organizativo total mientras exista un solo maintainer.

- Roles, acceso sensible y escalamiento: documentados.
- Arquitectura y threat model: documentados.
- Política de dependencias, licencias, SAST, SCA y secretos: documentada.
- DCO: automatizado.
- SBOM y release attestations: implementados.
- Reproducibilidad: implementada y verificada con dos builds.
- Revisión no autora: diferida hasta contar con un segundo maintainer real.
- VEX: se producirá cuando exista un finding no explotable que deba acompañar
  una release; generar VEX vacío no aporta evidencia.

## Cambios implementados

### Políticas y metadata

- `SECURITY.md`, `SUPPORT.md`, `CONTRIBUTING.md`.
- `CODE_OF_CONDUCT.md`, `GOVERNANCE.md`, `MAINTAINERS.md`.
- `docs/ARCHITECTURE.md`, `docs/THREAT_MODEL.md`.
- `docs/DEPENDENCY_POLICY.md`, `docs/SECRETS_POLICY.md`.
- `docs/RELEASE_SECURITY.md`, `docs/OPENSSF.md`.
- `security-insights.yml` conforme al esquema oficial 2.2.0.
- `CODEOWNERS` e issue forms.

### GitHub Actions

- Todas las Actions fijadas a SHA completa.
- Permisos read-only por defecto a nivel workflow.
- Escritura limitada al job que realmente publica o reporta.
- Checkout sin credenciales persistentes salvo autenticación explícita de
  publicación.
- OpenSSF Scorecard con publicación y SARIF.
- CodeQL explícito para Java.
- Dependency Review con bloqueo de vulnerabilidades high/critical y licencias
  copyleft de red restringida.
- OSV-Scanner sobre el SBOM agregado.
- DCO para commits humanos.
- Validación CUE de Security Insights.
- `actionlint` desde una imagen fijada por digest.
- Build reproducible semanal.

### Build y release

- Maven Enforcer exige JDK 17+ y Maven 3.9+.
- `project.build.outputTimestamp` determinista.
- `META-INF/LICENSE` en los JAR binarios.
- CycloneDX 1.6 agregado y por módulo publicable.
- `integration-tests` excluido del SBOM y del despliegue.
- `maven-artifact-plugin` valida el build plan y genera `.buildinfo`.
- El release genera SHA-256, provenance Sigstore y SBOM attestation.
- Los bundles, checksums, SBOM agregado y `.buildinfo` se adjuntan a GitHub
  Releases.
- Se conservan las firmas PGP requeridas por Maven Central.

### Fuzzing

- Jazzer JUnit ejecuta el invariante de entrada RSQL arbitraria.
- ClusterFuzzLite construye un target JVM sobre la API pública y el backend
  Perplexhub.
- PR/push: fuzzing corto.
- Schedule: fuzzing batch prolongado.
- Los crashes deben convertirse en entradas permanentes del corpus.

## Validaciones ejecutadas

- Tests Maven del core y dependencias: aprobados.
- Generación CycloneDX y `.buildinfo`: aprobada.
- Security Insights contra CUE 2.2.0: aprobado.
- Todos los workflows con actionlint 1.7.12: aprobados.
- Build ClusterFuzzLite en la imagen oficial: aprobado.
- Target ClusterFuzzLite/Jazzer: 100 ejecuciones sin crash.
- Dos builds limpios con comparación SHA-256: bit-for-bit idénticos.
- JAR inspeccionado con `META-INF/LICENSE`.

## Configuración remota propuesta

Aplicar inmediatamente:

- `default_workflow_permissions=read`.
- impedir que Actions apruebe pull requests.
- mantener bloqueo de force-push, borrado, historial lineal y PR obligatorio.
- exigir que la rama esté actualizada antes del merge.

Aplicar después de que estos workflows existan en `master`:

- exigir pin SHA desde la configuración de Actions;
- restringir Actions a GitHub, OpenSSF, Google, CUE y la allowlist usada;
- agregar como required checks Dependency Review, CodeQL, DCO, Security
  Metadata y los checks de verificación existentes;
- activar merge protection de code scanning para findings high/critical.

No aplicar todavía:

- una aprobación humana obligatoria;
- CODEOWNERS review obligatorio;
- aprobación del último push por otra persona.

Esos controles bloquearían legítimamente a un proyecto de un solo maintainer.
Se activarán al incorporar un segundo maintainer con responsabilidad real.

## OpenSSF Best Practices

El repositorio queda preparado para completar el cuestionario Passing. La
creación del registro requiere autenticación del propietario mediante GitHub y
declaraciones humanas; no debe automatizarse ni responderse ficticiamente.

Silver y Gold quedan como objetivos de madurez. Los bloqueos reales son el bus
factor, revisión independiente y comunidad, no una carencia técnica que pueda
resolverse con otro YAML.

## Definition of Done

- [x] Políticas de seguridad, soporte, gobernanza y contribución.
- [x] Security Insights validado.
- [x] Actions fijadas a SHA y mínimo privilegio.
- [x] Scorecard, CodeQL, Dependency Review, OSV y DCO.
- [x] SBOM por módulo y agregado.
- [x] Checksums, provenance y SBOM attestations en release.
- [x] Build reproducible medido.
- [x] Licencia dentro de los artefactos.
- [x] Jazzer y ClusterFuzzLite.
- [x] Documentación de verificación.
- [ ] OpenSSF Best Practices Passing, pendiente de declaración humana.
- [ ] Revisión no autora, pendiente de un segundo maintainer.
- [ ] Hacer obligatorios los nuevos checks después de observar sus nombres en
      el primer PR.

## Fuentes primarias

- [OpenSSF](https://openssf.org/)
- [OSPS Baseline 2026.02.19](https://baseline.openssf.org/versions/2026-02-19.html)
- [OpenSSF Scorecard](https://github.com/ossf/scorecard)
- [Scorecard Action](https://github.com/ossf/scorecard-action)
- [Best Practices](https://www.bestpractices.dev/)
- [Security Insights](https://github.com/ossf/security-insights)
- [SLSA 1.2](https://slsa.dev/spec/v1.2/)
- [GitHub artifact attestations](https://docs.github.com/actions/security-for-github-actions/using-artifact-attestations/using-artifact-attestations-to-establish-provenance-for-builds)
- [CycloneDX Maven Plugin](https://github.com/CycloneDX/cyclonedx-maven-plugin)
- [OSV-Scanner](https://google.github.io/osv-scanner/)
- [ClusterFuzzLite JVM](https://google.github.io/clusterfuzzlite/build-integration/jvm-lang/)
- [Maven reproducible builds](https://maven.apache.org/guides/mini/guide-reproducible-builds.html)
