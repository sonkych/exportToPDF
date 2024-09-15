#!/bin/bash

export SPRING_PROFILES_ACTIVE=docker
export IS_DOCKER=true

exec java -jar /app/app.jar