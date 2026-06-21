FROM gcr.io/oss-fuzz-base/base-builder-jvm

RUN apt-get update \
    && apt-get install --no-install-recommends -y maven openjdk-17-jdk \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

COPY . $SRC/jpa-rsql-search
WORKDIR $SRC/jpa-rsql-search

COPY .clusterfuzzlite/build.sh $SRC/
COPY .clusterfuzzlite/RsqlSearchFuzzer.java $SRC/
