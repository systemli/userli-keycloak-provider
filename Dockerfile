FROM maven:3-amazoncorretto-21-debian-bookworm AS provider

COPY src/ /opt/keycloak-provider/src
COPY pom.xml /opt/keycloak-provider/pom.xml

WORKDIR /opt/keycloak-provider

RUN mvn clean package -DskipTests

FROM quay.io/keycloak/keycloak:23.0
COPY --chown=keycloak:keycloak --from=provider /opt/keycloak-provider/target/*.jar /opt/keycloak/providers/

ENV KC_HOSTNAME=localhost
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
