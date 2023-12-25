FROM openjdk:17
EXPOSE 8089
WORKDIR /opt/app
VOLUME /data
COPY target/*.jar app.jar
CMD ["java", "-jar", "app.jar"]
