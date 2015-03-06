docker run -it --rm -v `pwd`:/data spotify/cassandra:base cqlsh -f init-keyspace.cql $1
docker run -it --rm -v `pwd`:/data spotify/cassandra:base cqlsh -f init-schema.cql -k malt $1
docker run -it --rm -v `pwd`:/data spotify/cassandra:base cqlsh -f init-config.cql -k malt $1
docker run -it --rm -v `pwd`:/data spotify/cassandra:base cqlsh -f init-settings.cql -k malt $1
