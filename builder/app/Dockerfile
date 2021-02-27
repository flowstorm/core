FROM registry.gitlab.com/promethistai/system/java:openjdk-8-alpine

RUN apk add --no-cache bash rsync && \
    cd /tmp && \
    wget https://github.com/JetBrains/kotlin/releases/download/v1.4.20/kotlin-compiler-1.4.20.zip && \
    unzip kotlin-compiler-*.zip && \
    rsync -a kotlinc/ /usr/local/ && \
    rm -rf /var/cache/apk/*

COPY target/app.jar app.jar

CMD ["sh", "-c", "java $JAVA_OPTIONS -jar app.jar"]