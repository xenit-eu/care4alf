:: Remove shared symlinks

cd %~dp0

rmdir /s /q "c4a-impl/5x/src/main/java-shared"
rmdir /s /q "c4a-impl/5x/src/main/kotlin-shared"
rmdir /s /q "c4a-impl/5x/src/main/less"
rmdir /s /q "c4a-impl/5x/src/main/resources"
rmdir /s /q "c4a-impl/5x/src/main/ts"
del "c4a-impl\5x\package.json"

rmdir /s /q "c4a-impl/6x/src/main/java-shared"
rmdir /s /q "c4a-impl/6x/src/main/kotlin-shared"
rmdir /s /q "c4a-impl/6x/src/main/less"
rmdir /s /q "c4a-impl/6x/src/main/resources"
rmdir /s /q "c4a-impl/6x/src/main/ts"
del "c4a-impl\6x\package.json"

rmdir /s /q "c4a-test/5x/src/main/java"
rmdir /s /q "c4a-test/5x/src/main/resources"

rmdir /s /q "c4a-test/6x/src/main/java"
rmdir /s /q "c4a-test/6x/src/main/resources"