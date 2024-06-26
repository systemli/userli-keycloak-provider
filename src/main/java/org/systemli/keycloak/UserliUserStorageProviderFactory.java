package org.systemli.keycloak;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.utils.StringUtil;

import java.util.List;

public class UserliUserStorageProviderFactory implements UserStorageProviderFactory<UserliUserStorageProvider> {

    public static final String PROVIDER_ID = "userli-user-provider";

    @Override
    public UserliUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new UserliUserStorageProvider(session, model);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Userli User Provider";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
            .property(Constants.REALM_DOMAIN, "Realm domain", "Domain used for this Realm", ProviderConfigProperty.STRING_TYPE, "", null)
            .property(Constants.BASE_URL, "Base URL", "Base URL of the API", ProviderConfigProperty.STRING_TYPE, "", null)
            .property(Constants.API_TOKEN, "API token", "Token to interact with Userli", ProviderConfigProperty.STRING_TYPE, "", null)
            .build();
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        if (StringUtil.isBlank(config.get(Constants.REALM_DOMAIN))
            || StringUtil.isBlank(config.get(Constants.BASE_URL))
            || StringUtil.isBlank(config.get(Constants.API_TOKEN))) {
            throw new ComponentValidationException("Configuration not properly set, please verify.");
        }
    }
}
