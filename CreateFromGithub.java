//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.6.1
//DEPS org.eclipse.jgit:org.eclipse.jgit:6.5.0.202303070854-r
//DEPS org.apache.maven:maven-model:3.8.1
//DEPS org.apache.maven:maven-model-builder:3.8.1
//DEPS org.codehaus.plexus:plexus-utils:3.3.0
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


@Command(name = "from-github",
description = "Clone a GitHub project locally and clear its git configuration.",
mixinStandardHelpOptions = true)
public class CreateFromGithub implements Runnable {

    @Parameters(index = "0", description = "The owner and name of the GitHub repository, in the form 'owner/repository'.", arity = "1")
    private String repository;

    @Parameters(index = "1", description = "The coordinates of the project in the form [[GROUP-ID:]ARTIFACT-ID[:VERSION]].", arity = "0..1")
    private String coords;

    public void run() {
        String[] parts = repository.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid repository name: " + repository);
        }
        String repoName = parts[1];

        try {
            // Clone the repository
            CloneCommand cloneCommand = Git.cloneRepository()
            .setURI("https://github.com/" + repository + ".git")
            .setDirectory(new File(repoName));
            Git git = cloneCommand.call();
            Repository repository = git.getRepository();

            // Clear the Git configuration
            File dotGit = repository.getDirectory();
            File config = new File(dotGit, Constants.CONFIG);
            config.delete();


            System.out.println("Project cloned: " + repoName);

            File root = dotGit.getParentFile();
            if (coords != null && !coords.isBlank()) {
                System.out.println("Updating coordinates: " + coords);
                String newArtifactId = getArtifactId(coords);
                File newRoot = new File(root.getParentFile(), newArtifactId);
                root.renameTo(newRoot);
                updateProject(newRoot, coords);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getArtifactId(String coords) {
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

    public static void updateProject(File root, String coords) {
        // Parse the argument into groupId, artifactId, and version parts
        String[] parts = coords.split(":");
        String groupId = null;
        String artifactId = null;
        String version = null;
        switch (parts.length) {
            case 1:
            artifactId = parts[0];
            break;
            case 2:
            groupId = parts[0];
            artifactId = parts[1];
            break;
            case 3:
            groupId = parts[0];
            artifactId = parts[1];
            version = parts[2];
            break;
            default:
            throw new IllegalArgumentException("Invalid argument: " + coords);
        }

        try {
            // Load the Maven model
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader(new File(root, "pom.xml")));

            // Update the groupId, artifactId, and version
            if (groupId != null) {
                System.out.println("Setting groupId: " + groupId);
                model.setGroupId(groupId);
            }
            if (artifactId != null) {
                System.out.println("Setting artifactId: " + artifactId);
                model.setArtifactId(artifactId);
            }
            if (version != null) {
                System.out.println("Setting version: " + version);
                model.setVersion(version);
            }

            // Save the updated Maven model
            MavenXpp3Writer writer = new MavenXpp3Writer();
            FileWriter fileWriter = new FileWriter(new File(root, "pom.xml"));
            writer.write(fileWriter, model);
            fileWriter.close();

            System.out.println("Project updated: " + model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion());
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        CommandLine.run(new CreateFromGithub(), args);
    }
}

