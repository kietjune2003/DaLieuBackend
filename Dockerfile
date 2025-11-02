# ========================================
# Stage 1: Build ứng dụng với Maven
# ========================================
FROM maven:3.9.9-eclipse-temurin-21 AS build

# Đặt thư mục làm việc trong container
WORKDIR /app

# Copy file pom.xml và tải dependency trước (để tận dụng cache)
COPY pom.xml .

# Tải các dependency mà không cần build code (giúp tăng tốc build lần sau)
RUN mvn dependency:go-offline -B

# Copy toàn bộ mã nguồn vào container
COPY src ./src

# Build file jar (bỏ test nếu không cần)
RUN mvn clean package -DskipTests

# ========================================
# Stage 2: Tạo image chạy nhẹ hơn
# ========================================
FROM eclipse-temurin:21-jre

# Đặt thư mục làm việc
WORKDIR /app

# Copy file jar từ stage build
COPY --from=build /app/target/*.jar app.jar

# Cấu hình biến môi trường (tùy chỉnh nếu cần)
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS=""

# Cổng ứng dụng (phải trùng với server.port trong application.properties)
EXPOSE 8080

# Lệnh chạy ứng dụng
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
