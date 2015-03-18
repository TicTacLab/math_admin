#!/bin/sh
docker run -d -p 127.0.0.1:9160:9160 -p 127.0.0.1:9042:9042 --name cassandra spotify/cassandra
