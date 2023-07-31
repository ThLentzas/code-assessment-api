package gr.aegean.service.analysis;

import gr.aegean.exception.ServerErrorException;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
public class DockerService {

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

    public void analyzeMavenProject(String projectPath, ProcessBuilder processBuilder) throws IOException, InterruptedException {
        String dockerImage = buildDockerImage(projectPath, processBuilder);
        runDockerContainer(dockerImage, projectPath, processBuilder);

    }

    private String buildDockerImage(String projectPath, ProcessBuilder processBuilder) throws
            IOException, InterruptedException {
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
}
