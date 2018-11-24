FROM openjdk:8-jdk-alpine
ADD target/devops-exam-app-1.0-SNAPSHOT.jar devops-exam-app-1.0-SNAPSHOT.jar
CMD ["java", "-Xmx256M","-jar","devops-exam-app-1.0-SNAPSHOT.jar"]