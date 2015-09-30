CREATE TABLE IF NOT EXISTS malt.draft_models (
       session_id uuid PRIMARY KEY,
       file blob,
       file_name varchar,
       content_type varchar
);