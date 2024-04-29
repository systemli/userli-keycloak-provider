package org.systemli.keycloak;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import org.apache.http.impl.client.CloseableHttpClient;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;

import java.util.List;

public class UserliHttpClient {

        private final CloseableHttpClient httpClient;
        private final String realmDomain;
        private final String baseUrl;
        private final String keycloakApiToken;

        public UserliHttpClient(KeycloakSession session, ComponentModel model) {
                this.httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
                this.realmDomain = model.get(Constants.REALM_DOMAIN);
                this.baseUrl = model.get(Constants.BASE_URL);
                this.keycloakApiToken = model.get(Constants.API_TOKEN);
        }

        @SneakyThrows
        public List<UserliUser> getUsers(String search, int first, int max) {
                String url = String.format("%s/api/keycloak/%s", baseUrl, realmDomain);
                SimpleHttp simpleHttp = SimpleHttp.doGet(url, httpClient)
                        .auth(keycloakApiToken)
                        .param("first", String.valueOf(first))
                        .param("max", String.valueOf(max));
                if (search != null) {
                        simpleHttp.param("search", search);
                }
                return simpleHttp.asJson(new TypeReference<>() {});
        }

        @SneakyThrows
        public Integer getUsersCount() {
                String url = String.format("%s/api/keycloak/%s/count", baseUrl, realmDomain);
                String count = SimpleHttp.doGet(url, httpClient)
                        .auth(keycloakApiToken)
                        .asString();
                return Integer.valueOf(count);
        }

        @SneakyThrows
        public UserliUser getUserById(String id) {
                String url = String.format("%s/api/keycloak/%s/user/%s", baseUrl, realmDomain, id);
                SimpleHttp.Response response = SimpleHttp.doGet(url, httpClient)
                        .auth(keycloakApiToken)
                        .asResponse();
                if (response.getStatus() == 404) {
                        throw new UserNotFoundException();
                }
                return response.asJson(UserliUser.class);
        }

        @SneakyThrows
        public Boolean validate(String email, String password) {
                String url = String.format("%s/api/keycloak/%s/validate/%s", baseUrl, realmDomain, email);
                SimpleHttp.Response response = SimpleHttp.doPost(url, httpClient)
                        .auth(keycloakApiToken)
                        .param("password", password)
                        .asResponse();
            return response.getStatus() == 200;
        }

}
