package org.systemli.keycloak;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

import java.util.List;
import java.util.Map;

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

    /**
     * Security keys of the user. Fails if Userli doesn't answer, so a login doesn't skip the second factor.
     */
    @SneakyThrows
    public List<UserliWebauthnCredential> getWebauthnCredentials(String email) {
        String url = String.format("%s/api/keycloak/%s/credentials/webauthn/%s", baseUrl, realmDomain, email);
        SimpleHttp.Response response = SimpleHttp.doGet(url, session)
            .auth(keycloakApiToken)
            .asResponse();
        if (response.getStatus() != 200) {
            throw new IllegalStateException("Userli returned status " + response.getStatus() + " for the security keys");
        }
        return response.asJson(new TypeReference<>() {
        });
    }

    /**
     * Stores the sign count after Keycloak verified an assertion. False if Userli refuses it, e.g. because it does not increase.
     */
    @SneakyThrows
    public Boolean updateWebauthnSignCount(String email, int id, long signCount) {
        String url = String.format("%s/api/keycloak/%s/credentials/webauthn/%s/%d", baseUrl, realmDomain, email, id);
        return SimpleHttp.doPut(url, session)
            .auth(keycloakApiToken)
            .json(Map.of("signCount", signCount))
            .asResponse().getStatus() == 200;
    }

    @SneakyThrows
    public Boolean isConfiguredFor(String email, String credentialType) {
        String url = String.format("%s/api/keycloak/%s/configured/%s/%s", baseUrl, realmDomain, credentialType, email);
        return SimpleHttp.doGet(url, session)
            .auth(keycloakApiToken)
            .asResponse().getStatus() == 200;
    }

}
