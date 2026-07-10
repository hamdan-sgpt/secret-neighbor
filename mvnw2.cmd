@REM Maven Wrapper for Windows
@REM This script downloads Maven if needed and runs it

@echo off
setlocal


set JAVA_HOME=C:\Users\Pongo\.antigravity-ide\extensions\redhat.java-1.55.0-win32-x64\jre\21.0.11-win32-x86_64
set MAVEN_PROJECTBASEDIR=%~dp0

if not exist "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar" (
    echo Maven wrapper jar not found. Please run setup.
    exit /b 1
)

"%JAVA_HOME%\bin\java.exe" -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR:~0,-1%" -classpath "%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain %*
