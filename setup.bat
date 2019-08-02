:: Setup shared symlinks

cd %~dp0

rmdir /s /q "c4a-impl/5x/src/main/java-shared"
rmdir /s /q "c4a-impl/5x/src/main/kotlin-shared"
rmdir /s /q "c4a-impl/5x/src/main/less"
rmdir /s /q "c4a-impl/5x/src/main/resources"
rmdir /s /q "c4a-impl/5x/src/main/ts"
del "c4a-impl\5x\package.json"
mklink /J "c4a-impl/5x/src/main/java-shared" "c4a-impl/src/main/java"
mklink /J "c4a-impl/5x/src/main/kotlin-shared" "c4a-impl/src/main/kotlin"
mklink /J "c4a-impl/5x/src/main/less" "c4a-impl/src/main/less"
mklink /J "c4a-impl/5x/src/main/resources" "c4a-impl/src/main/resources"
mklink /J "c4a-impl/5x/src/main/ts" "c4a-impl/src/main/ts"
mklink "c4a-impl/5x/package.json" "..\package.json"

rmdir /s /q "c4a-impl/6x/src/main/java-shared"
rmdir /s /q "c4a-impl/6x/src/main/kotlin-shared"
rmdir /s /q "c4a-impl/6x/src/main/less"
rmdir /s /q "c4a-impl/6x/src/main/resources"
rmdir /s /q "c4a-impl/6x/src/main/ts"
del "c4a-impl\6x\package.json"
mklink /J "c4a-impl/6x/src/main/java-shared" "c4a-impl/src/main/java"
mklink /J "c4a-impl/6x/src/main/kotlin-shared" "c4a-impl/src/main/kotlin"
mklink /J "c4a-impl/6x/src/main/less" "c4a-impl/src/main/less"
mklink /J "c4a-impl/6x/src/main/resources" "c4a-impl/src/main/resources"
mklink /J "c4a-impl/6x/src/main/ts" "c4a-impl/src/main/ts"
mklink "c4a-impl/6x/package.json" "..\package.json"

rmdir /s /q "c4a-test/5x/src/main/java"
rmdir /s /q "c4a-test/5x/src/main/resources"
mklink /J "c4a-test/5x/src/main/java" "c4a-test/src/main/java"
mklink /J "c4a-test/5x/src/main/resources" "c4a-test/src/main/resources"

rmdir /s /q "c4a-test/6x/src/main/java"
rmdir /s /q "c4a-test/6x/src/main/resources"
mklink /J "c4a-test/6x/src/main/java" "c4a-test/src/main/java"
mklink /J "c4a-test/6x/src/main/resources" "c4a-test/src/main/resources"