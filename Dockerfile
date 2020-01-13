FROM jlesage/baseimage-gui:alpine-3.10-glibc

# Docker image version is provided via build arg.
ARG DOCKER_IMAGE_VERSION=unknown

# JDK version
ARG JDK=9
ARG TMP_DIR=muwire-tmp

ENV APP_HOME=/muwire

# Define working directory.
WORKDIR /$TMP_DIR

# Put sources into dir
COPY . .

# Install final dependencies
RUN apk add --no-cache openjdk${JDK}-jre

# Build and untar in future distribution dir
RUN apk add --no-cache openjdk${JDK}-jdk \
        && ./gradlew --no-daemon clean assemble \
        && mkdir -p ${APP_HOME} \
        # Extract to ${APP_HOME and ignore the first dir
        # First dir in tar is the "MuWire-<version>"
        && tar -C ${APP_HOME} --strip 1 -xvf gui/build/distributions/MuWire*.tar \
        # Cleanup
        && rm -rf "${TMP_DIR}" /root/.gradle /root/.java \
        && apk del openjdk${JDK}-jdk

WORKDIR ${APP_HOME}

# Maximize only the main/initial window.
RUN \
    sed-patch 's/<application type="normal">/<application type="normal" title="MuWire">/' \
        /etc/xdg/openbox/rc.xml

# Generate and install favicons.
#RUN \
#    APP_ICON_URL=https://raw.githubusercontent.com/jlesage/docker-templates/master/jlesage/images/jdownloader-2-icon.png && \
#    install_app_icon.sh "$APP_ICON_URL"

# Add files.
COPY docker/rootfs/ /

# Set environment variables.
ENV APP_NAME="MuWire" \
    S6_KILL_GRACETIME=8000

# Define mountable directories.
VOLUME ["$APP_HOME/.MuWire"]
VOLUME ["/output"]


# Metadata.
LABEL \
      org.label-schema.name="muwire" \
      org.label-schema.description="Docker container for MuWire" \
      org.label-schema.version="$DOCKER_IMAGE_VERSION" \
      org.label-schema.vcs-url="https://github.com/zlatinb/muwire" \
      org.label-schema.schema-version="1.0"
