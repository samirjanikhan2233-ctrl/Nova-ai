#!/bin/sh
# Gradle start up script - standard wrapper (Gradle 8.x)
DIRNAME=$(dirname "$0")
APP_HOME=$(cd "$DIRNAME" && pwd)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
exec java -Xmx64m -Xms64m -Dorg.gradle.appname="gradlew" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
