bash build.sh
VERSION=$(cat ~/bin/CaDoodle-ApplicationInstall/currentversion.txt)
echo $VERSION
cp build/libs/CaDoodle-Application.jar ~/bin/CaDoodle-ApplicationInstall/$VERSION/