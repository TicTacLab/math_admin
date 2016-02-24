-- name: write-user*!
INSERT INTO users
(name, login, is_admin, password, status)
VALUES
(:name, :login, :is_admin, :password, :status);

-- name: get-users*
SELECT name, login, status, is_admin
FROM users;

-- name: get-user*
SELECT login, name, is_admin
FROM users
WHERE login = :login;

-- name: get-user-with-password*
SELECT password, status, is_admin, login
FROM users
WHERE login = :login;

-- name: update-user-info*!
UPDATE users
SET
 name = :name,
 is_admin = :is_admin
WHERE login = :login;

-- name: update-user-password*!
UPDATE users
SET
 password = :password
WHERE login = :login;

-- name: update-user-status*!
UPDATE users
SET
 status = :status
WHERE login = :login;