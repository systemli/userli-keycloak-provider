package org.systemli.keycloak;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * Creates the client for the Userli instance a user is federated from, or null for other users.
 */
@FunctionalInterface
public interface UserliClientFactory {

    UserliHttpClient create(KeycloakSession session, RealmModel realm, UserModel user);
}
