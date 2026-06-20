import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

const projectKey = process.env.SONAR_PROJECT_KEY ?? "ggomarighetti_jpa-rsql-search";
const organizationId =
  process.env.SONAR_ORGANIZATION_ID ?? "df8bd268-70a4-4338-a093-2cfebc6ad4ff";
const token = process.env.SONAR_TOKEN;
const checkOnly = process.argv.includes("--check");
const modelPath = resolve(".sonar", "architecture-model.json");
const expectedModel = expectedArchitectureModel();

const model = JSON.parse(await readFile(modelPath, "utf8"));
validateModel(model);

if (checkOnly) {
  console.log("Sonar intended architecture declaration is valid.");
  process.exit(0);
}

if (!token) {
  throw new Error("SONAR_TOKEN is required to synchronize the architecture model.");
}

const mainBranchId = await findMainBranchId();
const authorization = `Basic ${Buffer.from(`${token}:`).toString("base64")}`;
const commonHeaders = {
  accept: "application/json",
  authorization,
  "content-type": "application/json",
};
const models = await sonarRequest(
  `https://api.sonarcloud.io/architecture/models?projectId=${encodeURIComponent(mainBranchId)}`,
  { headers: commonHeaders },
);

if (models.length === 0) {
  await sonarRequest("https://api.sonarcloud.io/architecture/models", {
    method: "POST",
    headers: commonHeaders,
    body: JSON.stringify({
      projectId: mainBranchId,
      organizationId,
      model,
    }),
  });
  console.log(`Created the Sonar intended architecture for ${projectKey}.`);
} else {
  await sonarRequest(
    `https://api.sonarcloud.io/architecture/models/${encodeURIComponent(models[0].id)}`,
    {
      method: "PATCH",
      headers: {
        ...commonHeaders,
        "content-type": "merge-patch+json",
      },
      body: JSON.stringify({ model }),
    },
  );
  console.log(`Updated the Sonar intended architecture for ${projectKey}.`);
}

async function findMainBranchId() {
  const response = await fetch(
    `https://sonarcloud.io/api/project_branches/list?project=${encodeURIComponent(projectKey)}`,
    { headers: { accept: "application/json" } },
  );
  if (!response.ok) {
    throw new Error(`Unable to list Sonar branches: HTTP ${response.status}`);
  }
  const payload = await response.json();
  const mainBranch = payload.branches?.find((branch) => branch.isMain);
  if (!mainBranch?.branchId) {
    throw new Error(`Unable to resolve the main Sonar branch for ${projectKey}.`);
  }
  return mainBranch.branchId;
}

async function sonarRequest(url, options) {
  const response = await fetch(url, options);
  if (!response.ok) {
    const body = await response.text();
    throw new Error(`Sonar architecture request failed: HTTP ${response.status}: ${body}`);
  }
  if (response.status === 204) {
    return undefined;
  }
  const body = await response.text();
  return body ? JSON.parse(body) : undefined;
}

function validateModel(candidate) {
  if (JSON.stringify(candidate) !== JSON.stringify(expectedModel)) {
    throw new Error(
      "The architecture declaration must describe the v2 Maven module DAG and its Sonar constraints.",
    );
  }
}

function expectedArchitectureModel() {
  return {
    perspectives: [
      {
        label: "v2-maven-reactor",
        description: "Direct v2 module DAG for the publishable jpa-rsql-search reactor.",
        groups: [
          group(
            "api",
            "Stable public definitions, request value objects, policy types, and API-level RSQL operator names.",
            "jpa-rsql-search-api/src/main/java/**",
          ),
          group(
            "rsql-spi",
            "Neutral RSQL AST, parser, operator metadata, backend contracts, and JPA binding SPI.",
            "jpa-rsql-search-rsql-spi/src/main/java/**",
          ),
          group(
            "core",
            "Search compiler, protection guards, RSQL engine orchestration, and runtime validation failures.",
            "jpa-rsql-search-core/src/main/java/**",
          ),
          group(
            "jpa-validation",
            "JPA metamodel validation over compiled search definitions.",
            "jpa-rsql-search-jpa-validation/src/main/java/**",
          ),
          group(
            "perplexhub",
            "Perplexhub backend adapter and convenience engine builders.",
            "jpa-rsql-search-perplexhub/src/main/java/**",
          ),
          group(
            "spring-boot-starter",
            "Spring Boot autoconfiguration and the recommended application entry point.",
            "jpa-rsql-search-spring-boot-starter/src/main/java/**",
          ),
        ],
        constraints: [
          exclusiveAllow(
            "The v2 API is the bottom of the public DAG: it may be consumed by every publishable module, but it must not consume implementation modules.",
            ["rsql-spi", "core", "jpa-validation", "perplexhub", "spring-boot-starter"],
            ["api"],
          ),
          exclusiveAllow(
            "The neutral RSQL SPI may be used only by the core engine, concrete backends, and the Spring Boot starter.",
            ["core", "perplexhub", "spring-boot-starter"],
            ["rsql-spi"],
          ),
          exclusiveAllow(
            "Core owns compilation and protection logic; only validation, backend convenience, and starter modules may depend on it.",
            ["jpa-validation", "perplexhub", "spring-boot-starter"],
            ["core"],
          ),
          exclusiveAllow(
            "JPA validation is an optional runtime validator and should only be assembled by the starter.",
            ["spring-boot-starter"],
            ["jpa-validation"],
          ),
          exclusiveAllow(
            "Perplexhub is an optional backend adapter and should only be assembled by the starter.",
            ["spring-boot-starter"],
            ["perplexhub"],
          ),
          deny(
            "The Spring Boot starter is the application entry point; lower-level library modules must not depend on autoconfiguration.",
            ["api", "rsql-spi", "core", "jpa-validation", "perplexhub"],
            ["spring-boot-starter"],
          ),
        ],
      },
    ],
    constraints: [
      deny(
        "Production code must not depend on test fixtures, integration consumers, or verification-only sources.",
        ["jpa-rsql-search-*/src/main/java/**"],
        ["jpa-rsql-search-*/src/test/java/**", "integration-tests/**"],
      ),
    ],
  };
}

function group(label, description, pattern) {
  return {
    label,
    description,
    patterns: [pattern],
  };
}

function exclusiveAllow(message, from, to) {
  return {
    message,
    from,
    to,
    relation: "exclusive-allow",
  };
}

function deny(message, from, to) {
  return {
    message,
    from,
    to,
    relation: "deny",
  };
}
