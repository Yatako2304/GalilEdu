# Imagen del backend (Spring Boot). Se construye en dos etapas para que la imagen final
# solo tenga el JRE y el .jar, no Maven ni el código fuente.

# 1) Compilación
FROM eclipse-temurin:21-jdk AS compilacion
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml formato-java.prefs ./
# Descarga las dependencias en una capa aparte: si solo cambia el código, no se vuelven a bajar.
RUN sh mvnw -B -q dependency:go-offline
COPY src/ src/
RUN sh mvnw -B -q -DskipTests -Dspotless.check.skip=true package

# 2) Ejecución
FROM eclipse-temurin:21-jre
WORKDIR /app
# curl: lo usa el chequeo de salud de docker-compose.
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 galiledu
COPY --from=compilacion /app/target/galiledu.jar app.jar
USER galiledu
EXPOSE 8080
# Memoria acotada para instancias pequeñas (t3.small); se puede cambiar con JAVA_OPTS.
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
