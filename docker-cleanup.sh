#!/bin/bash
echo "Wiping docker containers and volumes"
docker rm -f icedata-v2-db && echo "DB Removed"
docker volume rm live-demo_icedata-v2-mysql-volume && echo "DB Volume removed"
docker rm -f icedata-v2-java && echo "Java app container removed"

