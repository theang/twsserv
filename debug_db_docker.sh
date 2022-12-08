#!/bin/bash

# run:
# > /usr/local/bin/docker-entrypoint.sh postgres
# to start entrypoint (database initializations)

docker run -it --rm --user $(id -u):$(id -g) --name serv1-db -p 55432:5432 -v /etc/passwd:/etc/passwd:ro -v $(pwd)/data/db:/var/lib/postgresql/data -v $(pwd)/docker/docker-entrypoint-initdb.d:/docker-entrypoint-initdb.d -e POSTGRES_PASSWORD=secret123 postgres:14.5-alpine /bin/bash

