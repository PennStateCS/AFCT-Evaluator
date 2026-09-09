# AFCT Evaluator
## Dependencies
* Java Development Kit (JDK) (preferably from Oracle): https://www.oracle.com/java/technologies/downloads/
* Maven
  * A guide to install Maven on Windows: https://phoenixnap.com/kb/install-maven-windows
  * A guide to install Maven on Linux: https://www.geeksforgeeks.org/devops/how-to-install-maven-on-linux/

## Maven Command
To compile the AFCT Evaluator, you will first need to add the AFCT Client as a Maven dependency.
Run this once, and again whenever the client version in `pom.xml` changes:
```shell
./scripts/install-client-dependency.sh
```
It reads the version from `pom.xml` and installs the matching jar from `afct-client/`, so it
cannot go stale the way a copied command line does. Then build with `mvn clean verify`.

## Set Enviroment Variables
### Automatic Setup
* Go to the `Env-Vars` folder and change the path to the CFGAnalyzer binary file in either the `.ps1` (Windows) or `.sh` (Linux) file
* Run appropriate file

### Manual Setup
* Set the enviroment variable `CFGANALYZER_BINARY` to `"absolute_path_to_CFGBINARY_file"`
* Set the enviroment variable `CFGANALYZER_BINARY` to `15`

## Running Program
* Change the current directory to `target` folder
* Run `java -jar afct-evaluator.jar -h` for more details

## AFCT Client

This repository contains the evaluator for AFCT.

Related repositories:

- [AFCT Dashboard](https://github.com/PennStateCS/AFCT)
- [AFCT Client](https://github.com/PennStateCS/AFCT-Client)


## Making a release
Pushing a version tag publishes a downloadable jar. See [RELEASING.md](RELEASING.md) for the steps.

## Note
* Testing files are located in the `TESTINPUT` folder, including a text file including example scripts
