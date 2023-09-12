import java.io.File;
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


public class Project {

	public static final File DIR = new File(".");

	public static final File SRC = new File(DIR, "src");
	public static final File MAIN = new File(SRC, "main");
	public static final File JAVA = new File(MAIN, "java");

	public static final File TEST = new File(SRC, "test");
	public static final File TEST_JAVA = new File(TEST, "java");

	public static final File RESOURCES = new File(MAIN, "resources");

	public static final String NEWLINE = "\n";

	public static void requireJavaSources() {
		// Check directory structure
		if (!JAVA.exists() || !JAVA.isDirectory()) {
			System.out.println("Failed to locate the source folder `src/main/java`. Aborting!");
			return;
		}
	}

	/**
	 * Get the optional package of the fully qualified class name.
	 * @param fqcn the fully qualified class name
	 * @return the package wrapped in optional or empty.
	**/
	public static Optional<String> packageOf(String fqcn) {
		return fqcn.contains(".") 
		? Optional.of(fqcn.substring(0, fqcn.lastIndexOf("."))) 
		: Optional.empty();
	}

	/**
	 * Get the class name of the fully qualified class name.
	 * @param fqcn the fully qualified class name
	 * @return the class name.
	**/
	public static String classNameOf(String fqcn) {
		return fqcn.substring(fqcn.lastIndexOf(".") + 1);
	}


	/**
	 * Search the project and find a java source file matching the specified
	 * fully qualified class name.
	 * The function searches the project sources.
	 * @param fqcn the fully qualified class name
	 * @return an optional path or empty if file not found.
	**/
	public static Optional<Path> findJavaSourceFile(String fqcn) {
		String className = classNameOf(fqcn);
		try (Stream<Path> paths = Files.walk(JAVA.toPath())) {
			return paths.filter(p -> p.toFile().getName().equals(className + ".java")).findFirst();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create the Path to a java source file matching the specified
	 * fully qualified class name.
	 * The function just perfroms mapping it does not actually search.
	 * @param fqcn the fully qualified class name
	 * @return the paht.
	**/
	public static Path javaSourceFileOf(String fqcn) {
		return JAVA.toPath().resolve(fqcn.replace('.', File.separatorChar) + ".java");
	}

	/**
	 * Search the project and find a java test file matching the specified
	 * fully qualified class name.
	 * The function searches the project sources.
	 * @param fqcn the fully qualified class name
	 * @return an optional path or empty if file not found.
	**/
	public static Optional<Path> findJavaTestFile(String fqcn) {
		String className = classNameOf(fqcn);
		try (Stream<Path> paths = Files.walk(TEST_JAVA.toPath())) {
			return paths.filter(p -> p.toFile().getName().equals(className + ".java")).findFirst();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create the Path to a java source file matching the specified
	 * fully qualified class name.
	 * The function just perfroms mapping it does not actually search.
	 * @param fqcn the fully qualified class name
	 * @return the paht.
	**/
	public static Path javaTestFileOf(String fqcn) {
		return TEST_JAVA.toPath().resolve(fqcn.replace('.', File.separatorChar) + ".java");
	}

	/**
	 * Read the content of the file
	 * @param path The path to the file
	 * @return The content in String
	*/
	public static String readFile(Path path) {
		try {
			return Files.readAllLines(path).stream().collect(Collectors.joining(NEWLINE));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
