CREATE TABLE IF NOT EXISTS files (
 id int PRIMARY KEY,
 name varchar,
 file text,
 file_name varchar,
 content_type varchar,
 in_sheet_name varchar,
 out_sheet_name varchar,
 last_modified timestamp,
 rev varchar
);

CREATE TABLE IF NOT EXISTS users (
 login varchar PRIMARY KEY,
 name varchar,
 password varchar,
 status varchar,
 is_admin boolean
);

-- INSERT INTO users
-- (login,name,password,status, is_admin)
-- VALUES
-- ('admin', 'Admin', '$s0$e0801$eRcvK6dZYIhXWbvWJjCZ0g==$8dePdNlKUclyFiXrrMTRXg3xNwuZHVl22yeaBvzU+34=', 'active', true);

CREATE TABLE IF NOT EXISTS sessions (
 session_id uuid PRIMARY KEY,
 login varchar,
 last_used timestamp
);