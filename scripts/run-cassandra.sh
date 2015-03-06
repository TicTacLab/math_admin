#!/bin/sh
docker run -d -p 9160:9160 -p 9042:9042 --name cassandra spotify/cassandra
