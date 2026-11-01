package org.systemli.keycloak;

import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.UserCredentialManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;

/**
 * Hands the security keys from Userli to Keycloak's WebAuthn authenticator, which verifies assertions
 * against their public keys. Userli gets the new sign count back. Keys are managed in Userli only,
 * all other credentials are handled by Keycloak as usual.
 */
public class UserliCredentialManager extends UserCredentialManager {

    static final String ID_PREFIX = "userli-webauthn-";

    private static final Logger logger = Logger.getLogger(UserliCredentialManager.class);

    private final UserliUserAdapter user;

    public UserliCredentialManager(KeycloakSession session, RealmModel realm, UserliUserAdapter user) {
        super(session, realm, user);
        this.user = user;
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsByTypeStream(String type) {
        if (!WebAuthnCredentialModel.TYPE_TWOFACTOR.equals(type)) {
            return super.getStoredCredentialsByTypeStream(type);
        }

        return Stream.concat(super.getStoredCredentialsByTypeStream(type), user.getWebauthnCredentials().stream().map(UserliCredentialManager::toModel));
    }

    @Override
    public CredentialModel getStoredCredentialById(String id) {
        if (!isUserliCredential(id)) {
            return super.getStoredCredentialById(id);
        }

        return findWebauthnCredential(id).map(UserliCredentialManager::toModel).orElse(null);
    }

    @Override
    public void updateStoredCredential(CredentialModel cred) {
        if (!isUserliCredential(cred.getId())) {
            super.updateStoredCredential(cred);
            return;
        }

        // Keycloak updates the sign count after it verified an assertion
        Optional<UserliWebauthnCredential> credential = findWebauthnCredential(cred.getId());
        if (credential.isEmpty()) {
            return;
        }

        long signCount = WebAuthnCredentialModel.createFromCredentialModel(cred).getWebAuthnCredentialData().getCounter();
        if (!user.getClient().updateWebauthnSignCount(user.getEmail(), credential.get().getId(), signCount)) {
            logger.warnf("Userli refused the sign count %d of security key %s", signCount, credential.get().getId());
            return;
        }
        credential.get().setSignCount(signCount);
    }

    @Override
    public boolean removeStoredCredentialById(String id) {
        // Security keys are removed in Userli
        return !isUserliCredential(id) && super.removeStoredCredentialById(id);
    }

    @Override
    public void updateCredentialLabel(String credentialId, String credentialLabel) {
        // Security keys are renamed in Userli
        if (!isUserliCredential(credentialId)) {
            super.updateCredentialLabel(credentialId, credentialLabel);
        }
    }

    @Override
    public boolean moveStoredCredentialTo(String id, String newPreviousCredentialId) {
        return !isUserliCredential(id) && !isUserliCredential(newPreviousCredentialId)
            && super.moveStoredCredentialTo(id, newPreviousCredentialId);
    }

    /**
     * Keycloak offers an authenticator only for the credential types of the user, so the security keys from Userli count as well.
     */
    @Override
    public Stream<String> getConfiguredUserStorageCredentialTypesStream() {
        Stream<String> types = super.getConfiguredUserStorageCredentialTypesStream();
        if (user.getWebauthnCredentials().isEmpty()) {
            return types;
        }

        return Stream.concat(types, Stream.of(WebAuthnCredentialModel.TYPE_TWOFACTOR)).distinct();
    }

    private Optional<UserliWebauthnCredential> findWebauthnCredential(String id) {
        return user.getWebauthnCredentials().stream()
            .filter(credential -> id.equals(ID_PREFIX + credential.getId()))
            .findFirst();
    }

    private static boolean isUserliCredential(String id) {
        return id != null && id.startsWith(ID_PREFIX);
    }

    /**
     * Userli encodes the credential id and the COSE public key base64url, Keycloak expects the credential id in plain base64.
     */
    static WebAuthnCredentialModel toModel(UserliWebauthnCredential credential) {
        String credentialId = Base64.getEncoder().encodeToString(Base64.getUrlDecoder().decode(credential.getCredentialId()));
        List<String> transports = credential.getTransports() == null ? List.of() : credential.getTransports();

        WebAuthnCredentialModel model = WebAuthnCredentialModel.create(
            WebAuthnCredentialModel.TYPE_TWOFACTOR,
            credential.getName(),
            credential.getAaguid(),
            credentialId,
            null,
            credential.getPublicKey(),
            credential.getSignCount(),
            credential.getAttestationType(),
            new HashSet<>(transports));
        model.setId(ID_PREFIX + credential.getId());
        if (credential.getCreatedAt() != null) {
            model.setCreatedDate(OffsetDateTime.parse(credential.getCreatedAt()).toInstant().toEpochMilli());
        }

        return model;
    }
}
