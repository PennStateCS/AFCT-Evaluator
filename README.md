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
mvn install:install-file -Dfile="afct-client/afct-client.jar" -DgroupId="edu.rit.cs" -DartifactId="afct-client" -Dversion="1.1.2" -Dpackaging=jar
```

## Set Enviroment Variables
### Automatic Setup
* Go to the `/Envrioment-Vars` folder and change the path to the CFGAnalyzer binary file in either the `.ps1` (Windows) or `.sh` (Linux) file
* Run appropriate file

### Manual Setup
* Set the enviroment variable `CFGANALYZER_BINARY` to the `"absolute_path_to_CFGBINARY_file"`
* Set the enviroment variable `CFGANALYZER_BINARY` to `15`

## Running Program
* Set the current directory to `path_to_repo\target`
* Run `java -jar afct-evaluator.jar -h` for more details

## Note
* Testing files are located in the `TESTINPUT` folder
