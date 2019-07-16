# first stage - build and package app
FROM registry.promethist.ai/common/common/builder AS builder

WORKDIR /project

#COPY ./build.gradle.kts .
#COPY ./gradlew .
#COPY ./gradle/wrapper ./gradle/wrapper/
## this prevents docker build to download gradle and its dependencies every run (layer will be taken from docker cache)
#RUN ./gradlew downloadDependencies

COPY . .
RUN ./gradlew assembleDist

# second stage - build app runtime
FROM registry.promethist.ai/common/common/java:openjdk-8

WORKDIR /app

COPY --from=builder /project/build/distributions/datastore-app.tar .

RUN tar xvf datastore-app.tar && mv datastore-app/* . && rm -rf datastore-app*

CMD ["/app/bin/datastore-app"]