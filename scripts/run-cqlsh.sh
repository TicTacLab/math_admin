#!/bin/sh
docker run -it --rm spotify/cassandra:base cqlsh $@
