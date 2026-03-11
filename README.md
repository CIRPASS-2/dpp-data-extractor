# DPP Data Extractor

A service able to extract data from JSON, JSON-LD or RDF DPPs, and make them available in a storage to enable search over the decentralized repository.

© CIRPASS-2 Consortium, 2024-2027

<img width="832" height="128" alt="image" src="https://raw.githubusercontent.com/CIRPASS-2/assets/main/images/cc-commons.png" />

The CIRPASS-2 project receives funding under the European Union's DIGITAL EUROPE PROGRAMME under the GA No 101158775.

**Important disclaimer:**
All software and artifacts produced by the CIRPASS-2 consortium are designed for exploration and are provided for information purposes only. They should not be interpreted as being either complete, exhaustive, or normative. The CIRPASS-2 consortium partners are not liable for any damage that could result from making use of this information.
Technical interpretations of the European Digital Product Passport system expressed in these artifacts are those of the author(s) only and do not necessarily reflect those of the European Union, European Commission, or the European Health and Digital Executive Agency (HADEA). Neither the European Union, the European Commission nor the granting authority can be held responsible for them. Technical interpretations of the European Digital Product Passport system expressed in these artifacts are those of the author(s) only and should not be interpreted as reflecting those of CEN-CENELEC JTC 24.

## Overview

This application leverages the Mock EU Registry repository to retrieve the latest added DPP registry entry and use the associated live URL to fetch the DPP, extract data from it and persist that data into a separate storage/cache to enable DPP searches.

### Key Features

- **RESTful API** for capabilities exposure and extraction configuration management
- **Flexible Extraction Configuration** with runtime customization support
- **Multi Format DPP support**: JSON, JSON-LD, RDF-XML and other RDF formats like Turtle, N3, N-Quads
- **Multiple database backends** (PostgreSQL, MariaDB)
- **OpenID Connect authentication** with role-based access control

## Table of Contents

