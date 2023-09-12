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
public class GenerateGraphqlApi implements Runnable {


	@CommandLine.Parameters(index = "0", arity="1..N", description = "The (fully qualified) class name of the entity.")
	String entityName;

	@CommandLine.Parameters(index = "1", arity="1..N", description = "The (fully qualified) class name of the rest endpoint.")
	String restEndpointName;

	@Option(names = { "-p", "--panache" }, description = "flag to enable panache", required = false, hidden=true)
	boolean usePanache;

	@Option(names = { "-t", "--token" }, description = "The OpenAI API token", required = true, defaultValue = "${OPENAI_API_KEY}", hidden=true)
	String token;

	@Option(names = { "-m", "--model" }, description = "The OpenAI model to use", required = true, defaultValue = "gpt-3.5-turbo", hidden=true)
	String model;

	@Option(names = { "-T", "--temperature" }, description = "The temperature to use", required = true, defaultValue = "0.1", hidden=true)
	double temperature;

	@Override
	public void run() {

		Optional<String> packageName = Project.packageOf(restEndpointName);
		String className = Project.classNameOf(restEndpointName);
		Path restEndpointPath = Project.javaSourceFileOf(restEndpointName);

		Optional<Path> sourceFile = Project.findJavaSourceFile(entityName);
		sourceFile.ifPresent(f-> {
			//working around CR1 bug with passing arguments
			System.out.println("Generating GraphQL API for entity " + entityName + " with model " + model + " and temperature " + temperature + ". Have patience...");

			CodeGenerator generator = CodeGenerator.forJava(token, model, temperature);
			List<String> lines = generator.generate("Your input is going to be JPA entity source files." +
				"Generate a java class with name: " + className + packageName.map(p -> " and package:" + p).orElse(" and no package") + "."  +
        "The class should be a GraphQL API that implements CRUD operations that uses quarkus-smallrye-graphql."  +
        "The class should import annotations from org.eclipse.microprofile.graphql."  +
        "The class should be annotated with @GrphQLApi."  +
        "@Query and @Mutation should include name that contains the entity name."  +
        "The generated class should only use named queries or utility methods if they exists in the entity." +
        "The generated class may use static methods found on the entity." +
        (usePanache ? "The generated class may use static PanacheEntity methods." : "The generated class should only inject the EntityManager.") +
        "Methods that that write to the database should be annotated with the @jakarta.transaction.Transactional annotation."  +
        "The generated class should include a method to list all." +
        "Use jakarta. packages instead of javax." +
        "The target entity is: \n" +
				Project.readFile(f));

      if (restEndpointPath.toFile().exists()) {
         restEndpointPath.toFile().delete();
      }

			try (FileWriter writer = new FileWriter(restEndpointPath.toFile(), true)) {
				for (String line : lines) {
					writer.write(line);
					System.out.println(line);
					writer.write("\n");
				}

				System.out.println("File " + restEndpointPath + " has been succesfully created!");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
}

