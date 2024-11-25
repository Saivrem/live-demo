#!/bin/bash
old_jar='./icedata-v2/build/libs/*.jar'

function cleanup() {
    echo "Wiping docker containers and volumes"
    docker compose down
    docker rm -f icedata-v2-db && echo "DB Removed"
    docker volume rm live-demo_icedata-v2-mysql-volume && echo "DB Volume removed"
    docker rm -f icedata-v2-java && echo "Java app container removed"
}

function test() {
    cd ./icedata-v2 || export
    ./gradlew clean test && cd .. || exit
}

case $1 in
-c)
  cleanup
  ;;
-t)
  test
  ;;
esac

echo "Removing $old_jar"
rm -rf "$old_jar"
echo "Assembling project and starting docker"
cd ./icedata-v2 || exit
./gradlew bootJar && \
cd .. && \
docker compose up -d --build
