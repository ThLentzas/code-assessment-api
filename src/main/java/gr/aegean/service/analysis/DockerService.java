package gr.aegean.service.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import gr.aegean.exception.ServerErrorException;


@Service
public class DockerService {
    @Value("${sonar.token}")
    private String authToken;

    public String createLinguistContainer(String path) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(
                "docker",
                "run",
                "--rm",
                "-v",
                path + ":/code",
                "linguist",
                "github-linguist",
                "/code"
        );

        StringBuilder reportBuilder = new StringBuilder();

        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                reportBuilder.append(line).append("\n");
            }
        } catch (IOException ioe) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }

        return reportBuilder.toString();
    }

    public void analyzeMavenProject(String projectKey, String projectPath) throws
            IOException,
            InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();

        createDockerFile(projectKey, projectPath);
        String dockerImage = buildDockerImage(projectPath, processBuilder);
        runDockerContainer(dockerImage, projectPath, processBuilder);
    }

    private String buildDockerImage(String projectPath, ProcessBuilder processBuilder) throws
            IOException,
            InterruptedException {
        /*
            Splitting with the escape character which is also the file separator in Windows
         */
        String dockerImage = projectPath.split("\\\\")[3];

        processBuilder.command(
                "docker",
                "build",
                "-t",
                dockerImage,
                "."
        );

         /*
            Setting the directory of the command execution to be the projects directory, so we can use .
         */
        processBuilder.directory(new File(projectPath));
        Process process = processBuilder.start();
        process.waitFor();

        return dockerImage;
    }

    private void runDockerContainer(String dockerImage, String projectPath, ProcessBuilder processBuilder) throws
            IOException {
        String containerName = projectPath.split("\\\\")[3];

        processBuilder.command(
                "docker",
                "run",
                "--rm",
                "--name",
                containerName,
                "--network",
                "code-assessment-net",
                dockerImage
        );
        processBuilder.start();
    }

    private void createDockerFile(String projectKey, String projectPath) throws IOException {
        Path dockerfilePath = Paths.get(projectPath, "Dockerfile");
        String dockerfileContent = String.format("""
                    FROM maven:3.8.7-openjdk-18-slim
                    WORKDIR /app
                    COPY . .
                    CMD sh -c 'mvn clean verify sonar:sonar \
                    -Dmaven.test.skip=true \
                    -Dsonar.host.url=http://sonarqube:9000 \
                    -Dsonar.projectKey=%s \
                    -Dsonar.token=%s;'
                """, projectKey, authToken);

        /*
            1st argument: the path to write the docker file. The root directory of the project.
            2nd argument: the content to write in the file.
            If there is a dockerfile named "Dockerfile" it will be overwritten by ours.
         */
        Files.write(dockerfilePath, dockerfileContent.getBytes());
    }
}
