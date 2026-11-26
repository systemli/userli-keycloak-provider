package org.systemli.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

class UserliUserStorageProviderTest {

    private final UserliHttpClient client = mock(UserliHttpClient.class);
    private final RealmModel realm = mock(RealmModel.class);
    private final UserliUserStorageProvider provider;

    UserliUserStorageProviderTest() {
        ComponentModel model = new ComponentModel();
        model.setId("userli");
        provider = new UserliUserStorageProvider(mock(KeycloakSession.class), model, client);
    }

    @Test
    void unknownUserIsNotFound() throws Exception {
        when(client.getUserById("unknown@example.org")).thenThrow(new UserNotFoundException());

        assertNull(provider.getUserByUsername(realm, "unknown@example.org"));
        assertNull(provider.getUserByEmail(realm, "unknown@example.org"));
    }

    @Test
    void userliErrorIsNotFound() throws Exception {
        when(client.getUserById("user@example.org")).thenThrow(WebApplicationException.class);

        assertNull(provider.getUserByUsername(realm, "user@example.org"));
    }

    @Test
    void knownUserIsLoadedOnce() throws Exception {
        UserliUser userliUser = new UserliUser();
        userliUser.setId("user@example.org");
        userliUser.setEmail("user@example.org");
        when(client.getUserById("user@example.org")).thenReturn(userliUser);

        UserModel user = provider.getUserByEmail(realm, "user@example.org");

        assertEquals("user@example.org", user.getEmail());
        assertSame(user, provider.getUserByUsername(realm, "user@example.org"));
        verify(client, times(1)).getUserById("user@example.org");
    }
}
