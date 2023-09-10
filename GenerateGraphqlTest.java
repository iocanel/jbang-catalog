///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 11+
// Update the Quarkus version to what you want here or run jbang with
// `-Dquarkus.version=<version>` to override it.
//DEPS io.quarkus:quarkus-bom:${quarkus.version:3.0.0.CR1}@pom
//DEPS io.quarkus:quarkus-picocli
//DEPS io.quarkus:quarkus-rest-client-reactive-jackson
//Q:CONFIG quarkus.banner.enabled=false
//Q:CONFIG quarkus.log.level=WARN
//SOURCES chat/GPT.java chat/GPTResponse.java chat/CodeGenerator.java chat/Project.java
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.Quarkus;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 * Inspired and reused code from: https://github.com/maxandersen/jbang-catalog/blob/master/explain/explain.java
 */
@CommandLine.Command
public class GenerateGraphqlTest implements Runnable {

	@CommandLine.Parameters(index = "0", arity="1..N", description = "The (fully qualified) class name of the GraphQL api.")
	String apiName;

	@CommandLine.Parameters(index = "1", arity="0..1", description = "The (fully qualified) class name of the test class.")
	String testName;

	@Option(names = { "-e", "--entity" }, description = "The entity to use", required = false, hidden=true)
	String entityName;

	@Option(names = { "-t", "--token" }, description = "The OpenAI API token", required = true, defaultValue = "${OPENAI_API_KEY}", hidden=true)
	String token;

	@Option(names = { "-m", "--model" }, description = "The OpenAI model to use", required = true, defaultValue = "gpt-3.5-turbo", hidden=true)
	String model;

	@Option(names = { "-T", "--temperature" }, description = "The temperature to use", required = true, defaultValue = "0.1", hidden=true)
	double temperature;

	@Override
	public void run() {
    if (testName == null || testName.isEmpty()) {
      testName = apiName + "Test";
    }

    if (entityName == null || entityName.isEmpty()) {
      String apiClassName = Project.classNameOf(apiName);
      entityName = apiClassName
      .replaceAll("Resource$", "")
      .replaceAll("GraphQLApi$", "");
    }

		Optional<String> packageName = Project.packageOf(testName);
		String className = Project.classNameOf(testName);
		Path testPath = Project.javaTestFileOf(testName);

		Optional<Path> apiFile = Project.findJavaSourceFile(apiName);
    Optional<Path> entityFile = Project.findJavaSourceFile(entityName);

		File importSql = new File(Project.RESOURCES, "import.sql");
		apiFile.ifPresent(f-> {
			//working around CR1 bug with passing arguments
			System.out.println("Generating test for GraphQL API " + apiName + " of entity " + entityName + " with model " + model + " and temperature " + temperature + ". Have patience...");

			CodeGenerator generator = CodeGenerator.forJava(token, model, temperature);
			List<String> lines = generator.generate("Your input is going to be GraphQL API source files." +
				"Generate a java 17 class with name: " + className + packageName.map(p -> " and package:" + p).orElse(" and no package") + "."  +
        "The class should be Junit5 that uses RestAssured to test the GraphQL API."  +
        "Use java 17 triple quotation syntax for grapql queries and mutations instead of escaping quotes."  +
        "The class should be annotated with the @QuarkusTest annotation."  +
        "The test methods should take into consideration the import.sql that is used to initialize test data."  +
        "The method that tests create should use realistic random data and avoid data that are present in the import.sql and may cause constraint violations."  +
        "The import.sql is imported by Quarkus and should not be imported by the test class."  +
        "The test should only interact with application via GraphQL." +
        "The test should never inject the EntityManager." +
        "The test should use no Before or After annotations."  +
        "Avoid using the same ids for create, delete and update methods to prevent ordering issues." +
        "The target GraphQL API is: \n" +
				Project.readFile(f) + "\n." +
        entityFile.map(e -> "The target entity is: \n" + Project.readFile(e)).orElse("") +
        "The import.sql script that is used to initialize test data is:" + Project.readFile(importSql.toPath()) + "."
      );

      if (!testPath.getParent().toFile().exists() 
      && !testPath.getParent().toFile().mkdirs()) {
        System.err.println("Can't create directories for " + testPath.toAbsolutePath());
        System.exit(1);
      }

      if (testPath.toFile().exists()) {
         testPath.toFile().delete();
      }

			try (FileWriter writer = new FileWriter(testPath.toFile(), true)) {
				for (String line : lines) {
					writer.write(line);
					System.out.println(line);
					writer.write("\n");
				}

				System.out.println("File " + testPath + " has been succesfully created!");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
}

