#!/bin/sh

# Gradle wrapper script

# Encontra o diretorio raiz do projeto
APP_HOME=$(cd "$(dirname "$0")" && pwd)

# Classpath do wrapper
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Detecta o Java
if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Verifica se java existe
if ! command -v "$JAVACMD" > /dev/null 2>&1; then
    echo "ERRO: Java nao encontrado. Configure JAVA_HOME."
    exit 1
fi

# Verifica se o wrapper jar existe
if [ ! -f "$CLASSPATH" ]; then
    echo "ERRO: gradle-wrapper.jar nao encontrado em $CLASSPATH"
    exit 1
fi

# Executa o Gradle Wrapper - sem DEFAULT_JVM_OPTS problemáticos
exec "$JAVACMD" \
    -Xmx512m \
    -Xms256m \
    -Dorg.gradle.appname="$(basename "$0")" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
