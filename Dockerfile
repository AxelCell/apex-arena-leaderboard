# ---- Stage 1: build the jar with the JDK and Maven wrapper
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src

# Copy only the build files first: this layer (all dependencies) is cached
# and only re-downloaded when pom.xml changes, not on every code change.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q package -DskipTests \
 && java -Djarmode=tools -jar target/leaderboard-0.0.1-SNAPSHOT.jar extract --layers --launcher --destination target/extracted

# ---- Stage 2: small runtime image with only the JRE and the app
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 app
WORKDIR /app

# Spring Boot layers, least to most frequently changed, so code changes
# only rebuild the last thin layer.
COPY --from=build /src/target/extracted/dependencies/ ./
COPY --from=build /src/target/extracted/spring-boot-loader/ ./
COPY --from=build /src/target/extracted/snapshot-dependencies/ ./
COPY --from=build /src/target/extracted/application/ ./

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
