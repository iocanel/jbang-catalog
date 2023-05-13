///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 11+
// Update the Quarkus version to what you want here or run jbang with
// `-Dquarkus.version=<version>` to override it.
//DEPS io.quarkus:quarkus-bom:${quarkus.version:3.0.0.CR1}@pom
//DEPS io.quarkus:quarkus-picocli
//DEPS io.quarkus:quarkus-rest-client-reactive-jackson
//Q:CONFIG quarkus.banner.enabled=false
//Q:CONFIG quarkus.log.level=WARN
//SOURCES chat/GPT.java chat/GPTResponse.java chat/JavaCodeGenerator.java
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.runtime.Quarkus;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 * Inspired and reused code from: https://github.com/maxandersen/jbang-catalog/blob/master/explain/explain.java
 */
@CommandLine.Command
public class GenerateEntity implements Runnable {

	private static final Predicate<Path> IS_JAVA_FILE = path -> path.toString().endsWith(".java");

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
		File root = new File(".");
		File src = new File(root, "src");
		File main = new File(src, "main");
		File java = new File(main, "java");

		// Check directory structure
		if (!java.exists() || !java.isDirectory()) {
			System.out.println("Failed to locate the source folder `src/main/java`. Aborting!");
			return;
		}

		String packageName = name.contains(".") ? name.substring(0, name.lastIndexOf(".")) : null;
		String className = name.substring(name.lastIndexOf(".") + 1);
		Path sourcePath = java.toPath().resolve(name.replace('.', File.separatorChar) + ".java");
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

		JavaCodeGenerator generator = new JavaCodeGenerator(token, model, temperature);

		StringBuilder instructions = new StringBuilder();
		instructions.append("Generatre a JPA entity with class name " + className  + ".");
		if (packageName != null && !packageName.isEmpty()) {
			instructions.append("Use package name " + packageName + ".");
			instructions.append("Ensure that getters and setters are generated.");
		}

		try (FileWriter writer = new FileWriter(sourceFile, false)) {
			System.out.println("Generating entity " + name + " with model " + model + " and temperature " + temperature + ". Have patience...");
			for (String line : generator.generate(instructions.toString())) {
				//Address the javax / jakarta rename which is not handled correctly by ChatGPT
				line = line.replaceAll("(?<![a-zA-Z0-9])javax(?![a-zA-Z0-9])", "jakarta");
				System.out.println(line);
				writer.write(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

