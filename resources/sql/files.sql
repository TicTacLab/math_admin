-- name: write-file*!
INSERT INTO files
(id, name, file, file_name, content_type,
 in_sheet_name, out_sheet_name, rev, last_modified)
VALUES
(:id, :name, :file::text, :file_name, :content_type,
 :in_sheet_name, :out_sheet_name, :rev, :last_modified::timestamp);

-- name: get-files*
SELECT
 id, name, file_name, in_sheet_name,
 out_sheet_name, last_modified, rev, content_type
FROM files;

-- name: get-file*
SELECT id, name, file_name, in_sheet_name, out_sheet_name
FROM files
WHERE id = :id;

-- name: get-raw-file*
SELECT
 id, name, file, file_name, in_sheet_name, out_sheet_name,
 last_modified, rev, content_type
FROM files
WHERE id = :id;

-- name: update-with-raw-file*!
UPDATE files
SET
 file = :file::text,
 name = :name,
 file_name = :file_name,
 content_type = :content_type,
 rev = :rev,
 last_modified = :last_modified::timestamp
WHERE id = :id;

-- name: update-without-raw-file*!
UPDATE files
SET
 name = :name
WHERE id = :id;