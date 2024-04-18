package org.systemli.keycloak;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;

import java.util.List;

@Slf4j
public class UserliApiUserClientSimpleHttp implements UserliApiUserClient {

        private final CloseableHttpClient httpClient;
        private final String realmDomain;
        private final String baseUrl;
        private final String apiToken;

        public UserliApiUserClientSimpleHttp(KeycloakSession session, ComponentModel model) {
                this.httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
                this.realmDomain = model.get(Constants.REALM_DOMAIN);
                this.baseUrl = model.get(Constants.BASE_URL);
                this.apiToken = model.get(Constants.API_TOKEN);
        }

        @Override
        @SneakyThrows
        public List<UserliApiUser> getUsers(String search, int first, int max) {
                String url = String.format("%s/api/keycloak", baseUrl);
                SimpleHttp simpleHttp = SimpleHttp.doGet(url, httpClient)
                        .auth(apiToken)
                        .param("domain", realmDomain)
                        .param("first", String.valueOf(first))
                        .param("max", String.valueOf(max));
                if (search != null) {
                        simpleHttp.param("search", search);
                }
                return simpleHttp.asJson(new TypeReference<>() {});
        }

        @Override
        @SneakyThrows
        public Integer getUsersCount() {
                String url = String.format("%s/api/keycloak/count", baseUrl);
                String count = SimpleHttp.doGet(url, httpClient)
                        .auth(apiToken)
                        .param("domain", realmDomain)
                        .asString();
                return Integer.valueOf(count);
        }

        @Override
        @SneakyThrows
        public UserliApiUser getUserById(String id) {
                String url = String.format("%s/api/keycloak/user/%s", baseUrl, id);
                SimpleHttp.Response response = SimpleHttp.doGet(url, httpClient)
                        .auth(apiToken)
                        .param("domain", realmDomain)
                        .asResponse();
                if (response.getStatus() == 404) {
                        throw new WebApplicationException(response.getStatus());
                }
                return response.asJson(UserliApiUser.class);
        }

        @Override
        @SneakyThrows
        public Boolean validate(String email, String password) {
                log.warn("UserliApiUserClientSimpleHttp validate: User with email '{}' and password '{}' tries to login", email, password);
                String url = String.format("%s/api/keycloak/validate/%s", baseUrl, email);
                SimpleHttp.Response response = SimpleHttp.doPost(url, httpClient)
                        .auth(apiToken)
                        .param("domain", realmDomain)
                        .param("password", password)
                        .asResponse();
                if (response.getStatus() == 200) {
                        return true;
                }
                return false;
        }

}
