package org.systemli.keycloak;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

import java.util.List;

public class UserliHttpClient {

    private final KeycloakSession session;
    private final String realmDomain;
    private final String baseUrl;
    private final String keycloakApiToken;

    public UserliHttpClient(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.realmDomain = model.get(Constants.REALM_DOMAIN);
        this.baseUrl = model.get(Constants.BASE_URL);
        this.keycloakApiToken = model.get(Constants.API_TOKEN);
    }

    @SneakyThrows
    public List<UserliUser> getUsers(String search, int first, int max) {
        String url = String.format("%s/api/keycloak/%s", baseUrl, realmDomain);
        SimpleHttp simpleHttp = SimpleHttp.doGet(url, session)
            .auth(keycloakApiToken)
            .param("first", String.valueOf(first))
            .param("max", String.valueOf(max));
        if (search != null) {
            simpleHttp.param("search", search);
        }
        return simpleHttp.asJson(new TypeReference<>() {
        });
    }

    @SneakyThrows
    public Integer getUsersCount() {
        String url = String.format("%s/api/keycloak/%s/count", baseUrl, realmDomain);
        String count = SimpleHttp.doGet(url, session)
            .auth(keycloakApiToken)
            .asString();
        return Integer.valueOf(count);
    }

    @SneakyThrows
    public UserliUser getUserById(String id) {
        String url = String.format("%s/api/keycloak/%s/user/%s", baseUrl, realmDomain, id);
        SimpleHttp.Response response = SimpleHttp.doGet(url, session)
            .auth(keycloakApiToken)
            .asResponse();
        if (response.getStatus() == 404) {
            throw new UserNotFoundException();
        }
        return response.asJson(UserliUser.class);
    }

    @SneakyThrows
    public Boolean validate(String email, String password, String credentialType) {
        String url = String.format("%s/api/keycloak/%s/validate/%s", baseUrl, realmDomain, email);
        SimpleHttp.Response response = SimpleHttp.doPost(url, session)
            .auth(keycloakApiToken)
            .param("credentialType", credentialType)
            .param("password", password)
            .asResponse();
        return response.getStatus() == 200;
    }

    @SneakyThrows
    public List<UserliWebauthnCredential> getWebauthnCredentials(String email) {
        String url = String.format("%s/api/keycloak/%s/credentials/webauthn/%s", baseUrl, realmDomain, email);
        SimpleHttp.Response response = SimpleHttp.doGet(url, session)
            .auth(keycloakApiToken)
            .asResponse();
        if (response.getStatus() != 200) {
            return List.of();
        }
        return response.asJson(new TypeReference<>() {
        });
    }

    /**
     * Userli verifies the WebAuthn assertion against the stored public key. All values are base64url encoded.
     */
    @SneakyThrows
    public Boolean validateWebauthn(String email, String challenge, String credentialId, String clientDataJSON,
                                    String authenticatorData, String signature, String userHandle) {
        String url = String.format("%s/api/keycloak/%s/validate/webauthn/%s", baseUrl, realmDomain, email);
        SimpleHttp simpleHttp = SimpleHttp.doPost(url, session)
            .auth(keycloakApiToken)
            .param("challenge", challenge)
            .param("credentialId", credentialId)
            .param("clientDataJSON", clientDataJSON)
            .param("authenticatorData", authenticatorData)
            .param("signature", signature);
        if (userHandle != null && !userHandle.isEmpty()) {
            simpleHttp.param("userHandle", userHandle);
        }
        return simpleHttp.asResponse().getStatus() == 200;
    }

    @SneakyThrows
    public Boolean isConfiguredFor(String email, String credentialType) {
        String url = String.format("%s/api/keycloak/%s/configured/%s/%s", baseUrl, realmDomain, credentialType, email);
        return SimpleHttp.doGet(url, session)
            .auth(keycloakApiToken)
            .asResponse().getStatus() == 200;
    }

}
