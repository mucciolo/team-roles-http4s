CREATE TABLE roles
(
    id   UUID DEFAULT gen_random_UUID() PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

INSERT INTO
    roles (id, name)
VALUES
    ('328fa001-14f5-401e-877c-c80109c46417', 'Developer'),
    ('4b8e1517-76b8-411b-bb08-7b747b42f895', 'Product Owner'),
    ('58b229a1-7c9b-4393-bae0-096af42e85c7', 'Tester');

CREATE TABLE team_member_role
(
    team_id UUID,
    user_id UUID,
    role_id UUID,
    PRIMARY KEY (team_id, user_id),
    CONSTRAINT fk_role FOREIGN KEY (role_id) REFERENCES roles (id)
);
