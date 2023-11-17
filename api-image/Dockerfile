# Using Amazon Corretto image (20)
FROM amazoncorretto:20

RUN mkdir /app

# Define the working directory
WORKDIR /app

# Copy the jar file to the container
COPY seasonsforce-ms-notification-api-1.0-SNAPSHOT.jar /app/app.jar

# Expose the port 8080
EXPOSE 8080

# Start the application
CMD ["java", "-jar", "app.jar"]