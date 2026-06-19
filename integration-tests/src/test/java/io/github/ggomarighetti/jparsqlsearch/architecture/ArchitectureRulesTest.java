package io.github.ggomarighetti.jparsqlsearch.architecture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchitectureRulesTest {
    private static final List<String> PRODUCT_MODULES = List.of(
            "jpa-rsql-search-api",
            "jpa-rsql-search-rsql-spi",
            "jpa-rsql-search-core",
            "jpa-rsql-search-jpa-validation",
            "jpa-rsql-search-perplexhub",
            "jpa-rsql-search-spring-boot-starter");

    private final JavaClasses classes =
            new ClassFileImporter()
                    .withImportOption(new ImportOption.DoNotIncludeTests())
                    .importPackages("io.github.ggomarighetti.jparsqlsearch");

    @Test
    void productionModulesAreInternallyAcyclic() {
        Path root = Path.of(System.getProperty("workspace.root"));
        for (String module : PRODUCT_MODULES) {
            JavaClasses moduleClasses = new ClassFileImporter()
                    .importPath(root.resolve(module).resolve("target/classes"));
            slices().matching("io.github.ggomarighetti.jparsqlsearch.(*)..")
                    .should().beFreeOfCycles()
                    .check(moduleClasses);
        }
    }

    @Test
    void sonarIntendedArchitectureMapsEveryProductionPackage() throws IOException {
        Path root = Path.of(System.getProperty("workspace.root"));
        JsonNode model = new ObjectMapper()
                .readTree(Files.readString(root.resolve(".sonar/architecture-model.json")));
        Set<String> mappedPatterns = new HashSet<>();
        JsonNode perspective = model.path("perspectives").path(0);
        assertEquals(
                "exclusive-allow",
                perspective.path("constraints").path(0).path("relation").asText(),
                "Sonar documents production packages; Maven and ArchUnit enforce the dependency DAG.");
        perspective
                .path("groups")
                .forEach(group -> {
                    assertEquals(
                            0,
                            group.path("groups").size(),
                            () -> "Sonar architecture package groups must stay flat: " + group.path("label").asText());
                    assertEquals(
                            1,
                            group.path("patterns").size(),
                            () -> "Every Sonar architecture package group must declare exactly one pattern: "
                                    + group.path("label").asText());
                    mappedPatterns.add(group.path("patterns").path(0).asText());
                });

        Set<String> expectedPatterns = expectedSonarPackagePatterns(root);
        assertEquals(
                expectedPatterns,
                mappedPatterns,
                () -> "Sonar architecture patterns differ from production Java packages: " + mappedPatterns);
    }

    @Test
    void capabilitiesDoNotDependOnDefinitionComposition() {
        noClasses().that().resideInAnyPackage(
                        "..filter..",
                        "..sort..")
                .should().dependOnClassesThat().resideInAPackage("..definition..")
                .check(classes);
        noClasses().that().resideInAPackage("io.github.ggomarighetti.jparsqlsearch.validation..")
                .should().dependOnClassesThat().resideInAPackage("..definition..")
                .check(classes);
    }

    @Test
    void backendSpiDoesNotDependOnEngine() {
        noClasses().that().resideInAPackage("..rsql.backend..")
                .and().resideOutsideOfPackage("..rsql.backend.perplexhub..")
                .should().dependOnClassesThat().resideInAPackage("..rsql.engine..")
                .check(classes);
    }

    @Test
    void coreDoesNotDependOnPerplexhub() {
        noClasses().that().resideInAnyPackage("..compile..", "..rsql.engine..")
                .should().dependOnClassesThat().resideInAPackage("..rsql.backend.perplexhub..")
                .check(classes);
    }

    @Test
    void operatorMetadataDoesNotDependOnJpaBindings() {
        noClasses().that().resideInAPackage("..rsql.metadata..")
                .should().dependOnClassesThat().resideInAPackage("..rsql.jpa..")
                .check(classes);
    }

    private static Set<String> expectedSonarPackagePatterns(Path root) throws IOException {
        Set<String> patterns = new HashSet<>();
        for (String module : PRODUCT_MODULES) {
            Set<String> packageNames = productionPackageNames(root, module);
            packageNames.forEach(packageName -> patterns.add(
                    module + "/src/main/java/" + packageName.replace('.', '/') + "/*.java"));
        }
        return patterns;
    }

    private static Set<String> productionPackageNames(Path root, String module) throws IOException {
        Set<String> packageNames = new HashSet<>();
        Path sourceRoot = root.resolve(module).resolve("src/main/java");
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .map(sourceRoot::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace('\\', '.').replace('/', '.'))
                    .map(path -> path.substring(0, path.lastIndexOf('.')))
                    .map(path -> path.substring(0, path.lastIndexOf('.')))
                    .forEach(packageNames::add);
        }
        return packageNames;
    }
}
