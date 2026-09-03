# AFCT Evaluator
## Dependencies
* Java Development Kit (JDK) (preferably from Oracle): https://www.oracle.com/java/technologies/downloads/
* Maven
  * A guide to install Maven on Windows: https://phoenixnap.com/kb/install-maven-windows
  * A guide to install Maven on Linux: https://www.geeksforgeeks.org/devops/how-to-install-maven-on-linux/

## Maven Command
To compile the AFCT Evaluator, you will first need to add the AFCT Client as a Maven dependency.
This can be done using the following command:
```shell
mvn install:install-file -Dfile="afct-client/afct-client-v1.6.7.jar" -DgroupId="edu.rit.cs" -DartifactId="afct-client" -Dversion="1.6.7" -Dpackaging=jar
```

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


## Note
* Testing files are located in the `TESTINPUT` folder, including a text file including example scripts
