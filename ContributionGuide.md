
# Contribution Guide

This document is to document how contribution to CaDoodle works. 

## 1) Build the software from source

Go to the main readme and checkout the main branch. Build the code from source using the instructions. 

## 2) Understand the Repository structure

CaDoodle is an application made up of the UI application

BowlerStudio robotics engine which provides the 3d engine and the splash screen system

bowler-script-kernel which provides the git file system, the download manager of plugins and all of the language supports. 

java-bowler which provides core math features and kinematics

JCSG which provides the CAD kernel

These modules are connected to CaDoodle-Application as git submodules

```

CaDoodle-Application
  |
  ---BowlerStudio
  		|
  		---bowler-scripting-kernel
  			|
  			---java-bowler
  			|
  			---JCSG
```

## 3) Make a Bug Report or Feature Request

Make a feature request or bug report

https://github.com/CommonWealthRobotics/CaDoodle-Application/issues/new/choose 
 
The Repo has a template for the information to document what you would like to see done. I like to start there to keep a running history of a feature. The URL of this issue will be used in all the following steps. 

Keep any relevant conversation about the feature or issue in this thread to keep it together. 

## 4) (Outside contributors only) Make forks/branches of the repositories you wish to change

If you only make changes to the top level project CaDoodle-Application, just make a fork of that. If you need to make a contribution across many repositories, fork each layer up to CaDoodle-Application. 

## 5) Implement the feature in a branch

Make a branch name with your initials and the title of the issue from step 3. 

Make sure each commit contains the URL of the issue from step 3. 

Once you have at least one commit, make a draft PR of the feature on the main line repository it will eventually be merged into. If I can see work in progress, it is easier for me to help. 

If you are modifying multiple repositories, then you will need one PR for each. 

Commit often with small changes and a message that describes reason for the specific change. Or at the very least do your best to do that :)

Make sure to run

```
./gradlew spotlessApply
```

This will format the code. The spotless checker will fail the CI build if the formatting is not applied by the spotless plugin. 

## 6) Finalizing the feature and merging

Make sure all the Unit tests pass and the final formatting have been run. 

Mark the PR(s) as ready for review and request a review from a maintainer. 

Once the review is in, the mainteiners will merge the PR's in order. 







