# Skeleton project

This project represents referential structure + implementation of server app using following technologies:

* support for using both Kotlin (preferred) and Java languages 
* Jersey JAX-RS standard implementation support
* Netty based http2 server instantiating Jersey application
* Swagger Maven plugin (https://github.com/kongchen/swagger-maven-plugin) to generate swagger.json + swagger.yaml resource files during build
* Common API resource classes serving `/check` + `/swagger.(json|yaml)` routes (+CORS filter)

#### After cloning

* change base package from `com.promethistai.skeleton` to `com.promethistai.<project>` in app module (API resources should be placed in `com.promethistai.<project>.resources` package)
* `pom.xml` + `<module>/pom.xml` - update groupId/artifactId/name + properties (groupId = base package, artifactId = `<project>(-<module>)`)
* `app/config.properties` + `deploy/app/values.yaml` update package
* `deploy/app/Chart.yaml` update name + description

#### Build using Maven
```
mvn -Dapi.host=localhost:8080 package
```

#### Build and run using Docker compose
```
docker-compose up -d
```

#### ToDo's 
* support for google swagger extensions (e.g. quotas)
* `com.promethistai.common.*` classes should be moved to separated java library (`com.promethistai.common:common-lib`) and referenced by dependency
* FIX compiling Java first