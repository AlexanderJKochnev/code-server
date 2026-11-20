FROM codercom/code-server:4.96.4

USER root

# Установка JDK 21 (стандарт в Debian 13 Trixie) и базовых утилит
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jdk-headless \
    unzip \
    curl \
    git \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV ANDROID_HOME=/home/coder/Android/Sdk
ENV PATH="${PATH}:/home/coder/.local/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/cmdline-tools/latest/bin"

# ENV PATH="${PATH}:/home/coder/.android/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/cmdline-tools/latest/bin"

USER coder
WORKDIR /home/coder

# Установка официального Android CLI
RUN curl -fsSL https://dl.google.com/android/cli/latest/linux_x86_64/install.sh | bash

# Предустановка плагинов Kotlin и Java для VS Code
RUN code-server --install-extension JetBrains.intellij-server

# RUN code-server --install-extension fwcd.kotlin \
#     && code-server --install-extension vscjava.vscode-java-pack
