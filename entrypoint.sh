#!/bin/bash

export SPRING_PROFILES_ACTIVE=docker

exec java -jar /app/app.jar