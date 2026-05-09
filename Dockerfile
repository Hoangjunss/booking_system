FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy toàn bộ thư mục src (bên ngoài) vào container
# Trong thư mục src của host có chứa pom.xml và thư mục src con
COPY src/ /app/

# Kiểm tra (có thể bỏ dòng này sau khi chạy thành công)
RUN ls -la /app/

# Build ứng dụng
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]