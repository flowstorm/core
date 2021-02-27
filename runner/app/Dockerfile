FROM registry.gitlab.com/promethistai/system/java:openjdk-8-alpine

RUN apk add --no-cache ffmpeg && \
    ln -s /usr/bin/ffmpeg /usr/local/bin/ && \
    rm -rf /var/cache/apk/*

COPY target/app.jar app.jar

CMD ["sh", "-c", "java $JAVA_OPTIONS -jar app.jar"]