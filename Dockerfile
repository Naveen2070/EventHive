# --- Stage 1: Build the Application ---
FROM gradle:8.14.3-jdk21-alpine AS builder
WORKDIR /app

# 1. Copy only dependency files first
# This allows Docker to cache dependencies if these files haven't changed
COPY build.gradle.kts settings.gradle.kts ./

# 2. Copy source code
COPY src ./src

# 3. Build the WAR
# "--no-daemon" is crucial for Docker to save memory
# "-x test" skips tests (since you likely ran them in CI/CD already)
RUN gradle bootWar -x test --no-daemon

# --- Stage 2: Create the Runtime Image ---
# Use a lightweight Alpine Linux with JRE 21
FROM eclipse-temurin:21-jdk-alpine-3.23
WORKDIR /app

# 4. Create a non-root user (Security Best Practice)
# Running as root is a security risk; this creates a user named 'eventhive'
RUN addgroup -S eventhive && adduser -S eventhive -G eventhive
USER eventhive:eventhive

# 5. Copy the built WAR from the 'builder' stage
# We rename it to 'app.war' so the ENTRYPOINT is simple
COPY --from=builder /app/build/libs/*.war app.war

# 6. Configuration
EXPOSE 8080

# 7. Run it
ENTRYPOINT ["java", "-jar", "app.war"]