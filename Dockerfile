FROM eclipse-temurin:21-jdk-alpine
WORKDIR /blog
COPY build/libs/online-store.jar /blog/online-store.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/blog/online-store.jar"]