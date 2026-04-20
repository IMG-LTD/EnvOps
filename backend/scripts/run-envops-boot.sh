#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

: "${ENVOPS_SECURITY_TOKEN_SECRET:?ENVOPS_SECURITY_TOKEN_SECRET is required}"
: "${ENVOPS_CREDENTIAL_PROTECTION_SECRET:?ENVOPS_CREDENTIAL_PROTECTION_SECRET is required}"

mvn -f "$ROOT_DIR/pom.xml" -pl envops-boot -am spring-boot:run
