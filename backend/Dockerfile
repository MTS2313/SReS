FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x mvnw
COPY backend/src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre

RUN useradd --system --create-home --home-dir /home/sres --shell /usr/sbin/nologin sres \
    && mkdir -p /app \
    && chown sres:sres /app
WORKDIR /app
COPY --from=build --chown=sres:sres /workspace/target/*.jar /app/sres-api.jar
USER sres
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/sres-api.jar"]
