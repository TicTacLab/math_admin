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
CREATE KEYSPACE IF NOT EXISTS malt_test WITH REPLICATION = {'class' : 'SimpleStrategy', 'replication_factor' : 1};
  
  CREATE TABLE IF NOT EXISTS malt_test.configuration (
         config varchar PRIMARY KEY
  );
  
  CREATE TABLE IF NOT EXISTS malt_test.models (
         id int PRIMARY KEY,
         name varchar,
         file blob,
         file_name varchar,
         content_type varchar,
         in_sheet_name varchar,
         out_sheet_name varchar
  );