- [Quick Start](#quick-start)
- [Configuration](#configuration)
    - [Configuration Variables Reference](#configuration-variables-reference)
    - [Configuration Examples](#configuration-examples)
- [Extraction Configuration](#extraction-configuration)
    - [Default Configuration](#default-configuration)
    - [Configuration Customization](#configuration-customization)
    - [Configuration Resolution Order](#configuration-resolution-order)
- [REST API](#rest-api)
    - [Capabilities Endpoints](#capabilities-endpoints)
    - [Configuration Management Endpoints](#configuration-management-endpoints)
- [Authentication & Authorization](#authentication--authorization)
- [License](#license)
- [Contributing](#contributing)
- [Support](#support)

## Quick Start

The application provides two Maven profiles:
- `pgsql-oidc`: builds the application using PostgreSQL as database and OIDC as authentication method.
- `mariadb-oidc`: builds the application using MariaDB as database and OIDC as authentication method.

Artifacts and Docker images are available [here](https://github.com/cirpass-2/mock-eu-registry/releases).

### Build the Application

```bash
mvn clean install -P pgsql-oidc
```
or
```bash
mvn clean install -P mariadb-oidc
```

### Run the Application

After building, you can run the application using the Quarkus runner.

Create an `application.properties` with your configuration parameters and specify its location.

**Run with PostgreSQL:**
```bash
java -Dquarkus.config.locations=file://path/to/application.properties -Dquarkus.profile=pgsql,oidc -jar target/quarkus-app/quarkus-run.jar
```

**Run with MariaDB:**
```bash
java -Dquarkus.config.locations=file://path/to/application.properties -Dquarkus.profile=mariadb,oidc -jar target/quarkus-app/quarkus-run.jar
```

Instead of an `application.properties`, environment variables can be used:
```bash
QUARKUS_DATASOURCE_REACTIVE_URL=vertx-reactive:postgresql://localhost:5432/registry_db \
QUARKUS_DATASOURCE_USERNAME=db_user \
QUARKUS_DATASOURCE_PASSWORD=db_password \
QUARKUS_OIDC_AUTH_SERVER_URL=https://your-idp.com/realms/your-realm \
QUARKUS_OIDC_CLIENT_ID=your-client-id \
QUARKUS_OIDC_CREDENTIALS_SECRET=your-secret \
java -jar target/quarkus-app/quarkus-run.jar
```

### Using Docker

See the [Docker Compose](#docker-compose) examples in the configuration section.

## Configuration

### Configuration Variables Reference

#### Database Configuration

| Variable                                           | Environment Variable                              | Description                                            | Default |
|----------------------------------------------------|---------------------------------------------------|--------------------------------------------------------|---------|
| `quarkus.datasource.reactive.url`                  | `QUARKUS_DATASOURCE_REACTIVE_URL`                 | Registry database URL                                  | -       |
| `quarkus.datasource.username`                      | `QUARKUS_DATASOURCE_USERNAME`                     | Registry database username                             | -       |
| `quarkus.datasource.password`                      | `QUARKUS_DATASOURCE_PASSWORD`                     | Registry database password                             | -       |
| `quarkus.datasource.reactive.max-size`             | `QUARKUS_DATASOURCE_REACTIVE_MAX_SIZE`            | Registry database maximum connection pool size         | `16`    |
| `quarkus.datasource.extraction.reactive.url`       | `QUARKUS_DATASOURCE_EXTRACTION_REACTIVE_URL`      | Extraction database URL (where search keys are stored) | -       |
| `quarkus.datasource.extraction.username`           | `QUARKUS_DATASOURCE_EXTRACTION_USERNAME`          | Extraction database username                           | -       |
| `quarkus.datasource.extraction.password`           | `QUARKUS_DATASOURCE_EXTRACTION_PASSWORD`          | Extraction database password                           | -       |
| `quarkus.datasource.extraction.reactive.max-size`  | `QUARKUS_DATASOURCE_EXTRACTION_REACTIVE_MAX_SIZE` | Extraction database maximum connection pool size       | `16`    |

**PostgreSQL Reactive URL format:**
```
vertx-reactive:postgresql://hostname:port/database_name
```
Example: `vertx-reactive:postgresql://localhost:5432/registry_db`

**MariaDB Reactive URL format:**
```
vertx-reactive:mysql://hostname:port/database_name
```
Example: `vertx-reactive:mysql://localhost:3306/registry_db`

> **Note**: For MariaDB, the reactive driver uses the `mysql` protocol identifier.

**PostgreSQL Schema Script (Extraction DB):**
```sql
CREATE TABLE IF NOT EXISTS extraction_registry (
    id BIGSERIAL PRIMARY KEY,
    processed_until TIMESTAMP
);

CREATE TABLE IF NOT EXISTS extraction_failures (
    id BIGSERIAL PRIMARY KEY,
    registry_id VARCHAR(36) UNIQUE NOT NULL,
    retrials INTEGER NOT NULL DEFAULT(0)
);

CREATE TABLE IF NOT EXISTS dpp_data (
    id BIGSERIAL PRIMARY KEY,
    upi VARCHAR(36) UNIQUE NOT NULL,
    live_url VARCHAR(1000),
    search_data JSONB NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS json_configs_seq;

CREATE TABLE IF NOT EXISTS json_configs (
    id BIGINT PRIMARY KEY DEFAULT nextval('json_configs_seq'),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_config JSONB NOT NULL
);
```

**MariaDB Schema Script (Extraction DB):**
```sql
CREATE TABLE IF NOT EXISTS dpp_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    registry_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    metadata JSON NOT NULL
);

CREATE TABLE IF NOT EXISTS extraction_registry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    processed_until TIMESTAMP
);

CREATE TABLE IF NOT EXISTS extraction_failures (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    registry_id VARCHAR(36) UNIQUE NOT NULL,
    retrials INT NOT NULL DEFAULT(0)
);

CREATE TABLE IF NOT EXISTS dpp_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    upi VARCHAR(36) UNIQUE NOT NULL,
    live_url VARCHAR(1000),
    search_data JSON NOT NULL
);

CREATE TABLE IF NOT EXISTS json_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_config JSON NOT NULL
);
```

#### OpenID Connect Configuration

| Variable                             | Environment Variable                 | Description                              | Default  |
|--------------------------------------|--------------------------------------|------------------------------------------|----------|
| `quarkus.oidc.auth-server-url`       | `QUARKUS_OIDC_AUTH_SERVER_URL`       | OIDC server URL (realm URL for Keycloak) | -        |
| `quarkus.oidc.client-id`             | `QUARKUS_OIDC_CLIENT_ID`             | OIDC client ID                           | -        |
| `quarkus.oidc.credentials.secret`    | `QUARKUS_OIDC_CREDENTIALS_SECRET`    | OIDC client secret                       | -        |
| `quarkus.oidc.roles.role-claim-path` | `QUARKUS_OIDC_ROLES_ROLE_CLAIM_PATH` | Role claim path in the JWT token         | `groups` |

#### Application Configuration

| Variable                                        | Environment Variable                            | Description                                                                                         | Default                                                    |
|-------------------------------------------------|-------------------------------------------------|-----------------------------------------------------------------------------------------------------|------------------------------------------------------------|
| `extractor.upi-field-name`                      | `EXTRACTOR_UPI_FIELD_NAME`                      | Field name in the registry entry for the Unique Product Identifier                                  | `upi`                                                      |
| `extractor.live-URL-field-name`                 | `EXTRACTOR_LIVE_URL_FIELD_NAME`                 | Field name in the registry entry for the live DPP URL                                               | `liveURL`                                                  |
| `extractor.granularity-level-field-name`        | `EXTRACTOR_GRANULARITY_LEVEL_FIELD_NAME`        | Field name in the registry entry for the granularity level                                          | `granularityLevel`                                         |
| `extractor.role-mappings`                       | `EXTRACTOR_ROLE_MAPPINGS`                       | Comma-separated mappings between external IdP roles and internal roles                              | `admin:admin,eo:eo,eu:eu`                                  |
| `extractor.extraction-config-location`          | `EXTRACTOR_EXTRACTION_CONFIG_LOCATION`          | Location of a custom extraction configuration (URL, file URI, or absolute path)                     | -                                                          |
| `extractor.max.retrials`                        | `EXTRACTOR_MAX_RETRIALS`                        | Maximum number of retry attempts for a failed extraction operation on a specific registry entry     | `3`                                                        |
| `start.extractor.every`                         | `START_EXTRACTOR_EVERY`                         | Polling interval for the extractor job (number + time unit, e.g. `30s`, `5m`)                       | `5s`                                                       |
| `extractor.dpp.reference-ontology.contexts`     | `EXTRACTOR_DPP_REFERENCE_ONTOLOGY_CONTEXTS`     | Context URIs used to identify JSON-LD documents compliant with the reference ontology               | -                                                          |
| `extractor.dpp.reference-ontology.vocabularies` | `EXTRACTOR_DPP_REFERENCE_ONTOLOGY_VOCABULARIES` | Vocabulary URIs (`@vocab`) used to identify JSON-LD documents compliant with the reference ontology | `http://dpp.taltech.ee/EUDPP#,http://dpp.taltech.ee/EUDPP` |

#### HTTP Configuration

| Variable            | Environment Variable | Description              | Default |
|---------------------|----------------------|--------------------------|---------|
| `quarkus.http.port` | `QUARKUS_HTTP_PORT`  | HTTP port of the service | `8080`  |

#### Configuration Notes

**Role Mappings**

Maps external Identity Provider roles to internal application roles. Internal roles are `admin`, `eo` (Economic Operator), and `eu` (End User).

Format: `external_role:internal_role,another_external:another_internal`

Example: `keycloak_admin:admin,keycloak_operator:eo,keycloak_user:eu`

**OIDC Role Claim Path**

Supports multiple paths separated by commas. The system searches for roles in the JWT token at each specified path in order.

Example: `group,realm_access.roles`

**Extractor Config Location**

Supports multiple formats:
- HTTP URL: `https://example.com/config.json`
- File URI: `file:///path/to/config.json`
- Absolute path: `/etc/extractor/config.json`

### Configuration Examples

#### Application Properties (PostgreSQL)

```properties
# Database - Registry
quarkus.datasource.reactive.url=vertx-reactive:postgresql://localhost:5432/registry_db
quarkus.datasource.username=dbuser
quarkus.datasource.password=dbpass
quarkus.datasource.reactive.max-size=20

# Database - Extraction
quarkus.datasource.extraction.reactive.url=vertx-reactive:postgresql://localhost:5432/extraction_db
quarkus.datasource.extraction.username=dbuser
quarkus.datasource.extraction.password=dbpass
quarkus.datasource.extraction.reactive.max-size=20

# OIDC
quarkus.oidc.auth-server-url=https://keycloak.example.com/realms/myrealm
quarkus.oidc.client-id=my-client
quarkus.oidc.credentials.secret=my-secret
quarkus.oidc.roles.role-claim-path=group,realm_access.roles

# Application
extractor.upi-field-name=upi
extractor.live-URL-field-name=liveURL
extractor.granularity-level-field-name=granularityLevel
extractor.role-mappings=keycloak_admin:admin,keycloak_operator:eo,keycloak_user:eu
extractor.extraction-config-location=/etc/extractor/config.json
extractor.max.retrials=3
start.extractor.every=30s
extractor.dpp.reference-ontology.vocabularies=http://dpp.taltech.ee/EUDPP#,http://dpp.taltech.ee/EUDPP
```

#### Application Properties (MariaDB)

```properties
# Database - Registry
quarkus.datasource.reactive.url=vertx-reactive:mysql://localhost:3306/registry_db
quarkus.datasource.username=dbuser
quarkus.datasource.password=dbpass
quarkus.datasource.reactive.max-size=20

# Database - Extraction
quarkus.datasource.extraction.reactive.url=vertx-reactive:mysql://localhost:3306/extraction_db
quarkus.datasource.extraction.username=dbuser
quarkus.datasource.extraction.password=dbpass
quarkus.datasource.extraction.reactive.max-size=20

# OIDC
quarkus.oidc.auth-server-url=https://keycloak.example.com/realms/myrealm
quarkus.oidc.client-id=my-client
quarkus.oidc.credentials.secret=my-secret
quarkus.oidc.roles.role-claim-path=group,realm_access.roles

# Application
extractor.upi-field-name=upi
extractor.live-URL-field-name=liveURL
extractor.granularity-level-field-name=granularityLevel
extractor.role-mappings=keycloak_admin:admin,keycloak_operator:eo,keycloak_user:eu
extractor.extraction-config-location=/etc/extractor/config.json
extractor.max.retrials=3
start.extractor.every=30s
extractor.dpp.reference-ontology.vocabularies=http://dpp.taltech.ee/EUDPP#,http://dpp.taltech.ee/EUDPP
```

#### Docker Compose (PostgreSQL)

```yaml
version: '3.8'

services:
  extractor:
    image: ghcr.io/cirpass-2/dpp-data-extractor-pgsql-oidc:latest
    ports:
      - "8080:8080"
    environment:
      # Database - Registry
      QUARKUS_DATASOURCE_REACTIVE_URL: vertx-reactive:postgresql://postgres:5432/registry_db
      QUARKUS_DATASOURCE_USERNAME: dbuser
      QUARKUS_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      QUARKUS_DATASOURCE_REACTIVE_MAX_SIZE: 20

      # Database - Extraction
      QUARKUS_DATASOURCE_EXTRACTION_REACTIVE_URL: vertx-reactive:postgresql://postgres:5432/extraction_db
      QUARKUS_DATASOURCE_EXTRACTION_USERNAME: dbuser
      QUARKUS_DATASOURCE_EXTRACTION_PASSWORD: ${DB_PASSWORD}
      QUARKUS_DATASOURCE_EXTRACTION_REACTIVE_MAX_SIZE: 20

      # OIDC
      QUARKUS_OIDC_AUTH_SERVER_URL: https://keycloak:8443/realms/myrealm
      QUARKUS_OIDC_CLIENT_ID: my-client
      QUARKUS_OIDC_CREDENTIALS_SECRET: ${OIDC_SECRET}
      QUARKUS_OIDC_ROLES_ROLE_CLAIM_PATH: group,realm_access.roles

      # Application
      EXTRACTOR_UPI_FIELD_NAME: upi
      EXTRACTOR_LIVE_URL_FIELD_NAME: liveURL
      EXTRACTOR_GRANULARITY_LEVEL_FIELD_NAME: granularityLevel
      EXTRACTOR_ROLE_MAPPINGS: keycloak_admin:admin,keycloak_operator:eo,keycloak_user:eu
      EXTRACTOR_EXTRACTION_CONFIG_LOCATION: /etc/extractor/config.json
      EXTRACTOR_MAX_RETRIALS: 3
      START_EXTRACTOR_EVERY: 30s
      EXTRACTOR_DPP_REFERENCE_ONTOLOGY_VOCABULARIES: "http://dpp.taltech.ee/EUDPP#,http://dpp.taltech.ee/EUDPP"
    depends_on:
      - postgres
    volumes:
      - ./config.json:/etc/extractor/config.json:ro

  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: registry_db
      POSTGRES_USER: dbuser
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

volumes:
  postgres-data:
```

#### Docker Compose (MariaDB)

```yaml
version: '3.8'

services:
  extractor:
    image: ghcr.io/cirpass-2/dpp-data-extractor-mariadb-oidc:latest
    ports:
      - "8080:8080"
    environment:
      # Database - Registry
      QUARKUS_DATASOURCE_REACTIVE_URL: vertx-reactive:mysql://mariadb:3306/registry_db
      QUARKUS_DATASOURCE_USERNAME: dbuser
      QUARKUS_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      QUARKUS_DATASOURCE_REACTIVE_MAX_SIZE: 20

      # Database - Extraction
      QUARKUS_DATASOURCE_EXTRACTION_REACTIVE_URL: vertx-reactive:mysql://mariadb:3306/extraction_db
      QUARKUS_DATASOURCE_EXTRACTION_USERNAME: dbuser
      QUARKUS_DATASOURCE_EXTRACTION_PASSWORD: ${DB_PASSWORD}
      QUARKUS_DATASOURCE_EXTRACTION_REACTIVE_MAX_SIZE: 20

      # OIDC
      QUARKUS_OIDC_AUTH_SERVER_URL: https://keycloak:8443/realms/myrealm
      QUARKUS_OIDC_CLIENT_ID: my-client
      QUARKUS_OIDC_CREDENTIALS_SECRET: ${OIDC_SECRET}
      QUARKUS_OIDC_ROLES_ROLE_CLAIM_PATH: group,realm_access.roles

      # Application
      EXTRACTOR_UPI_FIELD_NAME: upi
      EXTRACTOR_LIVE_URL_FIELD_NAME: liveURL
      EXTRACTOR_GRANULARITY_LEVEL_FIELD_NAME: granularityLevel
      EXTRACTOR_ROLE_MAPPINGS: keycloak_admin:admin,keycloak_operator:eo,keycloak_user:eu
      EXTRACTOR_EXTRACTION_CONFIG_LOCATION: /etc/extractor/config.json
      EXTRACTOR_MAX_RETRIALS: 3
      START_EXTRACTOR_EVERY: 30s
      EXTRACTOR_DPP_REFERENCE_ONTOLOGY_VOCABULARIES: "http://dpp.taltech.ee/EUDPP#,http://dpp.taltech.ee/EUDPP"
    depends_on:
      - mariadb
    volumes:
      - ./config.json:/etc/extractor/config.json:ro

  mariadb:
    image: mariadb:11
    environment:
      MARIADB_DATABASE: registry_db
      MARIADB_USER: dbuser
      MARIADB_PASSWORD: ${DB_PASSWORD}
      MARIADB_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
    volumes:
      - mariadb-data:/var/lib/mysql
    ports:
      - "3306:3306"

volumes:
  mariadb-data:
```

#### Kubernetes Deployment

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: extractor-config
data:
  QUARKUS_DATASOURCE_REACTIVE_URL: "vertx-reactive:postgresql://postgres-service:5432/registry_db"
  QUARKUS_DATASOURCE_USERNAME: "dbuser"
  QUARKUS_DATASOURCE_REACTIVE_MAX_SIZE: "20"
  QUARKUS_DATASOURCE_EXTRACTION_REACTIVE_URL: "vertx-reactive:postgresql://postgres-service:5432/extraction_db"
  QUARKUS_DATASOURCE_EXTRACTION_USERNAME: "dbuser"
  QUARKUS_DATASOURCE_EXTRACTION_REACTIVE_MAX_SIZE: "20"
  QUARKUS_OIDC_AUTH_SERVER_URL: "https://keycloak.example.com/realms/myrealm"
  QUARKUS_OIDC_CLIENT_ID: "my-client"
  QUARKUS_OIDC_ROLES_ROLE_CLAIM_PATH: "group,realm_access.roles"
  EXTRACTOR_UPI_FIELD_NAME: "upi"
  EXTRACTOR_LIVE_URL_FIELD_NAME: "liveURL"
  EXTRACTOR_GRANULARITY_LEVEL_FIELD_NAME: "granularityLevel"
  EXTRACTOR_ROLE_MAPPINGS: "keycloak_admin:admin,keycloak_operator:eo,keycloak_user:eu"
  EXTRACTOR_EXTRACTION_CONFIG_LOCATION: "/etc/extractor/config.json"
  EXTRACTOR_MAX_RETRIALS: "3"
  START_EXTRACTOR_EVERY: "30s"
  EXTRACTOR_DPP_REFERENCE_ONTOLOGY_VOCABULARIES: "http://dpp.taltech.ee/EUDPP#,http://dpp.taltech.ee/EUDPP"

---
apiVersion: v1
kind: Secret
metadata:
  name: extractor-secrets
type: Opaque
stringData:
  QUARKUS_DATASOURCE_PASSWORD: "dbpass"
  QUARKUS_DATASOURCE_EXTRACTION_PASSWORD: "dbpass"
  QUARKUS_OIDC_CREDENTIALS_SECRET: "my-secret"

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: extractor-deployment
spec:
  replicas: 2
  selector:
    matchLabels:
      app: extractor
  template:
    metadata:
      labels:
        app: extractor
    spec:
      containers:
      - name: extractor
        image: ghcr.io/cirpass-2/dpp-data-extractor-pgsql-oidc:latest
        ports:
        - containerPort: 8080
        envFrom:
        - configMapRef:
            name: extractor-config
        - secretRef:
            name: extractor-secrets
        volumeMounts:
        - name: extraction-config-volume
          mountPath: /etc/extractor
          readOnly: true
      volumes:
      - name: extraction-config-volume
        configMap:
          name: extraction-json-config
```

## Extraction Configuration

The application uses a JSON configuration to determine how to extract values from DPPs. The configuration is organized into four top-level sections: `searchFields`, `knownOntology`, `noOntology`, and `unknownOntology`. The three strategy sections are not mutually exclusive: the extractor determines which strategy to apply based on the format and semantic content of each incoming DPP document, and applies the corresponding section of the configuration.

### `searchFields`

An array of objects that declares the complete set of fields the extractor will attempt to populate for each DPP. Every field name referenced in any of the three strategy sections must have a corresponding entry here. Fields not listed in `searchFields` are ignored even if matched by a strategy.

Each entry supports the following properties:

| Property     | Required | Description                                                                                                                                                                                                                                                                                                                   |
|--------------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `fieldName`  | yes      | The name under which the extracted value will be stored in the storage/cache. This is also the key used to reference the field in the strategy sections.                                                                                                                                                                      |
| `targetType` | yes      | The expected data type of the extracted value. Supported values: `STRING`, `DECIMAL`. The extractor will attempt to coerce the raw value to this type.                                                                                                                                                                        |
| `dependsOn`  | no       | The `fieldName` of another field in this array. Refers a field to which this field is logically related, used to provide hints to client that might need to relate or render the data retrieved from the extractor. Typically used for unit-of-measure fields that are meaningless without their corresponding numeric value. |

### `knownOntology`

Configures extraction from JSON-LD documents that are compliant with the reference ontology. A document is considered compliant if its `@context` contains a URI matching one of the values in `extractor.dpp.reference-ontology.contexts`, or if its `@vocab` matches one of the values in `extractor.dpp.reference-ontology.vocabularies`.

Because the ontology structure is known, this strategy uses explicit property paths rather than heuristic name matching. The configuration provides the extractor with the exact traversal path from the document root to the target value.

The section contains a single `fields` object whose keys are `fieldName` values from `searchFields`. Each field entry has a `reference` object describing the traversal:

| Property     | Required        | Description                                                                                                                                                                                                                                                                                                                |
|--------------|-----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `key`        | yes             | The property name to navigate from the current node. For root-level scalar properties (e.g. `productName`) this is sufficient on its own. For nested values, this is the relation property to follow (e.g. `hasProperty`, `hasProductGroup`).                                                                              |
| `nativeType` | yes (leaf only) | The native type of the value at this node. Present only on leaf nodes (nodes without a `child`). Supported values: `STRING`, `DECIMAL`.                                                                                                                                                                                    |
| `@type`      | no              | The expected OWL class of the child node reached by following `key`. Used to disambiguate when the same relation (e.g. `hasProperty`) can point to nodes of different types (e.g. `Weight`, `CarbonFootprint`, `Durability`). The extractor will only follow the relation if the target node's `@type` matches this value. |
| `child`      | no              | A nested `reference` object describing the next step of the traversal. Present whenever the target value is not directly the object of `key` but requires a further navigation step within the matched node.                                                                                                               |

**Traversal example** — extracting `weight`:
```
Product → hasProperty → [node with @type Weight] → numericalValue → (decimal value)
```
Which maps to:
```json
{
  "@type": "Weight",
  "key": "hasProperty",
  "child": {
    "key": "numericalValue",
    "nativeType": "DECIMAL"
  }
}
```

**Traversal example** — extracting `productName` (root-level scalar, no child needed):
```json
{
  "key": "productName",
  "nativeType": "STRING"
}
```

### `noOntology`

Configures extraction from plain JSON documents that carry no semantic layer. The extractor walks the entire JSON object graph recursively, visiting every node at every nesting level, and at each node it applies two parallel matching mechanisms.

The section contains a single `fields` object whose keys are `fieldName` values from `searchFields`. Each field entry supports:

| Property              | Required | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|-----------------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `variants`            | yes      | A list of property names to look for directly on any visited node. If any node in the document has a key that matches one of these values (case-insensitive), the corresponding value is considered a candidate for extraction.                                                                                                                                                                                                                                              |
| `variantsWithContext` | no       | A context-conditioned matching rule. Extraction is attempted only when the **parent** node's key matches one of the `context` values (case-insensitive). When the context condition is met, the extractor looks for any of the `field` values as a key within the current node (case-insensitive). This allows disambiguation of generic field names (e.g. `value`, `uom`) that would otherwise produce false positives if matched unconditionally anywhere in the document. |

`variantsWithContext` has two sub-properties:

| Property  | Description                                                                                         |
|-----------|-----------------------------------------------------------------------------------------------------|
| `context` | List of parent node key names that must match for this rule to activate.                            |
| `field`   | List of property names to look for within the current node once the context condition is satisfied. |

Both `variants` and `variantsWithContext` are evaluated at every level of the document. `variants` alone is sufficient for fields with globally unique names; `variantsWithContext` is the preferred mechanism for fields with common or ambiguous names that need a structural anchor to be correctly identified.

**Example** — `weightUom` would produce false positives if matched by a generic variant like `uom` anywhere in the document. Binding it to a `context` of `["weight", "dimensionW"]` ensures the extractor only picks up a `uom` property that is a direct child of a node keyed `weight` or `dimensionW`.

### `unknownOntology`

Configures extraction from JSON-LD documents that carry semantic type information (`@type`) but are not compliant with the reference ontology (i.e. they do not match any of the configured context or vocabulary URIs). This strategy leverages the `@type` annotations present in the document to anchor the search, combining the structural disambiguation of a typed graph with the flexibility of name-variant matching.

The extractor walks the JSON-LD graph and, at each node that has a `@type` property, checks whether any of the declared types matches one of the configured `typeHints`. A `@type` value can be either a full URI (e.g. `http://example.org/ontology#Weight`) or a local term (e.g. `Weight`); the extractor matches against the local name in both cases.

When a node's `@type` matches a `typeHint`, the extractor searches the properties of that node for any key matching one of the `variants`.

The section contains a single `fields` object whose keys are `fieldName` values from `searchFields`. Each field entry supports:

| Property    | Required | Description                                                                                                                                                                                 |
|-------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `variants`  | yes      | List of property names to look for within a node whose `@type` has matched a `typeHint`. Matching is case-insensitive.                                                                      |
| `typeHints` | yes      | List of type names (local terms) to match against the `@type` of each visited JSON-LD node. The extractor compares against the local part of the URI if `@type` is expressed as a full URI. |

**Example** — to extract the numerical value of a weight node from an unknown ontology, the extractor looks for any node typed as `Weight`, `QuantitativeValue`, or `MassValue`, then within that node looks for a property named `numericalValue`, `value`, or `amount`:
```json
{
  "variants": ["numericalValue", "value", "amount"],
  "typeHints": ["Weight", "QuantitativeValue", "MassValue"]
}
```

### Default Configuration

The application ships with a default configuration that extracts the following fields:

- `productName` — product or model name
- `codeValue` — product classification code (e.g. ECLASS, UNSPSC)
- `codeSet` — URI of the classification scheme the code belongs to
- `environmentalFootprint` / `environmentalFootprintUom` — aggregated environmental footprint value and unit
- `recyclingRate` / `recyclingRateUom` — recycling rate value and unit
- `energyConsumption` / `energyConsumptionUom` — energy consumption value and unit
- `weight` / `weightUom` — product weight and unit
- `durability` / `durabilityUom` — durability indicator value and unit

<details>
<summary>View complete default configuration</summary>

```json
{
  "searchFields": [
    {
      "fieldName": "productName",
      "targetType": "STRING"
    },
    {
      "fieldName": "codeValue",
      "targetType": "STRING"
    },
    {
      "fieldName": "codeSet",
      "targetType": "STRING"
    },
    {
      "fieldName": "manufacturerName",
      "targetType": "STRING"
    },
    {
      "fieldName": "carbonFootprint",
      "targetType": "DECIMAL"
    },
    {
      "fieldName": "carbonFootprintUom",
      "targetType": "STRING",
      "dependsOn": "carbonFootprint"
    },
    {
      "fieldName": "recyclingRate",
      "targetType": "DECIMAL"
    },
    {
      "fieldName": "recyclingRateUom",
      "targetType": "STRING",
      "dependsOn": "recyclingRate"
    },
    {
      "fieldName": "energyConsumption",
      "targetType": "DECIMAL"
    },
    {
      "fieldName": "energyConsumptionUom",
      "targetType": "STRING",
      "dependsOn": "energyConsumption"
    },
    {
      "fieldName": "weight",
      "targetType": "DECIMAL"
    },
    {
      "fieldName": "weightUom",
      "targetType": "STRING",
      "dependsOn": "weight"
    },
    {
      "fieldName": "durability",
      "targetType": "DECIMAL"
    },
    {
      "fieldName": "durabilityUom",
      "targetType": "STRING",
      "dependsOn": "durability"
    }
  ],

  "knownOntology": {
    "fields": {
      "productName": {
        "reference": {
          "key": "productName",
          "nativeType": "STRING"
        }
      },
      "codeValue": {
        "reference": {
          "@type": "ClassificationCode",
          "key": "hasProductGroup",
          "child": {
            "key": "codeValue",
            "nativeType": "STRING"
          }
        }
      },
      "codeSet": {
        "reference": {
          "@type": "ClassificationCode",
          "key": "hasProductGroup",
          "child": {
            "key": "codeSet",
            "nativeType": "STRING"
          }
        }
      },
      "manufacturerName": {
        "reference": {
          "@type": "Actor",
          "key": "hasManufacturer",
          "child": {
            "key": "actorName",
            "nativeType": "STRING"
          }
        }
      },
      "carbonFootprint": {
        "reference": {
          "@type": "CarbonFootprint",
          "key": "hasProperty",
          "child": {
            "key": "numericalValue",
            "nativeType": "DECIMAL"
          }
        }
      },
      "carbonFootprintUom": {
        "reference": {
          "@type": "CarbonFootprint",
          "key": "hasProperty",
          "child": {
            "key": "hasMeasurementUnit",
            "nativeType": "STRING"
          }
        }
      },
      "recyclingRate": {
        "reference": {
          "@type": "RecyclingRate",
          "key": "hasProperty",
          "child": {
            "key": "numericalValue",
            "nativeType": "DECIMAL"
          }
        }
      },
      "recyclingRateUom": {
        "reference": {
          "@type": "RecyclingRate",
          "key": "hasProperty",
          "child": {
            "key": "hasMeasurementUnit",
            "nativeType": "STRING"
          }
        }
      },
      "energyConsumption": {
        "reference": {
          "@type": "EnergyConsumption",
          "key": "hasProperty",
          "child": {
            "key": "numericalValue",
            "nativeType": "DECIMAL"
          }
        }
      },
      "energyConsumptionUom": {
        "reference": {
          "@type": "EnergyConsumption",
          "key": "hasProperty",
          "child": {
            "key": "hasMeasurementUnit",
            "nativeType": "STRING"
          }
        }
      },
      "weight": {
        "reference": {
          "@type": "Weight",
          "key": "hasProperty",
          "child": {
            "key": "numericalValue",
            "nativeType": "DECIMAL"
          }
        }
      },
      "weightUom": {
        "reference": {
          "@type": "Weight",
          "key": "hasProperty",
          "child": {
            "key": "hasMeasurementUnit",
            "nativeType": "STRING"
          }
        }
      },
      "durability": {
        "reference": {
          "@type": "Durability",
          "key": "hasProperty",
          "child": {
            "key": "numericalValue",
            "nativeType": "DECIMAL"
          }
        }
      },
      "durabilityUom": {
        "reference": {
          "@type": "Durability",
          "key": "hasProperty",
          "child": {
            "key": "hasMeasurementUnit",
            "nativeType": "STRING"
          }
        }
      }
    }
  },
  "noOntology": {
    "fields": {
      "productName": {
        "variants": ["productName", "modelName"],
        "variantsWithContext": {
          "context": ["model", "product"],
          "field": ["productName", "modelName", "name"]
        }
      },
      "codeValue": {
        "variants": ["codeValue", "productGroupCode", "classificationCode", "categoryCode"],
        "variantsWithContext": {
          "context": ["productGroup", "classification", "category", "classificationCode"],
          "field": ["code", "codeValue", "value", "id"]
        }
      },
      "codeSet": {
        "variants": ["codeSet", "classificationCodeSet", "categoryCodeSet", "codeSystem", "classificationSystem"],
        "variantsWithContext": {
          "context": ["productGroup", "classification", "category", "classificationCode"],
          "field": ["codeSet", "system", "scheme", "uri", "namespace"]
        }
      },
      "manufacturerName": {
        "variants": ["manufacturerName", "manufacturer", "producerName", "brandName", "companyName"],
        "variantsWithContext": {
          "context": ["manufacturer", "producer", "brand", "actor", "economicOperator"],
          "field": ["actorName", "name", "manufacturerName", "companyName", "registeredTradeName"]
        }
      },
      "carbonFootprint": {
        "variants": ["carbonFootprint", "carbonEmission", "co2Footprint", "totalKgCo2", "kgCo2"],
        "variantsWithContext": {
          "context": ["carbonFootprint", "co2", "carbonEmission", "kgCo2"],
          "field": ["value", "numericalValue", "amount", "total", "totalKgCo2"]
        }
      },
      "carbonFootprintUom": {
        "variants": [
          "carbonFootprintUom", "kgCo2Uom", "totalKgCo2Uom",
          "carbonFootprintUnitOfMeasure", "kgCo2UnitOfMeasure",
          "carbonFootprintMeasurementUnit", "kgCo2MeasurementUnit"
        ],
        "variantsWithContext": {
          "context": ["carbonFootprint", "co2", "carbonEmission", "kgCo2", "totalKgCo2"],
          "field": ["uom", "unitOfMeasure", "measurementUnit", "unit"]
        }
      },
      "recyclingRate": {
        "variants": ["recyclingRate", "recyclingRateValue", "recyclingPercentage"],
        "variantsWithContext": {
          "context": ["recycling", "recyclingRate", "circularEconomy"],
          "field": ["value", "numericalValue", "rate", "percentage", "amount"]
        }
      },
      "recyclingRateUom": {
        "variants": [
          "recyclingRateUom", "recyclingRateUnitOfMeasure",
          "recyclingRateMeasurementUnit", "recyclingPercentageUom"
        ],
        "variantsWithContext": {
          "context": ["recyclingRate", "recycling"],
          "field": ["uom", "unitOfMeasure", "measurementUnit", "unit"]
        }
      },
      "energyConsumption": {
        "variants": ["energyConsumption", "energyUse", "powerConsumption", "energyUsage"],
        "variantsWithContext": {
          "context": ["energy", "energyConsumption", "power"],
          "field": ["value", "numericalValue", "consumption", "amount", "total"]
        }
      },
      "energyConsumptionUom": {
        "variants": [
          "energyConsumptionUom", "energyUseUom",
          "energyConsumptionUnitOfMeasure", "energyUseUnitOfMeasure",
          "energyConsumptionMeasurementUnit"
        ],
        "variantsWithContext": {
          "context": ["energyConsumption", "energyUse", "energy"],
          "field": ["uom", "unitOfMeasure", "measurementUnit", "unit"]
        }
      },
      "weight": {
        "variants": ["weight", "weightG", "weightKg"],
        "variantsWithContext": {
          "context": ["dimension", "dimensions", "physicalDimension","weight"],
          "field": ["weight", "weightG", "weightKg"]
        }
      },
      "weightUom": {
        "variants": ["weightUom", "weightUnitOfMeasure", "weightMeasurementUnit"],
        "variantsWithContext": {
          "context": ["weight", "dimensionW"],
          "field": ["uom", "unitOfMeasure", "measurementUnit", "unit"]
        }
      },
      "durability": {
        "variants": ["durability", "durabilityValue", "lifespan", "expectedLifespan", "productLifespan"],
        "variantsWithContext": {
          "context": ["durability", "quality", "qualityIndicator", "lifespan"],
          "field": ["value", "numericalValue", "years", "amount", "lifespan"]
        }
      },
      "durabilityUom": {
        "variants": [
          "durabilityUom", "durabilityUnitOfMeasure",
          "durabilityMeasurementUnit", "lifespanUom", "lifespanUnit"
        ],
        "variantsWithContext": {
          "context": ["durability", "lifespan"],
          "field": ["uom", "unitOfMeasure", "measurementUnit", "unit"]
        }
      }
    }
  },

  "unknownOntology": {
    "fields": {
      "productName": {
        "variants": ["productName", "modelName", "name"],
        "typeHints": ["Product", "ProductData", "DPP", "DPPData", "Model", "ModelData"]
      },
      "codeValue": {
        "variants": ["codeValue", "code", "value", "id"],
        "typeHints": ["ClassificationCode", "ProductGroup", "ProductCategory", "Category"]
      },
      "codeSet": {
        "variants": ["codeSet", "system", "scheme", "uri", "namespace"],
        "typeHints": ["ClassificationCode", "ProductGroup", "ProductCategory", "Category"]
      },
      "manufacturerName": {
        "variants": ["actorName", "name", "manufacturerName", "companyName", "registeredTradeName"],
        "typeHints": ["Actor", "Manufacturer", "ManufacturerRole", "LegalPerson", "Company", "Organisation"]
      },
      "carbonFootprint": {
        "variants": ["numericalValue", "value", "amount", "total", "totalKgCo2"],
        "typeHints": ["CarbonFootprint", "CO2Footprint", "CarbonEmission", "GHGEmission"]
      },
      "carbonFootprintUom": {
        "variants": ["hasMeasurementUnit", "unit", "uom", "unitOfMeasure"],
        "typeHints": ["CarbonFootprint", "CO2Footprint", "CarbonEmission", "GHGEmission"]
      },
      "recyclingRate": {
        "variants": ["numericalValue", "value", "rate", "percentage", "amount"],
        "typeHints": ["RecyclingRate", "RecyclingIndicator", "CircularEconomyIndicator", "RecyclingPercentage"]
      },
      "recyclingRateUom": {
        "variants": ["hasMeasurementUnit", "unit", "uom", "unitOfMeasure"],
        "typeHints": ["RecyclingRate", "RecyclingIndicator", "CircularEconomyIndicator"]
      },
      "energyConsumption": {
        "variants": ["numericalValue", "value", "consumption", "amount", "total"],
        "typeHints": ["EnergyConsumption", "EnergyUse", "PowerConsumption", "EnergyUsage"]
      },
      "energyConsumptionUom": {
        "variants": ["hasMeasurementUnit", "unit", "uom", "unitOfMeasure"],
        "typeHints": ["EnergyConsumption", "EnergyUse", "PowerConsumption"]
      },
      "weight": {
        "variants": ["numericalValue", "value", "amount"],
        "typeHints": ["Weight", "QuantitativeValue", "MassValue"]
      },
      "weightUom": {
        "variants": ["hasMeasurementUnit", "unit", "uom", "unitOfMeasure"],
        "typeHints": ["Weight", "QuantitativeValue", "MassValue"]
      },
      "durability": {
        "variants": ["numericalValue", "value", "years", "lifespan", "amount"],
        "typeHints": ["Durability", "ProductDurability", "LifeExpectancy", "QualityIndicator"]
      },
      "durabilityUom": {
        "variants": ["hasMeasurementUnit", "unit", "uom", "unitOfMeasure"],
        "typeHints": ["Durability", "ProductDurability", "LifeExpectancy"]
      }
    }
  }
}
```

</details>

### Configuration Customization

The application provides two ways to override the default configuration:

#### 1. Startup Configuration

Provide a configuration file location via the `extractor.extraction-config-location` property. The file is loaded once at application startup and takes precedence over the embedded default.

```properties
extractor.extraction-config-location=/etc/extractor/config.json
```

#### 2. Runtime API Submission

Submit a new configuration at runtime via the [POST /config/v1](#post-configv1) endpoint. The submitted configuration becomes immediately active without requiring a restart, and takes precedence over both the file-based and the default configuration.

### Configuration Resolution Order

The application resolves the active configuration according to the following priority order:

1. **Most recent configuration submitted via API** (highest priority)
2. **Configuration loaded from `extractor.extraction-config-location`** (if set)
3. **Default embedded configuration** (fallback)

## REST API

The application exposes two API groups:

1. **Capabilities API**: Returns the list of `searchFields` from the currently active configuration. Useful for clients (e.g. the search/renderer frontend) that need to know which fields are available in the storage/cache to build search queries dynamically.
2. **Configuration API**: Allows runtime submission and retrieval of extraction configurations without restarting the application.

To obtain the full OpenAPI document, start the application and issue a `GET` request to `/q/openapi`, using the `Accept` header to negotiate the format (`application/json` or `application/yaml`).

### Capabilities Endpoints

#### GET /capabilities/v1

Returns the `searchFields` array from the currently active extraction configuration.

**Example Request:**

```http
GET /capabilities/v1
```

**Example Response:**

```json
[
  { "fieldName": "productName", "targetType": "STRING" },
  { "fieldName": "codeValue", "targetType": "STRING" },
  { "fieldName": "codeSet", "targetType": "STRING" },
  { "fieldName": "environmentalFootprint", "targetType": "DECIMAL" },
  { "fieldName": "environmentalFootprintUom", "targetType": "STRING", "dependsOn": "environmentalFootprint" },
  { "fieldName": "recyclingRate", "targetType": "DECIMAL" },
  { "fieldName": "recyclingRateUom", "targetType": "STRING", "dependsOn": "recyclingRate" },
  { "fieldName": "energyConsumption", "targetType": "DECIMAL" },
  { "fieldName": "energyConsumptionUom", "targetType": "STRING", "dependsOn": "energyConsumption" },
  { "fieldName": "weight", "targetType": "DECIMAL" },
  { "fieldName": "weightUom", "targetType": "STRING", "dependsOn": "weight" },
  { "fieldName": "durability", "targetType": "DECIMAL" },
  { "fieldName": "durabilityUom", "targetType": "STRING", "dependsOn": "durability" }
]
```

### Configuration Management Endpoints

#### POST /config/v1

Submits a new extraction configuration. The submitted configuration immediately becomes the active one.

**Request Body:** A valid extraction configuration JSON.

**Example Request:**

```http
POST /config/v1
Content-Type: application/json

{
  "searchFields": [
    { "fieldName": "productName", "targetType": "STRING" },
    { "fieldName": "carbonFootprint", "targetType": "DECIMAL" }
  ],
  "knownOntology": {
    "fields": {
      "productName": {
        "reference": { "key": "productName", "nativeType": "STRING" }
      },
      "carbonFootprint": {
        "reference": {
          "@type": "CarbonFootprint",
          "key": "hasProperty",
          "child": { "key": "numericalValue", "nativeType": "DECIMAL" }
        }
      }
    }
  },
  "noOntology": {
    "fields": {
      "productName": {
        "variants": ["productName", "modelName"],
        "variantsWithContext": {
          "context": ["model", "product"],
          "field": ["productName", "modelName", "name"]
        }
      },
      "carbonFootprint": {
        "variants": ["carbonFootprint"],
        "variantsWithContext": {
          "context": ["carbonFootprint"],
          "field": ["value", "totalKgCo2", "kgCo2", "numericalValue"]
        }
      }
    }
  },
  "unknownOntology": {
    "fields": {
      "productName": {
        "variants": ["productName", "modelName", "name"],
        "typeHints": ["Product", "ProductData", "DPP", "DPPData", "Model", "ModelData"]
      },
      "carbonFootprint": {
        "variants": ["numericalValue", "value", "totalKgCo2"],
        "typeHints": ["CarbonFootprint", "CO2Footprint", "CarbonEmission"]
      }
    }
  }
}
```

#### GET /config/v1/current

Retrieves the currently active extraction configuration.

**Example Response:** Returns the full configuration JSON currently in use (same structure as the request body of `POST /config/v1`).

#### DELETE /config/v1/current

Removes the most recently submitted API configuration. After deletion, the system reverts to the next configuration in the [resolution order](#configuration-resolution-order):

1. If other configurations were previously submitted via API, the second-most-recent becomes active.
2. Otherwise, reverts to the file-based configuration (if `extractor.extraction-config-location` is set).
3. Otherwise, reverts to the default embedded configuration.

## Authentication & Authorization

The application uses **OpenID Connect (OIDC)** for authentication and implements role-based access control.

### Supported Roles

- **`admin`**: Full administrative access
- **`eo`** (Economic Operator): Operator-level access
- **`eu`** (End User): End-user level access

### Role Mapping

External Identity Provider roles are mapped to internal roles via the `extractor.role-mappings` configuration property:

```properties
extractor.role-mappings=keycloak_admin:admin,idp_operator:eo,idp_user:eu
```

### JWT Role Claims

The application extracts roles from JWT tokens using the paths specified in `quarkus.oidc.roles.role-claim-path`. Multiple paths can be specified as a comma-separated list; the system searches each path in order until roles are found.

```properties
quarkus.oidc.roles.role-claim-path=group,realm_access.roles,resource_access.my-client.roles
```

---

## License

This project is licensed under the Apache License 2.0.

```
Copyright 2024-2027 CIRPASS-2

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Contributing

We welcome contributions to this project. To contribute:

1. **Open a Pull Request** on GitHub with your changes.
2. **Include tests** for all modifications:
    - Bug fixes must include tests that verify the fix.
    - New features must include comprehensive test coverage.
    - Improvements should include tests where applicable.
3. **Request a review** from the maintainers.
4. Ensure all existing tests pass and that the code follows the project's coding standards.

All contributions will be reviewed before being merged.

## Support

For questions, issues, or support requests, please contact:

**marco.volpini@extrared.it**