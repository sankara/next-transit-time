FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/next-transit.jar /next-transit/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/next-transit/app.jar"]
