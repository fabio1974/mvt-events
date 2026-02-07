#!/bin/bash
cd /Users/fabio2barros/Documents/projects/mvt-events
set -a
source .env.local
set +a
exec ./gradlew bootRun
