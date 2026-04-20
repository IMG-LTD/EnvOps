#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

mvn -f "$ROOT_DIR/pom.xml" -pl envops-boot -am -Dtest=EnvOpsBootSmokeTest,AuthRouteControllerTest,UserControllerTest,AssetControllerTest,MonitorControllerTest,AppControllerTest,DeployExecutorControllerTest,DeployTaskControllerTest,TrafficControllerTest test
