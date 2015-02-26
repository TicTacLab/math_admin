CREATE KEYSPACE malt WITH REPLICATION = {'class' : 'SimpleStrategy', 'replication_factor' : 1};

CREATE TABLE malt.configuration (config varchar PRIMARY KEY);
