FROM eclipse-temurin:21-jdk-alpine
WORKDIR /payment
COPY payment-service/build/libs/payment-service.jar /payment/payment-service.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/payment/payment-service.jar"]