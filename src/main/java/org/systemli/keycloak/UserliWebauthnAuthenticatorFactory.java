package org.systemli.keycloak;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.WebAuthnAuthenticatorFactory;
import org.keycloak.models.KeycloakSession;

/**
 * Keycloak's WebAuthn authenticator, with support for passkeys registered in Userli.
 * Use it instead of the "WebAuthn Authenticator" in the browser flow.
 */
public class UserliWebauthnAuthenticatorFactory extends WebAuthnAuthenticatorFactory {

    public static final String PROVIDER_ID = "userli-webauthn-authenticator";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Userli WebAuthn Authenticator";
    }

    @Override
    public String getHelpText() {
        return "Second factor with the security keys and passkeys of Userli.";
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new UserliWebauthnAuthenticator(session);
    }
}
