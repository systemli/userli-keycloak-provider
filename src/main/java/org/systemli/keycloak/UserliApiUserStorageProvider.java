package org.systemli.keycloak;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;

import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

@Slf4j
public class UserliApiUserStorageProvider implements UserStorageProvider, UserLookupProvider, UserQueryProvider, CredentialInputValidator {

        private final KeycloakSession session;
        private final ComponentModel model;
        private final UserliApiUserClient client;

        protected Map<String, UserModel> loadedUsers = new HashMap<>();
        protected Properties properties = new Properties();

        public UserliApiUserStorageProvider(KeycloakSession session, ComponentModel model) {
                this.session = session;
                this.model = model;
                this.client = new UserliApiUserClientSimpleHttp(session, model);
        }

        @Override
        public void close() {
        }

        @Override
        public boolean supportsCredentialType(String credentialType) {
                return PasswordCredentialModel.TYPE.equals(credentialType);
        }

        @Override
        public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
                return supportsCredentialType(credentialType);
        }

        @Override
        public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
                if (!this.supportsCredentialType(credentialInput.getType()) || !(credentialInput instanceof UserCredentialModel)) {
                        return false;
                }

                String password = credentialInput.getChallengeResponse();
                if (password == null) {
                        return false;
                }

                return client.validate(user.getEmail(), password);
        }

        @Override
        public UserModel getUserById(RealmModel realm, String id) {
                return findUser(realm, StorageId.externalId(id));
        }

        @Override
        public UserModel getUserByUsername(RealmModel realm, String username) {
                return findUser(realm, username);
        }

        @Override
        public UserModel getUserByEmail(RealmModel realm, String email) {
                return findUser(realm, email);
        }

        private UserModel findUser(RealmModel realm, String identifier) {
                UserModel adapter = loadedUsers.get(identifier);
                if (adapter == null) {
                        try {
                                UserliApiUser user = client.getUserById(identifier);
                                adapter = new UserliApiUserAdapter(session, realm, model, user);
                                loadedUsers.put(identifier, adapter);
                        } catch (WebApplicationException e) {
                                log.warn("User with identifier '{}' could not be found, response from server: {}", identifier, e.getResponse().getStatus());
                        }
                }
                return adapter;
        }

        @Override
        public int getUsersCount(RealmModel real) {
                return client.getUsersCount();
        }

        @Override
        public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
                log.debug("searchForUserStream, params={}, first={}, max={}", params, firstResult, maxResults);
                String search = params.get(UserModel.SEARCH);
                return toUserModelStream(client.getUsers(search, firstResult, maxResults), realm);
        }

        private Stream<UserModel> toUserModelStream(List<UserliApiUser> users, RealmModel realm) {
                log.debug("Received {} users from provider", users.size());
                return users.stream().map(user -> new UserliApiUserAdapter(session, realm, model, user));
        }

        @Override
        public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
                // We don't support groups
                return Stream.empty();
        }

        @Override
        public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
                // We don't support user attributes
                return Stream.empty();
        }
}
