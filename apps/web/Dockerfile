# The build stage creates the Spring Boot jar inside Docker so the image does not depend on local target files.
FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/

RUN chmod +x ./mvnw && ./mvnw -DskipTests package

# The runtime stage keeps only the Java runtime and the built WorkOps web application jar.
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

RUN apk upgrade --no-cache

COPY --from=build /workspace/target/workops-web-0.0.1-SNAPSHOT.jar /app/workops-web.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/workops-web.jar"]
