:: Setup shared symlinks

cd %~dp0

rmdir /s /q "c4a-impl/5x/src/main/java-shared"
rmdir /s /q "c4a-impl/5x/src/main/kotlin-shared"
rmdir /s /q "c4a-impl/5x/src/main/less"
rmdir /s /q "c4a-impl/5x/src/main/resources"
rmdir /s /q "c4a-impl/5x/src/main/ts"
mklink /J "c4a-impl/5x/src/main/java-shared" "c4a-impl/src/main/java"
mklink /J "c4a-impl/5x/src/main/kotlin-shared" "c4a-impl/src/main/kotlin"
mklink /J "c4a-impl/5x/src/main/less" "c4a-impl/src/main/less"
mklink /J "c4a-impl/5x/src/main/resources" "c4a-impl/src/main/resources"
mklink /J "c4a-impl/5x/src/main/ts" "c4a-impl/src/main/ts"

rmdir /s /q "c4a-impl/6x/src/main/java-shared"
rmdir /s /q "c4a-impl/6x/src/main/kotlin-shared"
rmdir /s /q "c4a-impl/6x/src/main/less"
rmdir /s /q "c4a-impl/6x/src/main/resources"
rmdir /s /q "c4a-impl/6x/src/main/ts"
mklink /J "c4a-impl/6x/src/main/java-shared" "c4a-impl/src/main/java"
mklink /J "c4a-impl/6x/src/main/kotlin-shared" "c4a-impl/src/main/kotlin"
mklink /J "c4a-impl/6x/src/main/less" "c4a-impl/src/main/less"
mklink /J "c4a-impl/6x/src/main/resources" "c4a-impl/src/main/resources"
mklink /J "c4a-impl/6x/src/main/ts" "c4a-impl/src/main/ts"