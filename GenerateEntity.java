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
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import io.quarkus.runtime.Quarkus;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 * Inspired and reused code from: https://github.com/maxandersen/jbang-catalog/blob/master/explain/explain.java
 */
@CommandLine.Command
public class GenerateEntity implements Runnable {


	@CommandLine.Parameters(index = "0", description = "The (fully qualified) class name to genearate data for.")
	String name;

	@Option(names = { "-t", "--token" }, description = "The OpenAI API token", required = true, defaultValue = "${OPENAI_API_KEY}", hidden=true)
	String token;

	@Option(names = { "-m", "--model" }, description = "The OpenAI model to use", required = true, defaultValue = "gpt-3.5-turbo", hidden=true)
	String model;

	@Option(names = { "-T", "--temperature" }, description = "The temperature to use", required = true, defaultValue = "0.8", hidden=true)
	double temperature;

	@Override
	public void run() {
		Project.requireJavaSources();
		Optional<String> packageName = Project.packageOf(name);
		String className = Project.classNameOf(name);
		Path sourcePath = Project.javaSourceFileOf(name);
		File sourceFile = sourcePath.toFile();
		try {
			if (!sourceFile.getParentFile().exists() && !sourceFile.getParentFile().mkdirs()) {
				throw new IOException("Can't create directories!");
			}
			if (!sourceFile.getParentFile().isDirectory()) {
				throw new IOException("Not a directory!");
			}
		} catch (IOException e) {
			System.err.println("Failed to create directories for " + sourcePath.toAbsolutePath());
			return;
		}

		CodeGenerator generator = CodeGenerator.forJava(token, model, temperature);

		StringBuilder instructions = new StringBuilder();
		instructions.append("Generatre a JPA entity with class name " + className  + ".");
		instructions.append("The code needs to include getters and setters.");
		packageName.ifPresent(p -> {
			instructions.append("Use package name " + p + ".");
		});

		try (FileWriter writer = new FileWriter(sourceFile, false)) {
			System.out.println("Generating entity " + name + " with model " + model + " and temperature " + temperature + ". Have patience...");
			for (String line : generator.generate(instructions.toString())) {
				//Address the javax / jakarta rename which is not handled correctly by ChatGPT
				line = line.replaceAll("(?<![a-zA-Z0-9])javax(?![a-zA-Z0-9])", "jakarta");
				System.out.println(line);
				writer.write(line + "\n");
			}
			System.out.println("File " + sourceFile.toPath().relativize(Project.DIR.toPath()) + " has been succesfully created!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

