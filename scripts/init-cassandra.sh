#!/bin/sh
docker run -it --rm -v `pwd`:/data spotify/cassandra:base cqlsh -f init-keyspace.cql $(docker inspect --format '{{ .NetworkSettings.IPAddress }}' cassandra)
docker run -it --rm -v `pwd`:/data spotify/cassandra:base cqlsh -f init-schema.cql -k malt $(docker inspect --format '{{ .NetworkSettings.IPAddress }}' cassandra)
docker run -it --rm -v `pwd`:/data spotify/cassandra:base cqlsh -f init-config.cql -k malt $(docker inspect --format '{{ .NetworkSettings.IPAddress }}' cassandra)
docker run -it --rm -v `pwd`:/data spotify/cassandra:base cqlsh -f init-settings.cql -k malt $(docker inspect --format '{{ .NetworkSettings.IPAddress }}' cassandra)
