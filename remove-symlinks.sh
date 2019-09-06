#!/bin/bash
# Remove shared symlinks

cd "${0%/*}"

rm -f "c4a-impl/5x/src/main/java-shared"
rm -f "c4a-impl/5x/src/main/kotlin-shared"
rm -f "c4a-impl/5x/src/main/less"
rm -f "c4a-impl/5x/src/main/resources"
rm -f "c4a-impl/5x/src/main/ts"
rm -f "c4a-impl/5x/package.json"

rm -f "c4a-impl/6x/src/main/java-shared"
rm -f "c4a-impl/6x/src/main/kotlin-shared"
rm -f "c4a-impl/6x/src/main/less"
rm -f "c4a-impl/6x/src/main/resources"
rm -f "c4a-impl/6x/src/main/ts"
rm -f "c4a-impl/6x/package.json"

rm -f "c4a-test/5x/src/main/java"
rm -f "c4a-test/5x/src/main/resources"

rm -f "c4a-test/6x/src/main/java"
rm -f "c4a-test/6x/src/main/resources"