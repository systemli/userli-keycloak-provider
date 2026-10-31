package org.systemli.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.WebAuthnPolicy;
import org.keycloak.sessions.AuthenticationSessionModel;

class UserliWebauthnAuthenticatorTest {

    private static final String EMAIL = "user@example.org";

    private final UserliHttpClient client = mock(UserliHttpClient.class);

    private final AuthenticationFlowContext context = mock(AuthenticationFlowContext.class);

    private final LoginFormsProvider form = mock(LoginFormsProvider.class, RETURNS_SELF);

    private final Response response = mock(Response.class);

    private final Map<String, String> notes = new HashMap<>();

    private final MultivaluedHashMap<String, String> params = new MultivaluedHashMap<>();

    private final UserliWebauthnAuthenticator authenticator = new UserliWebauthnAuthenticator((session, realm, user) -> client);

    @BeforeEach
    void setUp() {
        UserModel user = mock(UserModel.class);
        when(user.getEmail()).thenReturn(EMAIL);
        when(context.getUser()).thenReturn(user);
        when(context.getSession()).thenReturn(mock(KeycloakSession.class));

        WebAuthnPolicy policy = mock(WebAuthnPolicy.class);
        when(policy.getRpId()).thenReturn("example.org");
        when(policy.getUserVerificationRequirement()).thenReturn("discouraged");
        RealmModel realm = mock(RealmModel.class);
        when(realm.getWebAuthnPolicy()).thenReturn(policy);
        when(context.getRealm()).thenReturn(realm);

        AuthenticationSessionModel authSession = mock(AuthenticationSessionModel.class);
        when(authSession.getAuthNote(anyString())).thenAnswer(invocation -> notes.get(invocation.<String>getArgument(0)));
        org.mockito.Mockito.doAnswer(invocation -> notes.put(invocation.getArgument(0), invocation.getArgument(1)))
            .when(authSession).setAuthNote(anyString(), anyString());
        org.mockito.Mockito.doAnswer(invocation -> notes.remove(invocation.<String>getArgument(0)))
            .when(authSession).removeAuthNote(anyString());
        when(context.getAuthenticationSession()).thenReturn(authSession);

        HttpRequest request = mock(HttpRequest.class);
        when(request.getDecodedFormParameters()).thenReturn(params);
        when(context.getHttpRequest()).thenReturn(request);

        when(context.form()).thenReturn(form);
        when(form.createLoginWebAuthn()).thenReturn(response);
        when(client.createWebauthnChallenge(EMAIL)).thenReturn("Y2hhbGxlbmdl");
    }

    @Test
    void authenticateSkipsUsersWithoutSecurityKeys() {
        when(client.isConfiguredFor(EMAIL, "webauthn")).thenReturn(false);

        authenticator.authenticate(context);

        verify(context).attempted();
        verify(context, never()).challenge(any());
    }

    @Test
    void authenticateRendersTheKeysOfTheUserWithAChallengeFromUserli() {
        UserliWebauthnCredential credential = new UserliWebauthnCredential();
        credential.setCredentialId("Y3JlZA");
        when(client.isConfiguredFor(EMAIL, "webauthn")).thenReturn(true);
        when(client.getWebauthnCredentials(EMAIL)).thenReturn(List.of(credential));

        authenticator.authenticate(context);

        assertEquals("Y2hhbGxlbmdl", notes.get(UserliWebauthnAuthenticator.CHALLENGE_NOTE));
        verify(form).setAttribute("challenge", "Y2hhbGxlbmdl");
        verify(form).setAttribute("rpId", "example.org");
        verify(form).setAttribute(eq("authenticators"), any(UserliWebauthnAuthenticator.AuthenticatorsBean.class));
        verify(context).challenge(response);
    }

    @Test
    void authenticateKeepsNoChallengeWhenUserliRefusesOne() {
        notes.put(UserliWebauthnAuthenticator.CHALLENGE_NOTE, "old");
        when(client.isConfiguredFor(EMAIL, "webauthn")).thenReturn(true);
        when(client.getWebauthnCredentials(EMAIL)).thenReturn(List.of());
        when(client.createWebauthnChallenge(EMAIL)).thenReturn(null);

        authenticator.authenticate(context);

        assertFalse(notes.containsKey(UserliWebauthnAuthenticator.CHALLENGE_NOTE));
        verify(form).setAttribute("challenge", "");
    }

    @Test
    void actionSucceedsWhenUserliVerifiesTheAssertion() {
        notes.put(UserliWebauthnAuthenticator.CHALLENGE_NOTE, "challenge");
        putAssertion();
        when(client.validateWebauthn(EMAIL, "challenge", "Y3JlZA", "Y2xpZW50", "YXV0aA", "c2ln", "aGFuZGxl")).thenReturn(true);

        authenticator.action(context);

        verify(context).success();
        assertFalse(notes.containsKey(UserliWebauthnAuthenticator.CHALLENGE_NOTE));
    }

    @Test
    void actionFailsWhenUserliRejectsTheAssertion() {
        notes.put(UserliWebauthnAuthenticator.CHALLENGE_NOTE, "challenge");
        putAssertion();
        when(client.validateWebauthn(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(false);

        authenticator.action(context);

        verify(context).failureChallenge(eq(AuthenticationFlowError.INVALID_CREDENTIALS), any());
        verify(context, never()).success();
    }

    @Test
    void actionFailsWithoutChallenge() {
        putAssertion();

        authenticator.action(context);

        verify(client, never()).validateWebauthn(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(context).failureChallenge(eq(AuthenticationFlowError.INVALID_CREDENTIALS), any());
    }

    @Test
    void actionShowsTheFormAgainWhenTheBrowserFailed() {
        notes.put(UserliWebauthnAuthenticator.CHALLENGE_NOTE, "challenge");
        params.putSingle("error", "NotAllowedError");

        authenticator.action(context);

        verify(form).setError("webauthn-error-api-get", "");
        verify(context).challenge(response);
        verify(client, never()).validateWebauthn(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void configuredForIsFalseForUsersOfOtherProviders() {
        UserliWebauthnAuthenticator other = new UserliWebauthnAuthenticator((session, realm, user) -> null);

        assertFalse(other.configuredFor(context.getSession(), context.getRealm(), context.getUser()));
    }

    private void putAssertion() {
        params.putSingle("credentialId", "Y3JlZA");
        params.putSingle("clientDataJSON", "Y2xpZW50");
        params.putSingle("authenticatorData", "YXV0aA");
        params.putSingle("signature", "c2ln");
        params.putSingle("userHandle", "aGFuZGxl");
    }
}
