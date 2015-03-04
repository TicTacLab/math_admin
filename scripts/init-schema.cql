CREATE TABLE IF NOT EXISTS configuration (
       config varchar PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS settings (
       settings varchar PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS models (
       id int PRIMARY KEY,
       name varchar,
       file blob,
       file_name varchar,
       content_type varchar,
       in_sheet_name varchar,
       out_sheet_name varchar
);

CREATE TABLE IF NOT EXISTS users (
       login varchar PRIMARY KEY,
       name varchar,
       password varchar,
       status varchar
);

INSERT INTO
       users (login,name,password,status)
VALUES
	('admin', 'Admin', '$s0$e0801$eRcvK6dZYIhXWbvWJjCZ0g==$8dePdNlKUclyFiXrrMTRXg3xNwuZHVl22yeaBvzU+34=', 'active');


CREATE TABLE IF NOT EXISTS sessions (
       session_id uuid PRIMARY KEY,
       login varchar
);