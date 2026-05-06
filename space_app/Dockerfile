# ============================================================
# Space Mission Control Backend — Dockerfile
# Java 21 · Spring Boot 3.x · Maven
# ============================================================

# ── Étape 1 : Build ─────────────────────────────────────────
FROM maven:3.9.11-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src src
RUN mvn package -DskipTests -B


# ── Étape 2 : Image finale ───────────────────────────────────
FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY --from=builder /app/target/*.jar space-mission-control.jar

EXPOSE 8080

CMD ["java", "-jar", "space-mission-control.jar"]
