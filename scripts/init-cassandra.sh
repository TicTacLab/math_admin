docker run -it --rm -v `pwd`:/data spotify/cassandra:base cqlsh -f init-schema.cql $1
docker run -it --rm -v `pwd`:/data spotify/cassandra:base cqlsh -f init-config.cql $1
