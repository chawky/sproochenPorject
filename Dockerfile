# this is the code that would create the build of the app .jar
# Use Maven + Java 17 to build the application
FROM maven:3.9.4-eclipse-temurin-17 AS build

# Create /app and work from there
WORKDIR /app

# Copy the Maven configuration
COPY pom.xml /app

# Copy the Java source code
COPY src /app/src

# Build the application and create the JAR
RUN mvn clean package -DskipTests

# Runtime stage: only Java is needed here
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the JAR from the first stage into this stage
# Copy from the Docker stage that we named .build
# Now we have  the jar
COPY --from=build /app/target/*.jar app.jar
# EXPOSE 8080 does not actually publish the port to Windows.
# It documents that the container expects to listen on port 8080.
EXPOSE 8080
# tell Docker what command to execute when the container starts
# Use EntryPoint
ENTRYPOINT ["java", "-jar", "app.jar"]