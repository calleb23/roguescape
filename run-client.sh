#!/bin/bash
set -e
cd "$(dirname "$0")"
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
echo "Starting RuneLite developer client for roguescape..."
./gradlew run -PmainClass=com.pluginideahub.roguescape.RogueScapePluginTestClient
