#!/usr/bin/env bash
set -e

mvn package
sls deploy
