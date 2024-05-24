# Description

This is an example Docker Compose project for running the Wren Engine.

# How to use

1. Set up the platform in `.env` file. (`linux/amd64`, `linux/arm64`)
2. Configure settings in the `etc/config.properties` file.
3. Run the docker-compose in this directory.
    ```bash
    docker compose --env-file .env up
    ```
4. Connect using psql or another PostgreSQL driver using port 7432.
    - Sample usernames and passwords are `ina` and `wah`, or `azki` and `guess`.
    - The default database name should match the catalog of the MDL file.
    - The default schema name should match the schema of the MDL file.
   ```bash
    psql 'host=localhost user=ina dbname=test_catalog port=7432 options=--search_path=test_schema'
    ```
