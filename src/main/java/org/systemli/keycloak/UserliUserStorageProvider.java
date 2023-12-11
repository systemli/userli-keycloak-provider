package org.systemli.keycloak;

import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;

import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class UserliUserStorageProvider
                implements UserStorageProvider, UserLookupProvider, CredentialInputValidator, UserQueryProvider {

        private final KeycloakSession session;
        private final ComponentModel model;
        private final PasswordVerifier passwordVerifier = new PasswordVerifier();
        private final UserliDomain domain;
        private final EntityManager entityManager;

        public UserliUserStorageProvider(KeycloakSession session, ComponentModel model) {
                this.session = session;
                this.model = model;
                this.entityManager = session.getProvider(JpaConnectionProvider.class, "userli").getEntityManager();
                this.domain = entityManager.createNamedQuery("findByName", UserliDomain.class)
                                .setParameter("name", model.get("domain")).getSingleResult();
        }

        @Override
        public void close() {
                entityManager.close();
        }

        @Override
        public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult,
                        Integer maxResults) {
                String search = params.get(UserModel.SEARCH);
                List<UserliUser> users = entityManager.createNamedQuery("findUsersByEmailAndDomain", UserliUser.class)
                                .setParameter("email", search + "%")
                                .setParameter("domain", domain)
                                .setMaxResults(maxResults)
                                .setFirstResult(firstResult)
                                .getResultList();

                return users.stream().map(user -> new UserliUserAdapter(session, realm, model, user));
        }

        @Override
        public int getUsersCount(RealmModel realm, boolean includeServiceAccount) {
                int count = entityManager.createNamedQuery("countUsersByDomain", Long.class)
                                .setParameter("domain", domain).getSingleResult().intValue();
                return count;
        }

        @Override
        public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult,
                        Integer maxResults) {
                // We don't support groups
                return Stream.empty();
        }

        @Override
        public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName,
                        String attrValue) {
                // We don't support user attributes
                return Stream.empty();
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
                if (!this.supportsCredentialType(credentialInput.getType())) {
                        return false;
                }

                UserliUser loadedUser = entityManager.createNamedQuery("findUserByEmailAndDomain", UserliUser.class)
                                .setParameter("email", user.getEmail()).setParameter("domain", domain)
                                .getSingleResult();

                if (loadedUser == null) {
                        return false;
                }

                return passwordVerifier.verify(credentialInput.getChallengeResponse(), loadedUser.getPassword());
        }

        @Override
        public UserModel getUserById(RealmModel realm, String id) {
                UserliUser user = entityManager.createNamedQuery("findUserByIdAndDomain", UserliUser.class)
                                .setParameter("id", StorageId.externalId(id))
                                .setParameter("domain", domain)
                                .getSingleResult();

                return new UserliUserAdapter(session, realm, model, user);
        }

        @Override
        public UserModel getUserByUsername(RealmModel realm, String username) {
                UserliUser user = entityManager.createNamedQuery("findUserByUsernameAndDomain", UserliUser.class)
                                .setParameter("username", username + "@" + domain.getName())
                                .setParameter("domain", domain)
                                .getSingleResult();

                return new UserliUserAdapter(session, realm, model, user);
        }

        @Override
        public UserModel getUserByEmail(RealmModel realm, String email) {
                UserliUser user = entityManager.createNamedQuery("findUserByEmailAndDomain", UserliUser.class)
                                .setParameter("email", email)
                                .setParameter("domain", domain)
                                .getSingleResult();

                return new UserliUserAdapter(session, realm, model, user);
        }
}
