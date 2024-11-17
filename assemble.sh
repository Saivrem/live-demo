#!/bin/bash
old_jar='./icedata-v2/build/libs/*.jar'

echo "Removing $old_jar"
rm -rf "$old_jar"
echo "Assembling project and starting docker"
cd ./icedata-v2 || exit
./gradlew bootJar && \
cd .. && \
docker compose up -d --build