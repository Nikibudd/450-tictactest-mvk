FROM alpine:3.23

RUN apk add --no-cache \
        git \
        openssh-client \
        ca-certificates \
        bash \
        curl \
        less \
        gnupg \
    && addgroup -g 1000 dev \
    && adduser -D -u 1000 -G dev -s /bin/bash dev

# gradle/gradle-daemon-jvm.properties pins toolchainVendor=AZUL, toolchainVersion=25.
# Gradle's own auto-download resolves a glibc build for "Linux aarch64" that won't
# run on Alpine's musl libc, so install Azul's musl-specific Zulu 25 build directly
# instead (also avoids needing network access to foojay.io at build/run time).
RUN ARCH="$(uname -m)" \
    && case "$ARCH" in \
         aarch64) ZULU_ARCH=aarch64 ;; \
         x86_64) ZULU_ARCH=x64 ;; \
         *) echo "Unsupported arch: $ARCH" >&2 && exit 1 ;; \
       esac \
    && curl -fsSL "https://cdn.azul.com/zulu/bin/zulu25.36.205-ca-jdk25.0.4.1-linux_musl_${ZULU_ARCH}.tar.gz" -o /tmp/zulu25.tar.gz \
    && mkdir -p /opt/zulu25 \
    && tar -xzf /tmp/zulu25.tar.gz -C /opt/zulu25 --strip-components=1 \
    && rm /tmp/zulu25.tar.gz

ENV JAVA_HOME=/opt/zulu25
ENV PATH="${JAVA_HOME}/bin:${PATH}"


# Pre-create with dev ownership: when the named volume for the gradle cache
# mounts here for the first time, Docker copies whatever is already at this
# path in the image (owner included) into the new volume. Without this, the
# volume starts out root-owned and dev can't write to it at all.
RUN mkdir -p /home/dev/.gradle && chown -R dev:dev /home/dev/.gradle

USER dev