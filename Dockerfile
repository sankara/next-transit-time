FROM java:8-alpine
MAINTAINER Sankara Rameswaran

ADD target/uberjar/next-transit.jar /next-transit/app.jar

EXPOSE 3000

CMD ["java", "-jar", "-Duser.timezone=America/Los_Angeles", "-Dconf=/next-transit/prod-config.edn", "/next-transit/app.jar"]
