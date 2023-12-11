FROM maven:3-amazoncorretto-21-debian-bookworm AS provider

COPY src/ /opt/keycloak-provider/src
COPY pom.xml /opt/keycloak-provider/pom.xml

WORKDIR /opt/keycloak-provider

RUN mvn clean package

FROM quay.io/keycloak/keycloak:23.0 AS builder

WORKDIR /opt/keycloak

ENV KC_DB=mariadb

# Add custom storage provider
COPY --chown=keycloak:keycloak --from=provider /opt/keycloak-provider/target/*.jar /opt/keycloak/providers/

RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:23.0
COPY --from=builder /opt/keycloak/ /opt/keycloak/

ENV KC_HOSTNAME=localhost
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
