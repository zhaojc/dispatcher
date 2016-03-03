FROM java:7
COPY target/dispatcher-SNAPSHOT-jar-with-dependencies.jar /bin/dispatcher.jar
CMD ["java", "-jar", "/bin/dispatcher.jar"]