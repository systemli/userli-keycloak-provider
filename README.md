# KeyCloak User Provider for Userli

This is a user provider for [Userli](https://github.com/systemli/userli) that uses Keycloak for authentication.

## Development

* Requirements: [Docker](https://www.docker.com/), [Docker Compose](https://docs.docker.com/compose/)

1. Build the provider and start the KeyCloak and MariaDB containers:

    ```bash
    docker-compose up -d --build
    ```

2. Login to KeyCloak at [http://localhost:8080](http://localhost:8080) with the credentials `admin`/`admin`.

3. Add the new Provider to KeyCloak:

    * Go to **User Federation**
    * Click **Add Userli-storage-provider**
    * Type "Userli" as **Name**
    * Type "example.org" as **Domain**
    * Click **Save**

4. Now you can login to KeyCloak with following users:

    * `admin@example.org` / `password`
    * `user@example.org` / `password`
