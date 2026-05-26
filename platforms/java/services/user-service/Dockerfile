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

# Hoặc cách 2: Nếu lib đã có sẵn file JAR (không có pom.xml)
# RUN for jar in /tmp/libs/*/*.jar; do \
#         mvn install:install-file \
#             -Dfile=$jar \
#             -DgroupId=com.uniwise \
#             -DartifactId=$(basename $jar .jar) \
#             -Dversion=0.0.1-SNAPSHOT \
#             -Dpackaging=jar; \
#     done

# Copy file pom.xml trước để cache layer dependencies (tối ưu tốc độ rebuild)
COPY platforms/java/services/user-service/pom.xml .

# Tải toàn bộ dependencies về local repository
# Chỉ chạy lại khi pom.xml thay đổi
RUN mvn dependency:go-offline -B

# Copy toàn bộ source code vào container build
COPY platforms/java/services/user-service/src ./src

# Build ứng dụng thành file JAR (skip test để nhanh hơn, tùy chọn)
RUN mvn clean package -DskipTests

# ========================= GIAI ĐOẠN 2: RUNTIME =========================
# Dùng JRE nhẹ (không có JDK, không có build tools)
# eclipse-temurin:21-jre-alpine rất nhẹ (~80MB)
FROM eclipse-temurin:21-jre-alpine AS runtime

# Tạo user không có root để chạy app (bảo mật)
RUN addgroup -S spring && adduser -S spring -G spring

# Đặt thư mục làm việc
WORKDIR /app

# Copy file JAR đã build từ giai đoạn builder
# Dùng --chown để đảm bảo quyền sở hữu thuộc user spring
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

# Chuyển sang user spring (không chạy root)
USER spring:spring

# Expose port (mặc định Spring Boot là 8080, có thể thay đổi)
# EXPOSE 8000

# Health check (tùy chọn, giúp orchestration như Docker Compose/K8s kiểm tra)
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Lệnh chạy ứng dụng
# - Xmx: giới hạn heap memory (tùy chỉnh theo server)
# - Djava.security.egd: tăng tốc khởi tạo random
ENTRYPOINT ["java", "-Xmx256m", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]