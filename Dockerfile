FROM openjdk:17
WORKDIR /opt/app
VOLUME /data
EXPOSE 8089
COPY target/*.jar app.jar
CMD ["java", "-jar", "app.jar"]
