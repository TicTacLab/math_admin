CREATE KEYSPACE malt WITH REPLICATION = {'class' : 'SimpleStrategy', 'replication_factor' : 1};

CREATE TABLE malt_admin.configuration (
       profile varchar PRIMARY KEY,
       config varchar
);

CREATE TABLE IF NOT EXISTS malt_admin.models (
       id int PRIMARY KEY,
       name varchar,
       file blob,
       file_name varchar,
       in_sheet_name varchar,
       out_sheet_name varchar
);