FROM java:8-alpine
MAINTAINER Sankara Rameswaran

ENV TZ=America/Los_Angeles
RUN apk add --update tzdata

ADD target/uberjar/next-transit.jar /next-transit/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/next-transit/app.jar"]
