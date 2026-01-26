FROM gradle:jdk25-ubi AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle clean build -x test
FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar userService.jar
ENTRYPOINT ["java", "-jar", "userService.jar"]
