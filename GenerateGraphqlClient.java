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
public class GenerateGraphqlClient implements Runnable {

	@CommandLine.Parameters(index = "0", arity="1..N", description = "The (fully qualified) class name of the GraphQL api.")
	String apiName;

	@CommandLine.Parameters(index = "1", arity="0..1", description = "The (fully qualified) class name of the client class.")
	String clientName;

	@Option(names = { "--test" }, description = "Flag to generate client under tests", required = false, hidden=true)
	boolean useSrcTest;

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
    if (clientName == null || clientName.isEmpty()) {
      clientName = apiName + "Client";
    }

    if (entityName == null || entityName.isEmpty()) {
      String apiClassName = Project.classNameOf(apiName);
      entityName = apiClassName
      .replaceAll("Resource$", "")
      .replaceAll("GraphQLApi$", "");
    }

		Optional<String> packageName = Project.packageOf(clientName);
		String className = Project.classNameOf(clientName);
		Path clientPath = useSrcTest ? Project.javaTestFileOf(clientName) : Project.javaSourceFileOf(clientName);

		Optional<Path> apiFile = Project.findJavaSourceFile(apiName);
    Optional<Path> entityFile = Project.findJavaSourceFile(entityName);

		File importSql = new File(Project.RESOURCES, "import.sql");
		apiFile.ifPresent(f-> {
			//working around CR1 bug with passing arguments
			System.out.println("Generating " + (useSrcTest ? "test client" : "client") + " for GraphQL API " + apiName + " of entity " + entityName + " with model " + model + " and temperature " + temperature + ". Have patience...");

			CodeGenerator generator = CodeGenerator.forJava(token, model, temperature);
			List<String> lines = generator.generate("Your input is going to be GraphQL API source files." +
				"Generate a java interface with name: " + className + packageName.map(p -> " and package:" + p).orElse(" and no package") + "."  +
        "The interface should be a graphql client interface for consuming the specified GraphQL API."  +
        "The class should be annotated with the @GraphQLClientApi annotation from io.smallrye.graphql.client.typesafe.api package."  +
        "Methods that perform mutations should be annotated with the @org.eclipse.microprofile.graphql.Mutation annotation." +
        "Methods that perform queries should be annotated with the @org.eclipse.microprofile.graphql.Query annotation." +
        "Methods that that write to the database should be annotated with the @jakarta.transaction.Transactional annotation."  +
        "The target GraphQL API is: \n" +
				Project.readFile(f) + "\n." +
        entityFile.map(e -> "The target entity is: \n" + Project.readFile(e)).orElse("") + "."
      );

      if (!clientPath.getParent().toFile().exists() 
      && !clientPath.getParent().toFile().mkdirs()) {
        System.err.println("Can't create directories for " + clientPath.toAbsolutePath());
        System.exit(1);
      }

      if (clientPath.toFile().exists()) {
         clientPath.toFile().delete();
      }

			try (FileWriter writer = new FileWriter(clientPath.toFile(), true)) {
				for (String line : lines) {
					writer.write(line);
					System.out.println(line);
					writer.write("\n");
				}

				System.out.println("File " + clientPath + " has been succesfully created!");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
}

