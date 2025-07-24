#!/usr/bin/env bash
#
#
#

docker-compose -f ./docker-compose.yml down --remove-orphans --volumes --rmi all develop-tessera-dfe


