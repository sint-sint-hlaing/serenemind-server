# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
# pom.xml နှင့် source code များကို copy ကူးပါ
COPY pom.xml .
COPY src ./src
# Project ကို Build လုပ်ပါ (Test များကို ကျော်ရန် -DskipTests သုံးပါသည်)
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
# Build လုပ်ထားသော jar ဖိုင်ကို ကူးယူပါ
COPY --from=build /app/target/*.jar app.jar
# Port ကို ဖွင့်ပါ
EXPOSE 8080
# Application ကို Run ပါ
ENTRYPOINT ["java", "-jar", "app.jar"]