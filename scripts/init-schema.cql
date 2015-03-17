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
       status varchar,
       is_admin boolean
);

INSERT INTO
       users (login,name,password,status, is_admin)
VALUES
	('admin', 'Admin', '$s0$e0801$eRcvK6dZYIhXWbvWJjCZ0g==$8dePdNlKUclyFiXrrMTRXg3xNwuZHVl22yeaBvzU+34=', 'active', true);


INSERT INTO
       users (login,name,password,status, is_admin)
VALUES
	('guest', 'Guest', '$s0$e0801$AVcxf3njSIifVxQKkxcmRw==$Q9IjCjfgnhI3Vl4wOUN4hYfRMZmfClAYNZVHb6UGm4g=', 'active', false);




CREATE TABLE IF NOT EXISTS sessions (
       session_id uuid PRIMARY KEY,
       login varchar
);

CREATE TABLE IF NOT EXISTS cache (
      model_id int,
      params blob,
      result blob,
      PRIMARY KEY (model_id, params)
);

CREATE TABLE IF NOT EXISTS calculation_log (
      session_id varchar,
      model_id int,
      in_params varchar,
      out_params blob,
      PRIMARY KEY ((session_id, model_id))
);
