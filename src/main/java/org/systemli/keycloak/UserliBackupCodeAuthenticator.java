package org.systemli.keycloak;

import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.utils.StringUtil;

/**
 * Second factor with the backup codes of Userli. They work for every user with two-factor
 * authentication, also for users with only security keys, and each code works once.
 */
public class UserliBackupCodeAuthenticator implements Authenticator {

    static final String CREDENTIAL_TYPE = "backup-code";

    static final String FIELD = "backupCode";

    private static final String TEMPLATE = "userli-backup-code.ftl";

    private final UserliClientFactory clientFactory;

    public UserliBackupCodeAuthenticator() {
        this(UserliBackupCodeAuthenticator::createClient);
    }

    UserliBackupCodeAuthenticator(UserliClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        // "Try another way" lists every alternative, so skip to the next one without backup codes
        if (!configuredFor(context.getSession(), context.getRealm(), context.getUser())) {
            context.attempted();
            return;
        }

        context.challenge(createForm(context, null));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        String code = context.getHttpRequest().getDecodedFormParameters().getFirst(FIELD);
        UserliHttpClient client = clientFactory.create(context.getSession(), context.getRealm(), context.getUser());

        boolean valid = client != null
            && StringUtil.isNotBlank(code)
            && client.validate(context.getUser().getEmail(), code.trim(), CREDENTIAL_TYPE);

        if (!valid) {
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                createForm(context, "userli-backup-code-invalid"));
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

        return client != null && client.isConfiguredFor(user.getEmail(), CREDENTIAL_TYPE);
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // Backup codes are created in Userli
    }

    @Override
    public void close() {
    }

    private Response createForm(AuthenticationFlowContext context, String error) {
        if (error != null) {
            context.form().addError(new FormMessage(FIELD, error));
        }

        return context.form().createForm(TEMPLATE);
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
}
