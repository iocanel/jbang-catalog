
import java.util.List;
import java.util.Map;

/**
 * Original source: https://github.com/maxandersen/jbang-catalog/blob/master/explain/GPTResponse.java
 */
class GPTResponse {
	public String id;
	public String object;
	public double created;
	public String model;
	public Map<String, Double> usage;
	public List<Choice> choices;

	static public class Choice {
		public Choice.Message message;

		public static class Message {
			public String role;
			public String content;
		}
	}
}
