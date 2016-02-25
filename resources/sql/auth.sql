-- name: create-session*!
INSERT INTO sessions
(session_id, login, expire)
VALUES
(:session_id, :login, :expire);

-- name: get-login-by-session-id*
SELECT login
FROM sessions
WHERE session_id = :session_id

-- name: get-expire*
SELECT expire
FROM sessions
WHERE session_id = :session_id

-- name: delete-session*!
DELETE FROM sessions
where session_id = :session_id;

-- name: update-session*!
UPDATE sessions
SET
 expire = :expire
WHERE session_id = :session_id;