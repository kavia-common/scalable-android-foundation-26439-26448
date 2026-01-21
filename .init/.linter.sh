#!/bin/bash
cd /home/kavia/workspace/code-generation/scalable-android-foundation-26439-26448/android_frontend
./gradlew lint
LINT_EXIT_CODE=$?
if [ $LINT_EXIT_CODE -ne 0 ]; then
   exit 1
fi

