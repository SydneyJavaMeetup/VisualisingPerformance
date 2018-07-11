#!/usr/bin/env bash
set -e

mvn package -Dmaven.test.skip=true
sls deploy
