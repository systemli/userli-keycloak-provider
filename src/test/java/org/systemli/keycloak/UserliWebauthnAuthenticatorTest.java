package org.systemli.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.WebAuthnConstants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.credential.CredentialInput;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.WebAuthnPolicy;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.sessions.AuthenticationSessionModel;

class UserliWebauthnAuthenticatorTest {

    private static final String USER_ID = "f:502936b6-00d4-4df2-9949-3beb4a40b791:user";

    private final AuthenticationFlowContext context = mock(AuthenticationFlowContext.class);

    private final HttpRequest request = mock(HttpRequest.class);

    private final UserModel user = mock(UserModel.class);

    private final MultivaluedHashMap<String, String> params = new MultivaluedHashMap<>();

    @BeforeEach
    void setUp() {
        params.putSingle(WebAuthnConstants.CREDENTIAL_ID, "credential");
        params.putSingle(WebAuthnConstants.SIGNATURE, "signature");
        params.putSingle(WebAuthnConstants.USER_HANDLE, "dXNlcmxpLWhhbmRsZQ");

        when(request.getDecodedFormParameters()).thenReturn(params);
        when(context.getHttpRequest()).thenReturn(request);
        when(context.getUser()).thenReturn(user);
    }

    @Test
    void withoutUserHandleDropsOnlyTheUserHandle() {
        MultivaluedMap<String, String> filtered = UserliWebauthnAuthenticator.withoutUserHandle(context)
            .getHttpRequest().getDecodedFormParameters();

        assertFalse(filtered.containsKey(WebAuthnConstants.USER_HANDLE));
        assertEquals("credential", filtered.getFirst(WebAuthnConstants.CREDENTIAL_ID));
        assertEquals("signature", filtered.getFirst(WebAuthnConstants.SIGNATURE));
    }

    @Test
    void withoutUserHandleKeepsTheRequestUnchanged() {
        UserliWebauthnAuthenticator.withoutUserHandle(context);

        assertEquals("dXNlcmxpLWhhbmRsZQ", params.getFirst(WebAuthnConstants.USER_HANDLE));
    }

    @Test
    void withoutUserHandleDelegatesEverythingElse() {
        AuthenticationFlowContext filtered = UserliWebauthnAuthenticator.withoutUserHandle(context);

        assertSame(user, filtered.getUser());

        filtered.success("webauthn");
        verify(context).success("webauthn");

        filtered.getHttpRequest().getHttpHeaders();
        verify(request).getHttpHeaders();
    }

    @Test
    void withoutUserHandlePassesExceptionsThrough() {
        IllegalStateException exception = new IllegalStateException("failed");
        when(context.getRealm()).thenThrow(exception);

        AuthenticationFlowContext filtered = UserliWebauthnAuthenticator.withoutUserHandle(context);

        assertSame(exception, assertThrows(IllegalStateException.class, filtered::getRealm));
    }

    /**
     * Passkeys send the user handle assigned by Userli, which is not the id of the federated user.
     */
    @Test
    void actionAcceptsPasskeysWithTheUserHandleOfUserli() {
        KeycloakSession session = mock(KeycloakSession.class, RETURNS_DEEP_STUBS);
        RealmModel realm = mock(RealmModel.class);
        WebAuthnPolicy policy = new WebAuthnPolicy();
        policy.setRpId("example.org");
        policy.setUserVerificationRequirement("not specified");
        when(realm.getWebAuthnPolicy()).thenReturn(policy);

        AuthenticationSessionModel authSession = mock(AuthenticationSessionModel.class);
        when(authSession.getAuthNote(WebAuthnConstants.AUTH_CHALLENGE_NOTE)).thenReturn("Y2hhbGxlbmdl");
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUri()).thenReturn(URI.create("https://id.example.org/"));

        SubjectCredentialManager credentials = mock(SubjectCredentialManager.class);
        when(credentials.isValid(any(CredentialInput.class))).thenReturn(true);
        when(user.getId()).thenReturn(USER_ID);
        when(user.credentialManager()).thenReturn(credentials);
        when(session.users().getUserById(realm, USER_ID)).thenReturn(user);

        params.putSingle(WebAuthnConstants.CREDENTIAL_ID, "Y3JlZGVudGlhbA");
        params.putSingle(WebAuthnConstants.CLIENT_DATA_JSON, "e30");
        params.putSingle(WebAuthnConstants.AUTHENTICATOR_DATA, "YXV0aA");
        params.putSingle(WebAuthnConstants.SIGNATURE, "c2ln");
        params.putSingle(WebAuthnConstants.USER_HANDLE, "IWlcfiDHuQOO");
        when(context.getRealm()).thenReturn(realm);
        when(context.getAuthenticationSession()).thenReturn(authSession);
        when(context.getUriInfo()).thenReturn(uriInfo);
        when(context.getEvent()).thenReturn(mock(EventBuilder.class, RETURNS_SELF));

        new UserliWebauthnAuthenticator(session).action(context);

        verify(context).success(WebAuthnCredentialModel.TYPE_TWOFACTOR);
        verify(context, never()).failure(any(AuthenticationFlowError.class), any());
    }

    @Test
    void factoryCreatesTheUserliAuthenticator() {
        UserliWebauthnAuthenticatorFactory factory = new UserliWebauthnAuthenticatorFactory();

        assertEquals("userli-webauthn-authenticator", factory.getId());
        assertInstanceOf(UserliWebauthnAuthenticator.class, factory.create(mock(KeycloakSession.class)));
    }
}
