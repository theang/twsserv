#!/bin/bash
set -e
set -x

create_database_and_user() {
    DATABASE=$1
    USERNAME=$2
    PASSWORD=$3
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        CREATE USER ${USERNAME} WITH PASSWORD '${PASSWORD}';
        CREATE DATABASE ${DATABASE};
        GRANT ALL PRIVILEGES ON DATABASE ${DATABASE} TO ${USERNAME};
EOSQL
}


create_database_and_user serv1 serv1 secret123
create_database_and_user serv1_test serv1_test secret123

