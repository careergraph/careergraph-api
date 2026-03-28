# Stage 1: Build Spring Boot
FROM maven:3.9.9-amazoncorretto-21 AS build

WORKDIR /app

# Copy pom trước để cache dependency
COPY pom.xml .

# Tải dependency trước (Docker cache layer này)
RUN mvn -B -q -e -DskipTests dependency:go-offline

# Copy source code
COPY src ./src

# Build jar
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM amazoncorretto:21-alpine

WORKDIR /app

# Copy jar từ build stage
COPY --from=build /app/target/*.jar app.jar

# Port
ENV PORT=8080
EXPOSE 8080

# Java memory config
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:MaxMetaspaceSize=128m"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar /app/app.jar"]