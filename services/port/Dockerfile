# first stage - build app preferably using common builder
FROM registry.promethist.ai/common/system/builder AS builder

ARG API_HOST=localhost:8080

WORKDIR /project

# download dependencies
COPY pom.xml .
COPY api/pom.xml api/
COPY app/pom.xml app/
RUN mvn verify --fail-never

# build and package app
COPY . .
RUN mvn -Dapi.host=${API_HOST} -B clean package

# second stage - build app runtime
FROM jetty:9.4.18-jre8-alpine

RUN java -jar $JETTY_HOME/start.jar --create-startd --add-to-start=http2 --approve-all-licenses

COPY --from=builder /project/api/target/classes/META-INF/port-api/swagger.* /app/
COPY --from=builder /project/app/app.properties .
COPY --from=builder /project/app/target/app.war /var/lib/jetty/webapps/ROOT.war