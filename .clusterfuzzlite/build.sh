#!/usr/bin/env bash
set -euo pipefail

cd "$SRC/jpa-rsql-search"

./mvnw -B -ntp -nsu -DskipTests install
./mvnw -B -ntp -nsu \
  -pl jpa-rsql-search-perplexhub \
  -am \
  dependency:copy-dependencies \
  -DincludeScope=runtime \
  -DoutputDirectory="$OUT/dependencies"

find . -path '*/target/*.jar' \
  ! -name '*-sources.jar' \
  ! -name '*-javadoc.jar' \
  ! -name '*-tests.jar' \
  -exec cp {} "$OUT/dependencies/" \;

build_classpath="$(find "$OUT/dependencies" -name '*.jar' -printf '%p:' | sort)"
javac -cp "${build_classpath}${JAZZER_API_PATH}" -d "$OUT" "$SRC/RsqlSearchFuzzer.java"

runtime_classpath='${this_dir}'
while IFS= read -r jar; do
  runtime_classpath="${runtime_classpath}:\${this_dir}/dependencies/$(basename "$jar")"
done < <(find "$OUT/dependencies" -name '*.jar' | sort)

cat > "$OUT/RsqlSearchFuzzer" <<EOF
#!/usr/bin/env bash
# LLVMFuzzerTestOneInput for fuzzer detection.
this_dir=\$(dirname "\$0")
LD_LIBRARY_PATH="${JVM_LD_LIBRARY_PATH}:\${this_dir}" \
\${this_dir}/jazzer_driver \
  --agent_path=\${this_dir}/jazzer_agent_deploy.jar \
  --cp="${runtime_classpath}" \
  --target_class=RsqlSearchFuzzer \
  --jvm_args="-Xmx2048m:-Djava.awt.headless=true" \
  "\$@"
EOF
chmod +x "$OUT/RsqlSearchFuzzer"
