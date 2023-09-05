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
public class GenerateData implements Runnable {


	@CommandLine.Parameters(index = "0", arity="1..N", description = "The (fully qualified) class name to genearate data for.")
	String name;

	@Option(names = { "-t", "--token" }, description = "The OpenAI API token", required = true, defaultValue = "${OPENAI_API_KEY}", hidden=true)
	String token;

	@Option(names = { "-m", "--model" }, description = "The OpenAI model to use", required = true, defaultValue = "gpt-3.5-turbo", hidden=true)
	String model;

	@Option(names = { "-T", "--temperature" }, description = "The temperature to use", required = true, defaultValue = "0.8", hidden=true)
	double temperature;

	@Option(names = { "-l", "--limit" }, description = "The limit (as in number of rows to generate)", required = true, defaultValue = "10", hidden=false)
	int limit;

	@Override
	public void run() {
		File importSql = new File(Project.RESOURCES, "import.sql");
		Optional<Path> sourceFile = Project.findJavaSourceFile(name);
		sourceFile.ifPresent(p -> {
			//working around CR1 bug with passing arguments
			System.out.println("Populating data for entity " + name + " with model " + model + " and temperature " + temperature + ". Have patience...");

			CodeGenerator generator = CodeGenerator.forSQL(token, model, temperature);
			List<String> lines = generator.generate("Your input is going to be JPA entity source files." +
				"The data that you are going to generate should be in the form of sql insert statements for the corresponding tables." +
				"Always include the id, even if it can be auto generated." +
				"Generated data should be realistic." +
				"To minimize the size of the exchanged data responses should contain a single insert statement that inserts " + limit + " rows." +
				"The entity is: \n" +
				Project.readFile(p));


			try (FileWriter writer = new FileWriter(importSql, true)) {
				for (String line : lines) {
					writer.write(line);
					System.out.println(line);
					writer.write("\n");
				}

				System.out.println("File " + importSql.toPath() + " has been succesfully updated!");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
}

