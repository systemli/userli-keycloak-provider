package org.systemli.keycloak;

import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

public class UserliUserStorageProviderFactory implements UserStorageProviderFactory<UserliUserStorageProvider> {
    public static final String PROVIDER_ID = "userli-storage-provider";

    protected final List<ProviderConfigProperty> configMetadata;

    public UserliUserStorageProviderFactory() {
        configMetadata = ProviderConfigurationBuilder.create()
                .property()
                .name("domain")
                .label("Domain")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("systemli.org")
                .helpText("Domain which is allowed to login")
                .required(true)
                .add()
                .build();
    }

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
        return "Userli User Storage Provider";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }
}
