SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

set -e
JVM_SCRIPTS=$SCRIPT_DIR/BowlerStudio/libraries/bowler-script-kernel/JCSG/jvm
bash $JVM_SCRIPTS/installJdk.sh
export JAVA_HOME=$HOME/bin/CaDoodle-ApplicationInstall/$(bash $JVM_SCRIPTS/getJdkArchiveName.sh)/
echo "Java home set to $JAVA_HOME"

./gradlew clean spotlessCheck shadowJar
#$JAVA_HOME/bin/java -jar build/libs/CaDoodle-Application.jar
#./gradlew run
