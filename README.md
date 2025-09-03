# AFCT Evaluator
## Dependencies
* Java Development Kit (JDK) (preferably from Oracle): https://www.oracle.com/java/technologies/downloads/
* Maven: https://maven.apache.org/download.cgi
  * A guide to install Maven on Windows: https://phoenixnap.com/kb/install-maven-windows

## Maven Command
To compile the AFCT Evaluator, you will first need to add the AFCT Client as a Maven dependency.
This can be done using the following command:
```shell
mvn install:install-file -Dfile="afct-client/afct-client.jar" -DgroupId="edu.rit.cs" -DartifactId="afct-client" -Dversion="1.1.2" -Dpackaging=jar
```

## Old README
To use AFCT Evaluator with Context Free Grammars, you will need to provide the path to the CFGAnalyzer binary.
This can be done by setting the environment variable "CFGANALYZER_BINARY".

By default, the limit for CFGAnalyzer is set to 15.
This limit can be changed by setting the environment variable "CFGANALYZER_LIMIT".

## Running Program
