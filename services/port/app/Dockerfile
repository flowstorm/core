# first stage - build app preferably using common builder
FROM registry.promethist.ai/common/common/builder AS builder

WORKDIR /project

# download dependencies
COPY pom.xml .
RUN mvn verify --fail-never

# build and package app
COPY . .
RUN mvn clean package

# second stage - build app runtime
FROM jetty:9.4.18-jre8-alpine

RUN java -jar $JETTY_HOME/start.jar --create-startd --add-to-start=http2 --approve-all-licenses

COPY --from=builder /project/app.properties .
COPY --from=builder /project/target/app.war /var/lib/jetty/webapps/ROOT.war