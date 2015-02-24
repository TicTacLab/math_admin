CREATE KEYSPACE cluster_store WITH REPLICATION = {'class' : 'SimpleStrategy', 'replication_factor' : 1};

CREATE TABLE cluster_store.configuration (
       profile varchar PRIMARY KEY,
       config varchar
);
