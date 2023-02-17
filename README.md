# Roles service

## Problem approach
The iterative process described below was followed to continuously improve the solution until all
acceptance criteria were met.
1. **Analysis:**
   1. requirements and constraints analysis
   2. familiarization with Team and User services
2. **Solution research:**
   1. high-level solution outlining
   2. design of data schema, REST API and architecture
   3. identification of technologies required to address the problem
3. **Planning:**
   1. outlined solution break down into codeable and manageable features
   2. implementation plan detailing
4. **Implementation:**
   1. selection of a single acceptance criterion
   2. development of the feature satisfying the AC along with tests to validate its fulfilment 
5. **AC validation:**
   1. review of written tests to ensure the selected AC is actually satisfied
   2. additional tests are added if necessary

## Solution description

This solution consists of an HTTP server application providing separate endpoints for each requested
feature and a relational database to store roles and role assignments. It is envisioned to
seamlessly extend the existing Team and User APIs so that they can be accessed through
a unified API Gateway.

## Architecture overview
![Architecture overview](img/architecture-overview.png?raw=true)

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

## Application components
The components of the application are organized into separate packages as follows:
- **client**: interaction with external services
- **config**: configurable parameters definition
- **database**: connection, transaction and migration management of relational databases
- **repository**: interaction with databases
- **routes**: HTTP REST endpoints definition
- **server**: routes hosting
- **service**: features implementation

## Database
The decision to use a relational database was based on the understanding that the underlying data
is inherently relational and one of the asked features requires joins. In order to maintain the
flexibility of the database schema, this service runs a Flyway migration command upon startup.

## Data model
![Data model](img/data-model.png?raw=true)

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

After that you should be able run the following command:

```shell
sbt IntegrationTest/test
```

### Application
First spin up the database container then launch the application using `sbt`.
```shell
docker compose up
sbt run
```

## Suggestions for improving Team and User services
1. When a team or user is not found, it's preferable to return a 404 status with an empty body
   rather than a 200 status with the string "null" as response body.
2. Publish an initial database snapshot and from there on the DDL events to a message/event-oriented
   middleware so that we can subscribe to them to keep our database synchronized and consistent.
3. Add distributed tracing support