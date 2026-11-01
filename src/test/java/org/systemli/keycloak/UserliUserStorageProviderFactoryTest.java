package org.systemli.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.CacheableStorageProviderModel;

class UserliUserStorageProviderFactoryTest {

    private final UserliUserStorageProviderFactory factory = new UserliUserStorageProviderFactory();

    @Test
    void validateConfigurationTurnsOffTheUserCache() {
        ComponentModel config = new ComponentModel();
        config.put(Constants.REALM_DOMAIN, "example.org");
        config.put(Constants.BASE_URL, "http://userli");
        config.put(Constants.API_TOKEN, "token");
        config.put(CacheableStorageProviderModel.CACHE_POLICY, "DEFAULT");

        factory.validateConfiguration(mock(KeycloakSession.class), mock(RealmModel.class), config);

        assertEquals("NO_CACHE", config.get(CacheableStorageProviderModel.CACHE_POLICY));
    }

    @Test
    void validateConfigurationRequiresAllSettings() {
        ComponentModel config = new ComponentModel();
        config.put(Constants.REALM_DOMAIN, "example.org");

        assertThrows(ComponentValidationException.class,
            () -> factory.validateConfiguration(mock(KeycloakSession.class), mock(RealmModel.class), config));
    }
}
