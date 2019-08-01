#!/bin/bash
# Setup shared symlinks

cd "${0%/*}"

rm -f "c4a-impl/5x/src/main/java-shared"
rm -f "c4a-impl/5x/src/main/kotlin-shared"
rm -f "c4a-impl/5x/src/main/less"
rm -f "c4a-impl/5x/src/main/resources"
rm -f "c4a-impl/5x/src/main/ts"
ln -s "../../../src/main/java" "c4a-impl/5x/src/main/java-shared"
ln -s "../../../src/main/kotlin" "c4a-impl/5x/src/main/kotlin-shared"
ln -s "../../../src/main/less" "c4a-impl/5x/src/main/less"
ln -s "../../../src/main/resources" "c4a-impl/5x/src/main/resources"
ln -s "../../../src/main/ts" "c4a-impl/5x/src/main/ts"