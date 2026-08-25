#!/usr/bin/env bash
set -euo pipefail

raw_tag="${1:-}"
if [[ -z "$raw_tag" ]]; then
  echo "Docker tag source must not be empty" >&2
  exit 1
fi

normalized="$(
  printf '%s' "$raw_tag" |
    LC_ALL=C sed -E \
      -e 's/[^A-Za-z0-9_.-]+/-/g' \
      -e 's/^[.-]+//' \
      -e 's/-+$//'
)"

if [[ -z "$normalized" ]]; then
  echo "Docker tag source '$raw_tag' has no valid characters" >&2
  exit 1
fi

if (( ${#normalized} > 128 )); then
  if command -v sha256sum >/dev/null 2>&1; then
    digest="$(printf '%s' "$raw_tag" | sha256sum | awk '{print $1}')"
  else
    digest="$(printf '%s' "$raw_tag" | shasum -a 256 | awk '{print $1}')"
  fi
  normalized="${normalized:0:115}-${digest:0:12}"
fi

printf '%s\n' "$normalized"
