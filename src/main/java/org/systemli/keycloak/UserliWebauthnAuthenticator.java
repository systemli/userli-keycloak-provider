package org.systemli.keycloak;

import java.util.List;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.component.ComponentModel;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.WebAuthnPolicy;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.utils.StringUtil;

/**
 * Second factor with WebAuthn security keys that are registered in Userli.
 * <p>
 * Userli creates a single-use challenge, Keycloak renders its WebAuthn login page with it and Userli
 * verifies the assertion against the stored public key. The realm WebAuthn policy sets the RP ID, which must match Userli.
 */
public class UserliWebauthnAuthenticator implements Authenticator {

    static final String CHALLENGE_NOTE = "USERLI_WEBAUTHN_CHALLENGE";

    private final UserliClientFactory clientFactory;

    public UserliWebauthnAuthenticator() {
        this(UserliWebauthnAuthenticator::createClient);
    }

    UserliWebauthnAuthenticator(UserliClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        // "Try another way" lists every alternative, so skip to the next one without security keys
        if (!configuredFor(context.getSession(), context.getRealm(), context.getUser())) {
            context.attempted();
            return;
        }

        context.challenge(createForm(context, null));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> params = context.getHttpRequest().getDecodedFormParameters();
        String challenge = context.getAuthenticationSession().getAuthNote(CHALLENGE_NOTE);
        context.getAuthenticationSession().removeAuthNote(CHALLENGE_NOTE);

        // Set by the login page when the browser call failed or was cancelled
        if (StringUtil.isNotBlank(params.getFirst("error"))) {
            context.challenge(createForm(context, "webauthn-error-api-get"));
            return;
        }

        UserliHttpClient client = clientFactory.create(context.getSession(), context.getRealm(), context.getUser());
        String credentialId = params.getFirst("credentialId");
        String clientDataJSON = params.getFirst("clientDataJSON");
        String authenticatorData = params.getFirst("authenticatorData");
        String signature = params.getFirst("signature");

        boolean valid = client != null
            && StringUtil.isNotBlank(challenge)
            && StringUtil.isNotBlank(credentialId)
            && StringUtil.isNotBlank(clientDataJSON)
            && StringUtil.isNotBlank(authenticatorData)
            && StringUtil.isNotBlank(signature)
            && client.validateWebauthn(context.getUser().getEmail(), challenge, credentialId, clientDataJSON,
                authenticatorData, signature, params.getFirst("userHandle"));

        if (!valid) {
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                createForm(context, "webauthn-error-auth-verification"));
            return;
        }

        context.success();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        UserliHttpClient client = clientFactory.create(session, realm, user);

        return client != null && client.isConfiguredFor(user.getEmail(), WebAuthnCredentialModel.TYPE_TWOFACTOR);
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // Security keys are registered in Userli
    }

    @Override
    public void close() {
    }

    /**
     * Renders the WebAuthn login page of the theme with a new challenge from Userli.
     */
    private Response createForm(AuthenticationFlowContext context, String error) {
        UserliHttpClient client = clientFactory.create(context.getSession(), context.getRealm(), context.getUser());
        String challenge = client == null ? null : client.createWebauthnChallenge(context.getUser().getEmail());
        if (StringUtil.isBlank(challenge)) {
            // The assertion is rejected in action() without a challenge
            challenge = "";
            context.getAuthenticationSession().removeAuthNote(CHALLENGE_NOTE);
        } else {
            context.getAuthenticationSession().setAuthNote(CHALLENGE_NOTE, challenge);
        }

        WebAuthnPolicy policy = context.getRealm().getWebAuthnPolicy();
        String rpId = policy.getRpId();
        if (StringUtil.isBlank(rpId)) {
            rpId = context.getUriInfo().getBaseUri().getHost();
        }

        List<AuthenticatorBean> authenticators = client == null ? List.of() : client.getWebauthnCredentials(context.getUser().getEmail())
            .stream()
            .map(credential -> new AuthenticatorBean(credential.getCredentialId()))
            .toList();

        LoginFormsProvider form = context.form()
            .setAttribute("challenge", challenge)
            .setAttribute("rpId", rpId)
            .setAttribute("userVerification", policy.getUserVerificationRequirement())
            .setAttribute("createTimeout", policy.getCreateTimeout())
            .setAttribute("isUserIdentified", "true")
            .setAttribute("shouldDisplayAuthenticators", false)
            .setAttribute("authenticators", new AuthenticatorsBean(authenticators));
        if (error != null) {
            // The messages have a placeholder for details, which Userli does not reveal
            form.setError(error, "");
        }

        return form.createLoginWebAuthn();
    }

    /**
     * Client for the Userli instance the user is federated from, or null for other users.
     */
    static UserliHttpClient createClient(KeycloakSession session, RealmModel realm, UserModel user) {
        if (user == null || user.getFederationLink() == null) {
            return null;
        }

        ComponentModel model = realm.getComponent(user.getFederationLink());
        if (model == null || !UserliUserStorageProviderFactory.PROVIDER_ID.equals(model.getProviderId())) {
            return null;
        }

        return new UserliHttpClient(session, model);
    }

    /**
     * Same shape as the bean Keycloak passes to webauthn-authenticate.ftl.
     */
    @Getter
    @RequiredArgsConstructor
    public static class AuthenticatorsBean {
        private final List<AuthenticatorBean> authenticators;
    }

    @Getter
    @RequiredArgsConstructor
    public static class AuthenticatorBean {
        private final String credentialId;
    }
}
