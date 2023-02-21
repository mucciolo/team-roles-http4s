# Team roles service
Integrates and extends a User & Teams services adding the "team role" feature.

## REST API schema

### Role creation
```http request
POST http://localhost:8080/teams/roles
Content-Type: application/json

{
  "name": "Role Name"
}
```

### Role assignment
```http request
PUT http://localhost:8080/teams/b1bd995c-9219-4eea-a7c4-f5c154a7e551/members/4fa9422d-7ef0-4bdc-aa2a-e258de5ddfb8/role
Content-Type: application/json

{
  "roleId": "328fa001-14f5-401e-877c-c80109c46417"
}
```

### Lookup team member role
```http request
GET http://localhost:8080/teams/b1bd995c-9219-4eea-a7c4-f5c154a7e551/members/4fa9422d-7ef0-4bdc-aa2a-e258de5ddfb8/role
```

### Lookup role assignments
```http request
GET http://localhost:8080/roles/328fa001-14f5-401e-877c-c80109c46417/assignments
```

## Stack:
- Scala 2.13
- Http4s
- Ember
- Cats
- Circe
- Doobie
- Postgres
- Flyway

## Running instructions
### Dependencies
To run the code, please ensure that you have installed compatible versions of the following:
- Java 8
- [sbt](https://www.scala-sbt.org/) 1.8
- Docker 23
- Docker Compose v2

Once you have installed the dependencies, you can navigate to the project's root directory
and run the following commands in your terminal:

### Unit tests
```shell
sbt Test/test
```

### Integration tests

If running Docker Compose v2 then before running integrations tests you will need to create
a dashed alias of the command `docker compose` as `docker-compose` due to a limitation in
[Testcontainers](https://www.testcontainers.org/) library.
```shell
alias docker-compose='docker compose'
```

After that you should be able to run the following command:

```shell
sbt IntegrationTest/test
```

### Application
First spin up the database container then launch the application using `sbt`.
```shell
docker compose up
sbt run
```
