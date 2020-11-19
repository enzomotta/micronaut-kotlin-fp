FROM openjdk:14-alpine
COPY target/micronaut-kotlin-fp-*.jar micronaut-kotlin-fp.jar
EXPOSE 8080
CMD ["java", "-Dcom.sun.management.jmxremote", "-Xmx128m", "-jar", "micronaut-kotlin-fp.jar"]