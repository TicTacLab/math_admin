#!/bin/sh
docker run -it --rm spotify/cassandra:base cqlsh $(docker inspect --format '{{ .NetworkSettings.IPAddress }}' cassandra)
