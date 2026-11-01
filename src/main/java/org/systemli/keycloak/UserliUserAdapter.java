package org.systemli.keycloak;

import java.util.List;
import java.util.Map;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapter;

public class UserliUserAdapter extends AbstractUserAdapter {

    private final UserliUser user;
    private final UserliHttpClient client;
    private List<UserliWebauthnCredential> webauthnCredentials;

    public UserliUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel storageProviderModel,
                             UserliUser user, UserliHttpClient client) {
        super(session, realm, storageProviderModel);
        this.storageId = new StorageId(storageProviderModel.getId(), user.getId());
        this.user = user;
        this.client = client;
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public boolean isEmailVerified() {
        return true;
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
        attributes.add(UserModel.USERNAME, getUsername());
        attributes.add(UserModel.EMAIL, getEmail());

        return attributes;
    }

    @Override
    public SubjectCredentialManager credentialManager() {
        return new UserliCredentialManager(session, realm, this);
    }

    UserliHttpClient getClient() {
        return client;
    }

    /**
     * Security keys of the user, loaded once per request because a login asks for them several times.
     */
    List<UserliWebauthnCredential> getWebauthnCredentials() {
        if (webauthnCredentials == null) {
            webauthnCredentials = client.getWebauthnCredentials(getEmail());
        }

        return webauthnCredentials;
    }
}
