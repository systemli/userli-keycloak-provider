# KeyCloak User Provider for Userli

This is a user provider for [Userli](https://github.com/systemli/userli) that uses Keycloak for authentication.

## Development

* Requirements: [Docker](https://www.docker.com/), [Docker Compose](https://docs.docker.com/compose/) or Podman

1. Build the provider and start the KeyCloak, Userli and MariaDB containers:

    ```bash
    podman compose up -d --build
    ```

2. Prepare data in Userli:

    ```bash
    podman compose exec userli bin/console doctrine:schema:create
    podman compose exec userli bin/console doctrine:fixtures:load
    ```

3. Login to KeyCloak at [http://localhost:8080](http://localhost:8080) with the credentials `admin`/`admin`.

4. Add the new Provider to KeyCloak:

    * Go to **User Federation**
    * Click **Add Userli-user-provider**
    * Type "Userli" as **Name**
    * Type "example.org" as **Domain**
    * Type "http://userli/" as **Base URL**
    * Type "keycloak" as **API token**
    * Click **Save**

5. Now you can login to KeyCloak with following users:

    * `admin@example.org` / `password`
    * `user@example.org` / `password`

## Security keys (WebAuthn)

Security keys and backup codes need Userli 6.13.0 or later.

Users can register security keys as second factor in Userli (Account → Two-Factor Authentication → Security Keys).
Keycloak's own **WebAuthn Authenticator** checks them: the provider hands it the keys of the user from Userli, Keycloak
verifies the assertion against the stored public key and sends the new sign count back to Userli.
Keys are registered, renamed and removed in Userli only.

The provider turns off Keycloak's user cache for the Userli federation (cache policy `NO_CACHE`), because cached users
get Keycloak's own credential manager, which doesn't know the keys from Userli. After an update of the provider,
open the federation in the admin console and save it once.

1. Keycloak and Userli must share a domain, e.g. `login.example.org` and `users.example.org`.
2. In Userli, go to **Settings → Two-Factor Authentication** and set **Security Key Domain** to the shared domain,
   e.g. `example.org`.
3. In Keycloak, go to **Authentication → Policies → WebAuthn Policy** and set **Relying Party ID** to the same domain.
   Leave **User Verification Requirement** at `not specified` or `discouraged`.
4. Add the authenticators to the browser flow. Duplicate the **browser** flow and, in its conditional 2FA sub-flow
   (**Condition - user configured**), set **WebAuthn Authenticator** to **Alternative** and add **Userli Backup Code**
   as **Alternative** next to **OTP Form**. Bind the new flow as browser flow.
5. Keep the required action **Webauthn Register** disabled, so users don't register keys in Keycloak.

Users choose between their second factors with **Try another way**. Methods a user has not set up are skipped.

## Backup codes

**Userli Backup Code** (`userli-backup-code`) accepts the backup codes from Userli, also for users with only security keys.
Each code works once. The **OTP Form** keeps accepting backup codes as well.
