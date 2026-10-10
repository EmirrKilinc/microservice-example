# Build context repo köküdür.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /repo
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl api-gateway -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /repo/api-gateway/target/api-gateway.jar app.jar
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "app.jar"]
