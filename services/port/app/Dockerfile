# first stage - build app preferably using common builder
FROM registry.promethist.ai/common/common/builder AS builder

ARG version=0.0.0
ARG api_host=server1:8080

WORKDIR /project

# download dependencies
COPY pom.xml .
RUN mvn verify --fail-never

# build and package app
COPY . .
#RUN mvn versions:set -DnewVersion=${version} versions:commit
RUN mvn -Dapi.host=${api_host} package

# second stage - build app runtime
FROM registry.promethist.ai/common/common/java:openjdk-8

WORKDIR /app

#TODO copy app files from builder into runtime
COPY --from=builder /project/config.properties /app/
COPY --from=builder /project/target/classes/swagger.* /app/
COPY --from=builder /project/target/app-jar-with-dependencies.jar /app/app.jar

CMD ["java", "-jar", "app.jar" ]