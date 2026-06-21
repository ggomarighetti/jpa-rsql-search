FROM gcr.io/oss-fuzz-base/base-builder-jvm@sha256:82352abfb9612e938c95b0f3326b8fa8f843bba188d44470671f04a1555b3963

RUN apt-get update \
    && apt-get install --no-install-recommends -y maven openjdk-17-jdk \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

COPY . $SRC/jpa-rsql-search
WORKDIR $SRC/jpa-rsql-search

COPY .clusterfuzzlite/build.sh $SRC/
COPY .clusterfuzzlite/RsqlSearchFuzzer.java $SRC/
