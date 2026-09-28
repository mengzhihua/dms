FROM maven:3.9-eclipse-temurin-8 AS build
WORKDIR /app
COPY backend/pom.xml .
COPY deploy/maven-settings.xml /opt/maven-settings.xml
RUN --mount=type=cache,target=/root/.m2 mvn -q -s /opt/maven-settings.xml dependency:go-offline || true
COPY backend/src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -s /opt/maven-settings.xml package -Dmaven.test.skip=true

FROM eclipse-temurin:8-jre
# 报表 PDF 中文渲染需要 CJK 字体
RUN apt-get update -qq && apt-get install -y -qq fonts-noto-cjk curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV JAVA_OPTS="-Xms256m -Xmx768m" \
    SPRING_PROFILES_ACTIVE=prod,mysql
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=10 \
  CMD curl -fsS http://localhost:8080/api/auth/captcha/required || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
