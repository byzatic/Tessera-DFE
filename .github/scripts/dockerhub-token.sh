#!/usr/bin/env bash
set -euo pipefail

: "${DOCKERHUB_USERNAME:?DOCKERHUB_USERNAME is required}"
: "${DOCKERHUB_TOKEN:?DOCKERHUB_TOKEN is required}"

jq -n \
  --arg username "$DOCKERHUB_USERNAME" \
  --arg password "$DOCKERHUB_TOKEN" \
  '{username: $username, password: $password}' |
  curl -fsSL -X POST "https://hub.docker.com/v2/users/login/" \
    -H "Content-Type: application/json" \
    --data-binary @- |
  jq -er '.token'
