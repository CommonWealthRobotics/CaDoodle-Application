# Eclipse setup

Make sure you have run build FIRST. 

Download This specific version of Eclipse 

https://github.com/CommonWealthRobotics/ExternalEditorsBowlerStudio/releases

## Setup Eclipse

**Import The Source**

<img src="docs/images/1.png" width="600" alt="Image 1">

**Select gradel ane press next**

<img src="docs/images/2.png" width="600" alt="Image 2">

**Enter where you downloaded the sources in the build step and press next **

**DO NOT press finish **


<img src="docs/images/3.png" width="600" alt="Image 3">

**Use custom configuration and set the Java Home to where the build script put the JVM**

<img src="docs/images/4.png" width="600" alt="Image 4">

**Now hit finish**

<img src="docs/images/5.png" width="600" alt="Image 5">

## Once and only once per workspace, configure Eclipse

**If you import and the build has build errors like this, you may have not yet configured eclipse**

<img src="docs/images/6.png" width="600" alt="Image 6">

**Go to Window->Preferences**

<img src="docs/images/7.png" width="600" alt="Image 7">

**Go to the Java->Installed JREs**

<img src="docs/images/8.png" width="600" alt="Image 8">

**Click Add... and select Standard VM and press next**

<img src="docs/images/9.png" width="600" alt="Image 9">

**Enter the directory where the build script extracted the JVM and hit finish.**

<img src="docs/images/10.png" width="600" alt="Image 10">

**Set the JVM you added as default and delete any others**

<img src="docs/images/11.png" width="600" alt="Image 11">

**Go to Java->Compiler->Errors/Warnings->Deprecated and Restricted and set Forbidden Reference to ignore**

<img src="docs/images/12.png" width="600" alt="Image 12">

**got to Java->Compiler->Building->Build Path Problems and set incompatable required binaries to Ignore**

<img src="docs/images/13.png" width="600" alt="Image 13">

**Go to Java->Compiler and set Compiler compliance level to 1.8**

<img src="docs/images/14.png" width="600" alt="Image 14">