# Build context repo köküdür.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /repo
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl services/notification-service -am package -DskipTests

# Worker olduğu için port açılmaz.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /repo/services/notification-service/target/notification-service.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
