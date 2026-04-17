# Stage 1: Build Spring Boot
FROM maven:3.9.9-amazoncorretto-21 AS build

WORKDIR /app

# Copy pom trước để cache dependency
COPY pom.xml .

# Tải dependency trước (Docker cache layer này)
RUN mvn -B -q -e -DskipTests dependency:go-offline

# Copy source code
COPY src ./src
COPY init-scripts ./init-scripts

# Build jar
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM amazoncorretto:21-alpine

WORKDIR /app

# LibreOffice headless is required for DOC/DOCX -> PDF conversion.
RUN apk add --no-cache libreoffice ttf-dejavu

# Copy jar từ build stage
COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/init-scripts ./init-scripts

# Port
ENV PORT=8080
EXPOSE 8080

# Java memory config
ENV JAVA_OPTS="-Xmx768m -Xms256m -XX:MaxMetaspaceSize=256m"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar /app/app.jar"]