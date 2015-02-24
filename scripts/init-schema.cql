CREATE KEYSPACE malt_admin WITH REPLICATION = {'class' : 'SimpleStrategy', 'replication_factor' : 1};

CREATE TABLE malt_admin.configuration (
       profile varchar PRIMARY KEY,
       config varchar
);
