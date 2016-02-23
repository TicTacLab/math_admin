-- name: write-file*!
INSERT INTO files
(id, name, file, file_name, content_type, in_sheet_name, out_sheet_name)
VALUES
(:id, :name, :file::text, :file_name, :content_type, :in_sheet_name, :out_sheet_name);
