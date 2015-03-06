#!/bin/sh
docker run -it --rm -v `pwd`:/data spotify/cassandra:base cqlsh -f init-config.cql $1
