-- name: create-session*!
INSERT INTO sessions
(session_id, login, last_used)
VALUES
(:session_id, :login, :last_used);

-- name: get-login-by-session-id*
SELECT login
FROM sessions
WHERE session_id = :session_id

-- name: get-expire*
SELECT last_used
FROM sessions
WHERE session_id = :session_id

-- name: delete-session*!
DELETE FROM sessions
where session_id = :session_id;

-- name: update-session*!
UPDATE sessions
SET
 last_used = :last_used
WHERE session_id = :session_id;