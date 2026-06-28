# syntax=docker/dockerfile:1.7

# The build stage creates the Spring Boot jar inside Docker so the image does not depend on local target files.
FROM public.ecr.aws/amazoncorretto/amazoncorretto:25-al2023 AS build

WORKDIR /workspace

RUN dnf update -y \
    && dnf install -y unzip tar gzip \
    && dnf clean all

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/

RUN --mount=type=secret,id=maven_settings,target=/tmp/maven-settings.xml,required=true \
    --mount=type=secret,id=codeartifact_token,target=/tmp/codeartifact-token,required=true \
    chmod +x ./mvnw \
    && CODEARTIFACT_AUTH_TOKEN="$(cat /tmp/codeartifact-token)" ./mvnw --settings /tmp/maven-settings.xml -DskipTests package

# The runtime stage keeps only the Corretto runtime and the built WorkOps web application jar.
FROM public.ecr.aws/amazonlinux/amazonlinux:2023-minimal

WORKDIR /app

RUN microdnf update -y \
    && microdnf install -y java-25-amazon-corretto-headless \
    && microdnf clean all

COPY --from=build /workspace/target/workops-web-0.0.1-SNAPSHOT.jar /app/workops-web.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/workops-web.jar"]
