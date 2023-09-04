//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.6.1
//DEPS org.eclipse.jgit:org.eclipse.jgit:6.5.0.202303070854-r
//DEPS org.apache.maven:maven-model:3.8.1
//DEPS org.apache.maven:maven-model-builder:3.8.1
//DEPS org.codehaus.plexus:plexus-utils:3.3.0
//DEPS org.slf4j:slf4j-simple:1.6.1
//Q:CONFIG quarkus.log.level=ERROR
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.Optional;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Command(name = "create-from-quickstart",
description = "Clone a Quarkus quickstart locally and clear its git configuration.",
mixinStandardHelpOptions = true)
@SuppressWarnings("deprecation")
public class CreateFromQuickstart implements Runnable {

    private String repository = "quarkusio/quarkus-quickstarts";

    @Parameters(index = "0", description = "The name of the quickstart to clone.", arity = "1")
    private String quickstart;

    @Parameters(index = "1", description = "The coordinates of the project in the form [[GROUP-ID:]ARTIFACT-ID[:VERSION]].", arity = "0..1")
    private String coords;

    public void run() {
        String newArtifactId = getArtifactId(coords);

        File checkoutDir = new File(quickstart);
        File finalDir = new File(newArtifactId != null ? newArtifactId : quickstart);
       

        // Fail fast
        if (finalDir.exists()) {
            System.out.println("A file named " + finalDir.getName() + " already exists. Aborting!");
            return;
        }

        try {

            Git git = null;
            Path cloneDir = null;
            for (int retry=1; retry <= 3; retry++) {
                try {
                    cloneDir = Files.createTempDirectory("create-from-quickstart-" + repoName);
                     // Clone the repository
                    CloneCommand cloneCommand = Git.cloneRepository()
                    .setURI("https://github.com/" + repository + ".git")
                    .setNoCheckout(true)
                    .setDirectory(cloneDir.toFile());

                    git = cloneCommand.call();
                } catch (TransportException e) {
                    System.out.println("Transport error, retrying ...");
                    continue;
                }
            }

            Repository repository = git.getRepository();
            RevWalk revWalk = new RevWalk(repository);
            ObjectId objectId = repository.resolve("HEAD");
            git.checkout().setStartPoint(objectId.getName()).addPath(quickstart).call();
            repository.close();

            // Clear the Git configuration
            File dotGit = repository.getDirectory();
            File config = new File(dotGit, Constants.CONFIG);
            config.delete();

            File root = new File(dotGit.getParentFile(), quickstart);
            File newRoot = new File(newArtifactId != null ? newArtifactId : repoName);
            move(root.toPath(), newRoot.toPath());

            // Check if coords have been specified for the project.
            if (coords != null && !coords.isBlank()) {
                updateProject(newRoot, coords);
                System.out.println("Created project: " + newArtifactId + " from repository:" + repoName);
            } else {
                System.out.println("Create project: " + repoName + " from repository:" + repoName);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Extracts the groupId from the specified maven coordinates.
     * The coordinates fomat is [[GROUP-ID:]ARTIFACT-ID[:VERSION]]
     * @param coords the coordinates
     * @return the groupId wrapped in {@link Optional} or throws {@link IllegalArgumentException} if the format is not expected.
     */
    public static Optional<String> getGroupId(String coords) {
        String[] parts = coords.split(":");
        switch (parts.length) {
            case 1:
            return Optional.empty();
            case 2:
            return Optional.of(parts[0]);
            case 3:
            return  Optional.of(parts[0]);
            default:
            throw new IllegalArgumentException("Invalid argument: " + coords);
        }
    }


    /**
     * Extracts the artifactId from the specified maven coordinates.
     * The coordinates fomat is [[GROUP-ID:]ARTIFACT-ID[:VERSION]]
     * @param coords the coordinates
     * @return the artifactId or throws {@link IllegalArgumentException} if the format is not expected.
     */
    public static String getArtifactId(String coords) {
        if (coords == null) {
            return null;
        }
        String[] parts = coords.split(":");
        switch (parts.length) {
            case 1:
            return parts[0];
            case 2:
            return parts[1];
            case 3:
            return  parts[1];
            default:
            throw new IllegalArgumentException("Invalid argument: " + coords);
        }
    }

    /**
     * Extracts the version from the specified maven coordinates.
     * The coordinates fomat is [[GROUP-ID:]ARTIFACT-ID[:VERSION]]
     * @param coords the coordinates
     * @return the version wraped in {@link Optional} or throws {@link IllegalArgumentException} if the format is not expected.
     */
    public static Optional<String> getVersion(String coords) {
        String[] parts = coords.split(":");
        switch (parts.length) {
            case 1:
            return Optional.empty();
            case 2:
            return Optional.empty();
            case 3:
            return  Optional.of(parts[2]);
            default:
            throw new IllegalArgumentException("Invalid argument: " + coords);
        }
    }


    /**
     * Removes common suffix present in root module artifactIds
     * For example:
     * - -pom
     *   -project
     *   -parent
     * @param artifactId
     * @return the base of the artfiactId
     */
    public static String createBaseArtifactId(String artifactId) {
        return artifactId.replaceAll("\\-pom$", "").replaceAll("\\-project", "").replaceAll("\\-parent", "");
    }

    public static void updateProject(File root, String coords) {
        String groupId = getGroupId(coords).orElse(null);
        String artifactId = getArtifactId(coords);
        String version = getVersion(coords).orElse(null);


        try {
            boolean isMultiModule = isMultiModuleProject(root);
            // Load the Maven model
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader(new File(root, "pom.xml")));

            if (!isMultiModule) {
                updateSingleModuleProject(root, groupId, artifactId, version);
            } else {
                String existingGroupId = model.getGroupId();

                String existingArtifactId = model.getArtifactId();
                String existingArtifactBase = createBaseArtifactId(existingArtifactId);
                String newArtifactIdBase = createBaseArtifactId(artifactId);

                String existingVersion = model.getVersion();
                visitPomFiles(root, f -> updateProjectModule(f, existingGroupId, existingArtifactBase, existingVersion, groupId, newArtifactIdBase, version));
            }

        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    private static boolean isMultiModuleProject(File directory) throws IOException {
        File rootPomFile = new File(directory, "pom.xml");
        if (!rootPomFile.exists()) {
            return false;
        }
        try {
            String content = new String(Files.readAllBytes(rootPomFile.toPath()));
            return content.contains("<modules>");
        } catch (IOException e) {
            System.err.println("Failed to read root pom file: " + e.getMessage());
            return false;
        }
    }

    private static void updateSingleModuleProject(File root, String groupId, String artifactId, String version) {
        try {
            // Load the Maven model
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader(new File(root, "pom.xml")));

            if (groupId != null) {
                model.setGroupId(groupId);
            }
            if (artifactId != null) {
                model.setArtifactId(artifactId);
            }
            if (version != null) {
                model.setVersion(version);
            }

            // Save the updated Maven model
            MavenXpp3Writer writer = new MavenXpp3Writer();
            FileWriter fileWriter = new FileWriter(new File(root, "pom.xml"));
            writer.write(fileWriter, model);
            fileWriter.close();

        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    private static void updateProjectModule(File pomFile, String originalGroupId, String originalArtifactId, String originalVersion, String groupId, String artifactId, String version) {
        try {
            // Load the Maven model
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader(pomFile));

            if (groupId != null) {
                String existingGroupId = model.getGroupId();
                if (existingGroupId != null) {
                    String newGroupId = existingGroupId.replace(originalGroupId, groupId);
                    model.setGroupId(newGroupId);
                }
            }
            if (artifactId != null) {
                String existingArtifactId = model.getArtifactId();
                String newArtifactId = existingArtifactId.replace(originalArtifactId, artifactId);
                model.setArtifactId(newArtifactId);
            }
            if (version != null) {
                String existingVersion = model.getVersion();
                if (existingVersion != null) {
                    model.setVersion(version);
                }
            }

            if (model.getParent() != null) {
                String parentGroupId = model.getParent().getGroupId();
                String parentArtifactId = model.getParent().getArtifactId();
                String parentVersion = model.getParent().getVersion();

                if (parentGroupId != null) {
                        String newParentGroupId = parentGroupId.replace(originalGroupId, groupId);
                        model.getParent().setGroupId(newParentGroupId);
                }

                if (parentArtifactId != null) {
                    String newParentArtifactId = parentArtifactId.replace(originalArtifactId, artifactId);
                    model.getParent().setArtifactId(newParentArtifactId);
                }

                if (parentVersion != null) {
                    model.getParent().setVersion(version);
                }
            }

            // Save the updated Maven model
            MavenXpp3Writer writer = new MavenXpp3Writer();
            FileWriter fileWriter = new FileWriter(pomFile);
            writer.write(fileWriter, model);
            fileWriter.close();

        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
    }


    private static void visitPomFiles(File directory, Consumer<File> consumer) {
        try (Stream<Path> stream = Files.walk(Paths.get(directory.getAbsolutePath()))) {
            List<File> pomFiles = stream
            .map(Path::toFile)
            .filter(file -> file.getName().endsWith("pom.xml"))
            .collect(Collectors.toList());
            for (File pomFile : pomFiles) {
                consumer.accept(pomFile);
            }
        } catch (IOException e) {
            System.err.println("Failed to visit pom files: " + e.getMessage());
        }
    }

    private static boolean move(Path source, Path destination) throws IOException {
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path destFile = destination.resolve(source.relativize(file));
                    Files.move(file, destFile, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path destDir = destination.resolve(source.relativize(dir));
                    Files.createDirectories(destDir);
                    return FileVisitResult.CONTINUE;
                }
            });
        return true;
    }

    public static void main(String[] args) {
        CommandLine.run(new CreateFromQuickstart(), args);
    }
}

