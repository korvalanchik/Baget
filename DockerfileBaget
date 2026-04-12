# ---------- BUILD STAGE ----------
FROM gradle:8.7-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle build -x test

# ---------- RUN STAGE ----------
FROM eclipse-temurin:17-jdk
WORKDIR /app

# копіюємо jar
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]