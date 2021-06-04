#!/bin/bash

./gradlew shadowJar
java -cp build/libs/antelope-1.0-all.jar MainKt