package gr.aegean.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import gr.aegean.exception.ServerErrorException;
import gr.aegean.model.analysis.sonarqube.SonarResponse;
import gr.aegean.model.analysis.sonarqube.Hotspot;
import gr.aegean.model.analysis.sonarqube.Rule;
import gr.aegean.model.analysis.sonarqube.Issue;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.*;

import static org.awaitility.Awaitility.await;

@Service
@RequiredArgsConstructor
public class SonarService {
    private final ProcessBuilder processBuilder;
    @Value("${sonar.token}")
    private String authToken;

    public void analyzeProject(String projectKey, String projectDirectory) {
        processBuilder.command("sonar-scanner.bat",
                "-Dsonar.projectKey=" + projectKey,
                "-Dsonar.sources=.",
                "-Dsonar.host.url=http://localhost:9000",
                "-Dsonar.login=" + authToken
        );

        /*
            Setting the directory of the command execution to be the projects directory, so we can use
            sources=.
         */
        try {
            processBuilder.directory(new File(projectDirectory));
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            System.out.println("\nExited with error code : " + exitCode);

        } catch (IOException | InterruptedException ioe) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }
    }

    public void fetchReport(String projectKey) throws InterruptedException, IOException {
        /*
            Wait 3 seconds for the report to be uploaded on the server.
         */
        await().pollDelay(Duration.ofSeconds(3)).until(() -> true);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        if(!projectExists(restTemplate, entity, projectKey)) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }

        int page = 1;
        int pageSize = 100;
        int totalNumberOfPages = Integer.MAX_VALUE;
        SonarResponse sonarResponse = null;
        SonarResponse tmp;

         /*
            SonarQube uses pagination. It returns 100 issues per request. If we have more than a 100 we need to perform
            more than 1 request to get all the issues from a single report.
         */
        while (page <= totalNumberOfPages) {
            String sonarUrl = "http://localhost:9000/api/issues/search?"
                    + "componentKeys=" + projectKey
                    + "&p=" + page
                    + "&ps=" + pageSize;
            ResponseEntity<SonarResponse> response = restTemplate.exchange(
                    sonarUrl,
                    HttpMethod.GET,
                    entity,
                    SonarResponse.class);

            tmp = response.getBody();

            if (sonarResponse != null && tmp != null) {
                sonarResponse.getIssues().addAll(tmp.getIssues());
            } else {
                sonarResponse = tmp;
            }

            totalNumberOfPages = (sonarResponse.getTotal() / pageSize) + 1;
            page++;
        }

        //toDO: Hotspots also need pagination
        String sonarURL = "http://localhost:9000/api/hotspots/search?projectKey=" + projectKey;
        ResponseEntity<Hotspot> response3 = restTemplate.exchange(
                sonarURL,
                HttpMethod.GET,
                entity,
                Hotspot.class
        );

        Hotspot hotspot = response3.getBody();
        Map<String, Rule> ruleDetails = new HashMap<>();

        /*
            For each hotspot rule we are getting the rule details.
         */
        hotspot.getHotspots().stream()
                .map(Hotspot.HotspotDetails::getRuleKey)
                .forEach(ruleKey -> {
                    String ruleUrl = "http://localhost:9000/api/rules/show?key=" + ruleKey;
                    ResponseEntity<Rule> response4 = restTemplate.exchange(
                            ruleUrl,
                            HttpMethod.GET,
                            entity,
                            Rule.class
                    );

                    ruleDetails.put(ruleKey, response4.getBody());
                });

        /*
            Similar to before now for every BUG, CODE SMELL, VULNERABILITY rule we are getting the rule details.
         */
        sonarResponse.getIssues().stream()
                .map(Issue::getRule)
                .forEach(rule -> {
                    String ruleUrl = "http://localhost:9000/api/rules/show?key=" + rule;
                    ResponseEntity<Rule> response4 = restTemplate.exchange(
                            ruleUrl,
                            HttpMethod.GET,
                            entity,
                            Rule.class
                    );

                    ruleDetails.put(rule, response4.getBody());
                });

        System.out.println(ruleDetails);
    }

    private boolean projectExists(RestTemplate restTemplate, HttpEntity<String> entity, String projectKey) {
        String sonarUrl = "http://localhost:9000/api/projects/search?projects=" + projectKey;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    sonarUrl,
                    HttpMethod.GET,
                    entity,
                    String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new ServerErrorException("The server encountered an internal error and was unable to complete " +
                        "your request. Please try again later.");
            }

            JSONObject responseObject = new JSONObject(response.getBody());
            JSONArray components = responseObject.getJSONArray("components");

            return components.length() != 0;
        } catch (JSONException e) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }
    }
}

