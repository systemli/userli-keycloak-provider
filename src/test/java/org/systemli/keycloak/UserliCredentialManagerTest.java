package org.systemli.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.credential.dto.WebAuthnCredentialData;

class UserliCredentialManagerTest {

    private static final String EMAIL = "user@example.org";

    // Bytes 0xfb 0xff 0x01, which differ between base64url ("-_8B") and base64 ("+/8B")
    private static final String CREDENTIAL_ID = "-_8B";

    private final UserliHttpClient client = mock(UserliHttpClient.class);

    private final List<UserliWebauthnCredential> credentials = new ArrayList<>();

    private UserliUserAdapter user;

    private UserliCredentialManager manager;

    @BeforeEach
    void setUp() {
        KeycloakSession session = mock(KeycloakSession.class, RETURNS_DEEP_STUBS);
        RealmModel realm = mock(RealmModel.class);
        ComponentModel model = mock(ComponentModel.class);
        when(model.getId()).thenReturn("userli");

        UserliUser userliUser = new UserliUser();
        userliUser.setId("user");
        userliUser.setEmail(EMAIL);

        when(client.getWebauthnCredentials(EMAIL)).thenReturn(credentials);
        user = new UserliUserAdapter(session, realm, model, userliUser, client);
        manager = new UserliCredentialManager(session, realm, user);
    }

    @Test
    void toModelConvertsTheKeyFromUserli() {
        WebAuthnCredentialModel model = UserliCredentialManager.toModel(credential(7, 3));
        WebAuthnCredentialData data = model.getWebAuthnCredentialData();

        assertEquals("userli-webauthn-7", model.getId());
        assertEquals(WebAuthnCredentialModel.TYPE_TWOFACTOR, model.getType());
        assertEquals("YubiKey", model.getUserLabel());
        assertEquals(1761948000000L, model.getCreatedDate());
        assertEquals("+/8B", data.getCredentialId());
        assertEquals("pQECAyYgASFYIA", data.getCredentialPublicKey());
        assertEquals("00000000-0000-0000-0000-000000000000", data.getAaguid());
        assertEquals(3, data.getCounter());
        assertEquals(Set.of("usb"), data.getTransports());
    }

    @Test
    void getStoredCredentialByIdFindsKeyFromUserli() {
        credentials.add(credential(7, 3));

        CredentialModel model = manager.getStoredCredentialById("userli-webauthn-7");

        assertEquals("userli-webauthn-7", model.getId());
        assertNull(manager.getStoredCredentialById("userli-webauthn-8"));
    }

    @Test
    void keysAreLoadedOncePerUser() {
        credentials.add(credential(7, 3));

        manager.getStoredCredentialById("userli-webauthn-7");
        manager.getConfiguredUserStorageCredentialTypesStream().toList();

        verify(client, times(1)).getWebauthnCredentials(EMAIL);
    }

    @Test
    void updateStoredCredentialSendsSignCountToUserli() {
        UserliWebauthnCredential credential = credential(7, 3);
        credentials.add(credential);
        when(client.updateWebauthnSignCount(EMAIL, 7, 4)).thenReturn(true);

        WebAuthnCredentialModel model = UserliCredentialManager.toModel(credential);
        model.updateCounter(4);
        manager.updateStoredCredential(model);

        verify(client).updateWebauthnSignCount(EMAIL, 7, 4);
        assertEquals(4, credential.getSignCount());
    }

    @Test
    void updateStoredCredentialKeepsSignCountRefusedByUserli() {
        UserliWebauthnCredential credential = credential(7, 3);
        credentials.add(credential);
        when(client.updateWebauthnSignCount(EMAIL, 7, 4)).thenReturn(false);

        WebAuthnCredentialModel model = UserliCredentialManager.toModel(credential);
        model.updateCounter(4);
        manager.updateStoredCredential(model);

        assertEquals(3, credential.getSignCount());
    }

    @Test
    void keysFromUserliCannotBeRemovedOrRenamed() {
        credentials.add(credential(7, 3));

        assertFalse(manager.removeStoredCredentialById("userli-webauthn-7"));
        manager.updateCredentialLabel("userli-webauthn-7", "Other");

        assertEquals("YubiKey", manager.getStoredCredentialById("userli-webauthn-7").getUserLabel());
        verify(client, never()).updateWebauthnSignCount(anyString(), anyInt(), anyLong());
    }

    @Test
    void webauthnIsConfiguredForUsersWithKeys() {
        assertFalse(manager.getConfiguredUserStorageCredentialTypesStream().toList().contains(WebAuthnCredentialModel.TYPE_TWOFACTOR));

        // The adapter keeps the list from Userli, so the new key shows up
        credentials.add(credential(7, 3));

        assertTrue(manager.getConfiguredUserStorageCredentialTypesStream().toList().contains(WebAuthnCredentialModel.TYPE_TWOFACTOR));
    }

    private static UserliWebauthnCredential credential(int id, long signCount) {
        UserliWebauthnCredential credential = new UserliWebauthnCredential();
        credential.setId(id);
        credential.setName("YubiKey");
        credential.setCredentialId(CREDENTIAL_ID);
        credential.setPublicKey("pQECAyYgASFYIA");
        credential.setAaguid("00000000-0000-0000-0000-000000000000");
        credential.setAttestationType("none");
        credential.setSignCount(signCount);
        credential.setTransports(List.of("usb"));
        credential.setCreatedAt("2025-10-31T23:00:00+01:00");

        return credential;
    }
}
