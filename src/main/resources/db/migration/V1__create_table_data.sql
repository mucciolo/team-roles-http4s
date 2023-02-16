CREATE TABLE roles
(
    id   UUID DEFAULT gen_random_UUID() PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

INSERT INTO
    roles (name)
VALUES
    ('Developer'),
    ('Product Owner'),
    ('Tester');

CREATE TABLE team_member_role
(
    team_id UUID,
    user_id UUID,
    role_id UUID,
    PRIMARY KEY (team_id, user_id),
    CONSTRAINT fk_role FOREIGN KEY (role_id) REFERENCES roles (id)
);
