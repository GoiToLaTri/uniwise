# ========================= GIAI ĐOẠN 1: BUILD =========================
# Dùng Maven image chính thức để build ứng dụng
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Đặt thư mục làm việc trong container build
WORKDIR /app

# Copy toàn bộ thư mục libs vào container
COPY platforms/java/libs /tmp/libs

# Install từng lib vào local Maven repository
# Cách 1: Nếu mỗi lib có pom.xml riêng
RUN for lib in /tmp/libs/*/; do \
        if [ -f "$lib/pom.xml" ]; then \
            cd $lib && mvn clean install -DskipTests; \
        fi \
    done

# Copy file pom.xml trước để cache layer dependencies (tối ưu tốc độ rebuild)
COPY platforms/java/workers/ffmpeg-worker/pom.xml .

# Tải toàn bộ dependencies về local repository
# Chỉ chạy lại khi pom.xml thay đổi
RUN mvn dependency:go-offline -B

# Copy toàn bộ source code vào container build
COPY platforms/java/workers/ffmpeg-worker/src ./src

# Build ứng dụng thành file JAR (skip test để nhanh hơn, tùy chọn)
RUN mvn clean package -DskipTests

# ========================= GIAI ĐOẠN 2: RUNTIME =========================
# Dùng JRE nhẹ (không có JDK, không có build tools)
# eclipse-temurin:21-jre-alpine rất nhẹ (~80MB)
FROM eclipse-temurin:21-jre-alpine AS runtime

# Cài đặt docker-cli vì worker cần gọi docker exec để chuyển tiếp công việc tới ffmpeg-service
RUN apk add --no-cache docker-cli

# Đặt thư mục làm việc
WORKDIR /app

# Copy file JAR đã build từ giai đoạn builder
COPY --from=builder /app/target/*.jar app.jar

# Lệnh chạy ứng dụng
ENTRYPOINT ["java", "-Xmx256m", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
