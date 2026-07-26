#!/bin/bash
# Fully detached build script — survives parent shell exit
set -e
export JAVA_HOME=/home/z/tools/jdk17
export ANDROID_HOME=/home/z/tools/sdk
export ANDROID_SDK_ROOT=/home/z/tools/sdk
# Use smaller heap to avoid OOM kill (system has only ~3.5GB free)
export GRADLE_OPTS="-Xmx1536m -Dorg.gradle.jvmargs=-Xmx1536m -Dorg.gradle.workers.max=2"
export JAVA_TOOL_OPTIONS="-Xmx1536m"
# Don't fork a separate kotlin daemon — use the gradle process
export KOTLIN_DAEMON_JVM_OPTIONS="-Xmx1024m"
export PATH=$JAVA_HOME/bin:$PATH
cd /home/z/my-project/AndroidIME
echo "=== BUILD STARTED $(date) ==="
./gradlew :app:assembleRelease --no-daemon -x lint -x lintVitalRelease --no-parallel --max-workers=1
RESULT=$?
echo "=== BUILD FINISHED $(date) exit=$RESULT ==="
if [ $RESULT -eq 0 ]; then
    ls -la app/build/outputs/apk/release/
fi
exit $RESULT
