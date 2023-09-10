import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

import java.net.URI;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

public class CodeGenerator {

	private static final String JAVA_PROMPT = "You are a java code generator. Your responses should be only parsable java code." + 
  "The code should be syntactically correct and compile." + 
  "The code should import all necessary classes." +
  "The code should jarkarta.* instead javax.* packages.";
	private static final String SQL_PROMPT = "You are a sql code generator. Your responses should be only parsable sql code."; 
	private static final String PARSING_INSTRUCTIONS = "The code that will be generated should always be surrounded by triple back ticks ` followed by a newline.";

	private final String token;
	private final String model;
	private final double temperature;
	private final String prompt;

	private CodeGenerator(String token, String model, double temperature, String prompt) {
		this.token = token;
		this.model = model;
		this.temperature = temperature;
		this.prompt = prompt;
	}

	public static CodeGenerator forJava(String token, String model, double temperature) {
		return new CodeGenerator(token, model, temperature, JAVA_PROMPT + PARSING_INSTRUCTIONS);
	}

	public static CodeGenerator forSQL(String token, String model, double temperature) {
		return new CodeGenerator(token, model, temperature, SQL_PROMPT + PARSING_INSTRUCTIONS);
	}

	public List<String> generate(String instructions) {
		List<String> source = new ArrayList<>();
		GPT gpt = RestClientBuilder.newBuilder().baseUri(URI.create("https://api.openai.com")).build(GPT.class);

		final List<Map<String, String>> messages = new ArrayList<>();
		messages.add(prompt("system", prompt));
		messages.add(prompt("user", instructions));

		String previous = null;
		int linesInserted = 0;
		boolean codeStarted = false;
		boolean codeEnded = false;
		while (!codeEnded) {
			var result = gpt.completions(token, Map.of("model", model, "temperature", temperature, "messages", messages));
			String response = result.choices.stream().map(m -> m.message.content).collect(Collectors.joining());
			previous = response;

			String[] lines = response.split("\\R");
			for (String line : lines) {
			if (line.startsWith("```")) {
					if (codeStarted) {
						codeEnded = true;
						break;
					}
					codeStarted=true;
					continue;
				}
				source.add(line);
			}
			messages.add(prompt("assistant", previous));
			messages.add(prompt("system", "continue"));
		}
		return source;
	}

	Map<String, String> prompt(String role, String content) {
		Map<String, String> m = new HashMap<>();
		m.put("role", role);
		m.put("content", content);
		return m;
	}
}
