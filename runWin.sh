SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

set -e
JVM_SCRIPTS=$SCRIPT_DIR/BowlerStudio/libraries/bowler-script-kernel/JCSG/jvm
bash $JVM_SCRIPTS/installJdk.sh
export JAVA_HOME=$HOME/bin/CaDoodle-ApplicationInstall/$(bash $JVM_SCRIPTS/getJdkArchiveName.sh)/
echo "Java home set to $JAVA_HOME"

#./gradlew clean shadowJar

$JAVA_HOME/bin/java.exe \
	-XX:MaxRAMPercentage=95 \
	--add-exports javafx.graphics/com.sun.javafx.css=ALL-UNNAMED \
	--add-exports javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED \
	--add-exports javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED \
	--add-exports javafx.base/com.sun.javafx.event=ALL-UNNAMED \
	--add-exports javafx.controls/com.sun.javafx.scene.control.skin.resources=ALL-UNNAMED \
	--add-exports javafx.graphics/com.sun.javafx.util=ALL-UNNAMED \
	--add-exports javafx.graphics/com.sun.javafx.scene.input=ALL-UNNAMED \
	--add-opens javafx.graphics/javafx.scene=ALL-UNNAMED \
	-jar build/libs/CaDoodle-Application.jar
