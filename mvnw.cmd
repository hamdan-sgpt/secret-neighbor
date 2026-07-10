@REM Maven Wrapper for Windows
@REM This script downloads Maven if needed and runs it

@echo off
setlocal


set MAVEN_PROJECTBASEDIR=%~dp0

if not exist "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar" (
    echo Maven wrapper jar not found. Please run setup.
    exit /b 1
)

"%JAVA_HOME%\bin\java.exe" -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR:~0,-1%" -classpath "%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain %*
