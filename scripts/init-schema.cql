CREATE KEYSPACE IF NOT EXISTS malt WITH REPLICATION = {'class' : 'SimpleStrategy', 'replication_factor' : 1};

CREATE TABLE IF NOT EXISTS malt.configuration (
       config varchar PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS malt.models (
       id int PRIMARY KEY,
       name varchar,
       file blob,
       file_name varchar,
       content_type varchar,
       in_sheet_name varchar,
       out_sheet_name varchar
);

CREATE TABLE IF NOT EXISTS malt.users (
       login varchar PRIMARY KEY,
       name varchar,
       password varchar,
       status varchar
);