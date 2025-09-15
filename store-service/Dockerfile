FROM eclipse-temurin:21-jdk-alpine
WORKDIR /blog
COPY build/libs/store-service.jar /blog/store-service.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/blog/store-service.jar"]