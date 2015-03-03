docker run -it --rm -v `pwd`:/data spotify/cassandra:base cqlsh -f init-settings.cql $1
