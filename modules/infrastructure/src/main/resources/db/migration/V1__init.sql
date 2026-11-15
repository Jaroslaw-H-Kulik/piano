CREATE TABLE generic_field_definition (
    tenant_id varchar(128) NOT NULL,
    name      varchar(64)  NOT NULL,
    type      varchar(16)  NOT NULL CHECK (type IN ('BOOL', 'INTEGER', 'STRING', 'DATETIME')),
    PRIMARY KEY (tenant_id, name)
);

CREATE TABLE customer (
    id             bigint       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id      varchar(128) NOT NULL,
    public_id      varchar(128) NOT NULL,
    first_name     varchar(255),
    last_name      varchar(255),
    generic_fields jsonb        NOT NULL DEFAULT '{}',
    version        bigint       NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, public_id)
);
