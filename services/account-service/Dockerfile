# Build context repo köküdür (docker-compose.yml'e bakın): servis, shared altındaki
# kontrat modülüne ve kökteki parent pom.xml'e ihtiyaç duyar.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /repo
COPY . .
# -pl ... -am: yalnızca bu modül ve bağımlı olduğu modüller derlenir.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl services/account-service -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /repo/services/account-service/target/account-service.jar app.jar
EXPOSE 8080 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
