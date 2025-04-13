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
