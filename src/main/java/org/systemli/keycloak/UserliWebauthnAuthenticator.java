package org.systemli.keycloak;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.WebAuthnConstants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.browser.WebAuthnAuthenticator;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;

/**
 * Keycloak's WebAuthn authenticator for security keys registered in Userli.
 * <p>
 * Passkeys (discoverable credentials, e.g. from password managers) send the user handle they were
 * registered with. Userli assigns its own user handles, so they never match the id of the federated
 * Keycloak user, and Keycloak's authenticator rejects the login as "different user". In the second
 * factor step the user is already known from the password step, so the user handle is dropped and
 * Keycloak verifies the assertion against the keys of that user only, like for security keys
 * without user handle.
 */
public class UserliWebauthnAuthenticator extends WebAuthnAuthenticator {

    public UserliWebauthnAuthenticator(KeycloakSession session) {
        super(session);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // Without a known user (passwordless) the user handle is the only way to find the user
        if (context.getUser() == null) {
            super.action(context);
            return;
        }

        super.action(withoutUserHandle(context));
    }

    /**
     * The context with the form parameters of the request, except the user handle.
     */
    static AuthenticationFlowContext withoutUserHandle(AuthenticationFlowContext context) {
        HttpRequest request = context.getHttpRequest();
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>(request.getDecodedFormParameters());
        params.remove(WebAuthnConstants.USER_HANDLE);

        HttpRequest filteredRequest = override(HttpRequest.class, request, "getDecodedFormParameters", params);

        return override(AuthenticationFlowContext.class, context, "getHttpRequest", filteredRequest);
    }

    /**
     * Proxy that returns the given value for the method without parameters and delegates everything else.
     */
    private static <T> T override(Class<T> type, T delegate, String method, Object value) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, invoked, args) -> {
            if (invoked.getName().equals(method) && invoked.getParameterCount() == 0) {
                return value;
            }

            try {
                return invoked.invoke(delegate, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }));
    }
}
