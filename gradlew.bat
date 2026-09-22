@rem Gradle startup script for Windows
@echo off
set DIR=%~dp0
"%JAVA_HOME%\bin\java.exe" -Xmx64m -Xms64m -Dorg.gradle.appname="gradlew.bat" -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
