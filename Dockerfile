FROM openjdk:8-jre

COPY service/app/target/promethist-core-service-app-1.0.0-SNAPSHOT.jar app.jar

CMD ["java","-jar","app.jar"]