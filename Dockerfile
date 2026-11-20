FROM codercom/code-server:latest

USER root

# Установка JDK 17 и базовых утилит
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jdk-headless \
    unzip \
    curl \
    git \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV ANDROID_HOME=/home/coder/Android/Sdk
ENV PATH="${PATH}:/home/coder/.android/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/cmdline-tools/latest/bin"

USER coder
WORKDIR /home/coder

# Установка официального Android CLI и базовых пакетов SDK
RUN curl -fsSL https://google.com | bash \
    && mkdir -p ${ANDROID_HOME} \
    && android sdk install platform-tools "build-tools/35.0.0" "platforms/android-35"

# Предустановка плагина Kotlin для VS Code
RUN code-server --install-extension fwcd.kotlin \
    && code-server --install-extension vscjava.vscode-java-pack
