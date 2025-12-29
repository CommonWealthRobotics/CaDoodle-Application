set -e
export JAVA_HOME=$HOME/bin/CaDoodle-ApplicationInstall/zulu21.46.19-ca-fx-jdk21.0.9-linux_x64/
./gradlew shadowJar

$JAVA_HOME/bin/java -Dprism.forceGPU=true -XX:MaxRAMPercentage=90.0 --add-exports javafx.graphics/com.sun.javafx.css=ALL-UNNAMED --add-exports javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED --add-exports javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED --add-exports javafx.base/com.sun.javafx.event=ALL-UNNAMED --add-exports javafx.controls/com.sun.javafx.scene.control.skin.resources=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.util=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.scene.input=ALL-UNNAMED --add-opens javafx.graphics/javafx.scene=ALL-UNNAMED  -jar  ./build/libs/CaDoodle-Application.jar "/home/hephaestus/Documents/bowler-workspace/gitcache/github.com/madhephaestus/TestRepo/Doodle1/TestRepo.doodle"
