package org.systemli.keycloak;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;

class UserliBackupCodeAuthenticatorTest {

    private static final String EMAIL = "user@example.org";

    private final UserliHttpClient client = mock(UserliHttpClient.class);

    private final AuthenticationFlowContext context = mock(AuthenticationFlowContext.class);

    private final LoginFormsProvider form = mock(LoginFormsProvider.class, RETURNS_SELF);

    private final Response response = mock(Response.class);

    private final MultivaluedHashMap<String, String> params = new MultivaluedHashMap<>();

    private final UserliBackupCodeAuthenticator authenticator = new UserliBackupCodeAuthenticator((session, realm, user) -> client);

    @BeforeEach
    void setUp() {
        UserModel user = mock(UserModel.class);
        when(user.getEmail()).thenReturn(EMAIL);
        when(context.getUser()).thenReturn(user);

        HttpRequest request = mock(HttpRequest.class);
        when(request.getDecodedFormParameters()).thenReturn(params);
        when(context.getHttpRequest()).thenReturn(request);

        when(context.form()).thenReturn(form);
        when(form.createForm("userli-backup-code.ftl")).thenReturn(response);
    }

    @Test
    void authenticateSkipsUsersWithoutBackupCodes() {
        when(client.isConfiguredFor(EMAIL, "backup-code")).thenReturn(false);

        authenticator.authenticate(context);

        verify(context).attempted();
        verify(context, never()).challenge(any());
    }

    @Test
    void authenticateShowsTheForm() {
        when(client.isConfiguredFor(EMAIL, "backup-code")).thenReturn(true);

        authenticator.authenticate(context);

        verify(context).challenge(response);
    }

    @Test
    void actionSucceedsWithAValidCode() {
        params.putSingle("backupCode", " 111111 ");
        when(client.validate(EMAIL, "111111", "backup-code")).thenReturn(true);

        authenticator.action(context);

        verify(context).success();
    }

    @Test
    void actionFailsWithAnInvalidCode() {
        params.putSingle("backupCode", "000000");
        when(client.validate(EMAIL, "000000", "backup-code")).thenReturn(false);

        authenticator.action(context);

        verify(form).addError(any(FormMessage.class));
        verify(context).failureChallenge(eq(AuthenticationFlowError.INVALID_CREDENTIALS), eq(response));
        verify(context, never()).success();
    }

    @Test
    void actionFailsWithoutCode() {
        authenticator.action(context);

        verify(client, never()).validate(anyString(), anyString(), anyString());
        verify(context).failureChallenge(eq(AuthenticationFlowError.INVALID_CREDENTIALS), eq(response));
    }
}
