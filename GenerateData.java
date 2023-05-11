///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 11+
// Update the Quarkus version to what you want here or run jbang with
// `-Dquarkus.version=<version>` to override it.
//DEPS io.quarkus:quarkus-bom:${quarkus.version:3.0.0.CR1}@pom
//DEPS io.quarkus:quarkus-picocli
//DEPS io.quarkus:quarkus-rest-client-reactive-jackson
//Q:CONFIG quarkus.banner.enabled=false
//Q:CONFIG quarkus.log.level=WARN
//SOURCES chat/GPT.java chat/GPTResponse.java
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
public class GenerateData implements Runnable {

	private static final Predicate<Path> IS_JAVA_FILE = path -> path.toString().endsWith(".java");

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

		File root = new File(".");
		File src = new File(root, "src");
		File main = new File(src, "main");
		File java = new File(main, "java");
		File resources = new File(main, "resources");
		File importSql = new File(resources, "import.sql");

		// Check directory structure
		if (!java.exists() || !java.isDirectory()) {
			System.out.println("Failed to locate the source folder `src/main/java`. Aborting!");
			return;
		}

		String packageName = name.contains(".") ? name.substring(0, name.lastIndexOf(".")) : null;
		String className = name.substring(name.lastIndexOf(".") + 1);
		Predicate<Path> isSourceFileForClass = path -> path.toString().endsWith(className + ".java");

		try (Stream<Path> paths = Files.walk(java.toPath())) {
			Optional<Path> sourceFile = paths.filter(isSourceFileForClass).findFirst();
			sourceFile.ifPresent(p -> {
				//working around CR1 bug with passing arguments
				System.out.println("Populating data for entity " + p + " with model " + model + " and temperature " + temperature + ". Have patience...");
				GPT gpt = RestClientBuilder.newBuilder().baseUri(URI.create("https://api.openai.com")).build(GPT.class);

				final List<Map<String, String>> messages = new ArrayList<>();
				messages.add(prompt("system", "You are a data generator that generates random data in sql format." +
					"Your input is going to be JPA entity source files." +
					"The data that you are going to generate should be in the form of sql insert statements for the corresponding tables." +
					"Responses should contain no text, or instructions just insert statements. They should always be valid sql code." +
					"To minimize the size of the exchanged data responses should contain a single insert statement that inserts " + limit + " rows.")); 

				try {
					messages.add(prompt("user", Files.readAllLines(sourceFile.get()).stream().collect(Collectors.joining("\n"))));
				} catch (IOException e) {
					throw new IllegalStateException("Could not read " + sourceFile, e);	
				}


				try (FileWriter writer = new FileWriter(importSql, true)) {
					String previous = null;
					int linesInserted = 0;
					while (linesInserted < limit) {
						var result = gpt.completions(token, Map.of("model", model, "temperature", temperature, "messages", messages));
						String response = result.choices.stream().map(m -> m.message.content).collect(Collectors.joining());
						previous = response;

						String insertPart = getInsertInto(response);
						List<String> rows = getRows(response);						

						// Let's trim text that may be added before the insert keyword
						String content = linesInserted == 0 
						? insertPart + "\n" + String.join(",\n", rows)
						: String.join(",\n", rows);

						//Folllow ups occasionally contain trash ensure we always start with a parenthesis
						if (linesInserted != 0) {
							content = content.replaceAll("^[^\\(]*", "");
						}

						linesInserted += rows.size();

						// Let's trim text that may be added after the ;
						if (linesInserted < limit) {
							content = content.trim().replaceAll("\\)\\s*$", "),\n");
						} else {
							content = content.trim().replaceAll("\\)\\s*$", ");\n");
						}

						System.out.println(content);
						writer.write(content);
						messages.add(prompt("assistant", previous));
						messages.add(prompt("system", "continue"));
					}
					System.out.println("File src/main/resources/import.sql has been succesfully updated!");
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	Map<String, String> prompt(String role, String content) {
		Map<String, String> m = new HashMap<>();
		m.put("role", role);
		m.put("content", content);
		return m;
	}

	public static String getInsertInto(String sql) {
		int count = 0;
                String regex = "^.*(INSERT INTO.*VALUES)\\s*";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(sql);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return "";
	}

	public static List<String> getRows(String sql) {
		List<String> result = new ArrayList<>();
                String regex = "\\(([^()]*|\\([^()]*\\))*\\)";
                Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(sql);
		boolean first = true;
		while (matcher.find()) {
			if (!first) {
           		  result.add(matcher.group());
			}
			first = false;
		}
		return result;
	}

	public static int countRows(String sql) {
		int count = 0;
                String regex = "\\(([^()]*|\\([^()]*\\))*\\)";
                return sql.split(regex).length - 1;
	}
}

