To compile the AFCT Evaluator, you will first need to add the AFCT Client as a Maven dependency.
This can be done using the following command:
```shell
mvn install:install-file -Dfile="afct-client/afct-client.jar" -DgroupId="edu.rit.cs" -DartifactId="afct-client" -Dversion="1.1.2" -Dpackaging=jar
```