package gr.aegean.service;

import gr.aegean.model.analysis.QualityMetric;
import org.apache.commons.math3.util.Precision;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.*;

import gr.aegean.exception.ServerErrorException;
import gr.aegean.entity.AnalysisReport;
import gr.aegean.model.analysis.sonarqube.HotspotsReport;
import gr.aegean.model.analysis.sonarqube.IssuesReport;
import gr.aegean.model.analysis.sonarqube.QualityMetricReport;
import gr.aegean.model.analysis.sonarqube.Rule;
import gr.aegean.model.analysis.sonarqube.Severity;

import static org.awaitility.Awaitility.await;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class SonarService {
    @Value("${sonar.token}")
    private String authToken;

    public void analyzeProject(String projectKey, String projectDirectory) {
        ProcessBuilder processBuilder = new ProcessBuilder();
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

            /*
                Waiting for the analysis to end not to upload the analysis to the server.
             */
            int exitCode = process.waitFor();


        } catch (IOException | InterruptedException ioe) {
            ioe.printStackTrace();
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }
    }

    public AnalysisReport createAnalysisReport(String projectKey, Map<String, Double> languages) throws
            InterruptedException, IOException {
        /*
            Wait 4 seconds for the report to be uploaded on the server.
         */
        await().pollDelay(Duration.ofSeconds(5)).until(() -> true);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        if (!projectExists(restTemplate, entity, projectKey)) {
            throw new ServerErrorException("The server encountered an internal error and was unable to complete your " +
                    "request. Please try again later.");
        }

        IssuesReport issuesReport = fetchIssues(restTemplate, entity, projectKey);
        HotspotsReport hotspotsReport = fetchHotspots(restTemplate, entity, projectKey);
        Map<String, Rule> ruleDetails = mapRuleToRuleDetails(restTemplate, entity, issuesReport, hotspotsReport);
        EnumMap<QualityMetric, Double> qualityMetricDetails = getQualityMetrics(
                issuesReport.getIssues(),
                restTemplate,
                entity,
                projectKey);

        return new AnalysisReport(languages, issuesReport, hotspotsReport, ruleDetails, qualityMetricDetails);
    }

    private boolean projectExists(RestTemplate restTemplate, HttpEntity<String> entity, String projectKey) {
        String projectUrl = String.format("http://localhost:9000/api/projects/search?projects=%s", projectKey);

        /*
            The request to check if a project is on the server does not return 404 but an empty components array if the
             project is not on the sonarqube server.
         */
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    projectUrl,
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

    private IssuesReport fetchIssues(RestTemplate restTemplate, HttpEntity<String> entity, String projectKey) {
        int page = 1;
        int pageSize = 100;
        int totalNumberOfPages = Integer.MAX_VALUE;
        IssuesReport issuesReport = null;
        IssuesReport tmp;

        /*
            SonarQube uses pagination. It returns 100 issues per request. If we have more than a 100 we need to perform
            more than 1 request to get all the issues from a single report.
         */
        while (page <= totalNumberOfPages) {
            String issuesUrl = String.format("http://localhost:9000/api/issues/search?"
                    + "componentKeys=%s"
                    + "&p=%d"
                    + "&ps=%d", projectKey, page, pageSize);
            ResponseEntity<IssuesReport> response = restTemplate.exchange(
                    issuesUrl,
                    HttpMethod.GET,
                    entity,
                    IssuesReport.class);

            tmp = response.getBody();

            if (issuesReport != null && tmp != null) {
                issuesReport.getIssues().addAll(tmp.getIssues());
            } else {
                issuesReport = tmp;
            }

            totalNumberOfPages = (issuesReport.getPaging().total() / pageSize) + 1;
            page++;
        }

        return issuesReport;
    }

    private HotspotsReport fetchHotspots(RestTemplate restTemplate, HttpEntity<String> entity, String projectKey) {
        int page = 1;
        int pageSize = 100;
        int totalNumberOfPages = Integer.MAX_VALUE;
        HotspotsReport hotspotsReport = null;
        HotspotsReport tmp;

        /*
            SonarQube uses pagination. It returns 100 issues per request. If we have more than a 100 we need to perform
            more than 1 request to get all the issues from a single report.
         */
        while (page <= totalNumberOfPages) {
            String hotspotUrl = String.format("http://localhost:9000/api/hotspots/search?"
                    + "projectKey=%s"
                    + "&p=%d"
                    + "&ps=%d", projectKey, page, pageSize);

            ResponseEntity<HotspotsReport> response = restTemplate.exchange(
                    hotspotUrl,
                    HttpMethod.GET,
                    entity,
                    HotspotsReport.class
            );

            tmp = response.getBody();

            if (hotspotsReport != null && tmp != null) {
                hotspotsReport.getHotspots().addAll(tmp.getHotspots());
            } else {
                hotspotsReport = tmp;
            }

            totalNumberOfPages = (hotspotsReport.getPaging().total() / pageSize) + 1;
            page++;
        }

        return hotspotsReport;
    }

    private Map<String, Rule> mapRuleToRuleDetails(RestTemplate restTemplate,
                                                   HttpEntity<String> entity,
                                                   IssuesReport issuesReport,
                                                   HotspotsReport hotspotsReport) {
        Map<String, Rule> ruleDetails = new HashMap<>();

        /*
            For each HOTSPOT rule we are getting the rule details.
         */
        hotspotsReport.getHotspots().stream()
                .map(HotspotsReport.HotspotDetails::getRuleKey)
                .forEach(ruleKey -> {
                    String ruleUrl = String.format("http://localhost:9000/api/rules/show?key=%s", ruleKey);
                    ResponseEntity<Rule> response = restTemplate.exchange(
                            ruleUrl,
                            HttpMethod.GET,
                            entity,
                            Rule.class
                    );

                    ruleDetails.put(ruleKey, response.getBody());
                });

        /*
            Similar to HOTSPOTS now for every BUG, CODE SMELL, VULNERABILITY rule we are getting the rule details.
         */
        issuesReport.getIssues().stream()
                .map(IssuesReport.IssueDetails::getRule)
                .forEach(rule -> {
                    String ruleUrl = String.format("http://localhost:9000/api/rules/show?key=%s", rule);
                    ResponseEntity<Rule> response = restTemplate.exchange(
                            ruleUrl,
                            HttpMethod.GET,
                            entity,
                            Rule.class
                    );

                    ruleDetails.put(rule, response.getBody());
                });

        return ruleDetails;
    }

    private EnumMap<QualityMetric, Double> getQualityMetrics(List<IssuesReport.IssueDetails> issueDetails,
                                                        RestTemplate restTemplate,
                                                        HttpEntity<String> entity,
                                                        String projectKey) {
        EnumMap<QualityMetric, Double> metricDetails = new EnumMap<>(QualityMetric.class);

        double reliabilityValue = measureReliability(issueDetails);
        metricDetails.put(QualityMetric.RELIABILITY, reliabilityValue);

        double securityValue = measureSecurity(issueDetails);
        metricDetails.put(QualityMetric.SECURITY, securityValue);

        double maintainabilityValue = fetchMaintainability(restTemplate, entity, projectKey);
        metricDetails.put(QualityMetric.MAINTAINABILITY, maintainabilityValue);

        List<Double> complexityValue = fetchComplexity(restTemplate, entity, projectKey);
        double cognitiveComplexityValue = complexityValue.get(0);
        double cyclomaticComplexityValue = complexityValue.get(1);
        metricDetails.put(QualityMetric.COGNITIVE_COMPLEXITY, cognitiveComplexityValue);
        metricDetails.put(QualityMetric.CYCLOMATIC_COMPLEXITY, cyclomaticComplexityValue);

        double technicalDeptValue = fetchTechnicalDept(restTemplate, entity, projectKey);
        metricDetails.put(QualityMetric.TECHNICAL_DEPT, technicalDeptValue);

        return metricDetails;
    }


    private double measureAttribute(List<IssuesReport.IssueDetails> issueDetails, String issueType) {
        double attributeValue;

        List<Severity> sortedIssues = issueDetails.stream()
                .filter(issue -> issue.getType().equals(issueType))
                .map(IssuesReport.IssueDetails::getSeverity)
                .sorted()
                .toList();

        /*
            If no Bugs were found the attributeValue has the max value.
        */
        if (sortedIssues.isEmpty()) {
            attributeValue = 1.0;

            return attributeValue;
        }

        attributeValue = sortedIssues.get(0).getValue();

        for (int i = 1; i < sortedIssues.size(); i++) {
            switch (sortedIssues.get(i)) {
                case BLOCKER -> attributeValue = attributeValue - attributeValue * 5 / 100;
                case CRITICAL -> attributeValue = attributeValue - attributeValue * 4 / 100;
                case MAJOR -> attributeValue = attributeValue - attributeValue * 3 / 100;
                case MINOR -> attributeValue = attributeValue - attributeValue * 2 / 100;
                case INFO -> attributeValue = attributeValue - attributeValue * 1 / 100;
                default ->
                        throw new ServerErrorException("The server encountered an internal error and was unable to " +
                                "complete your request. Please try again later.");
            }
        }
        /*
            Only 1 decimal digit.
        */
        attributeValue = Precision.round(attributeValue, 1);

        return attributeValue;
    }

    private double measureReliability(List<IssuesReport.IssueDetails> issueDetails) {
        return measureAttribute(issueDetails, "BUG");
    }

    private double measureSecurity(List<IssuesReport.IssueDetails> issueDetails) {
        return measureAttribute(issueDetails, "VULNERABILITY");
    }

    /*
        toDO:For the color on the frontend subtract the technical dept ratio from 100
     */
    private double fetchMaintainability(RestTemplate restTemplate, HttpEntity<String> entity, String projectKey) {
        String maintainabilityUrl = String.format("http://localhost:9000/api/measures/search?"
                + "projectKeys=%s"
                + "&metricKeys=%s", projectKey, "sqale_debt_ratio");

        ResponseEntity<QualityMetricReport> response = restTemplate.exchange(
                maintainabilityUrl,
                HttpMethod.GET,
                entity,
                QualityMetricReport.class);

        QualityMetricReport qualityMetricReportDetails = response.getBody();

        return qualityMetricReportDetails.getMeasures().get(0).getValue();
    }

    private List<Double> fetchComplexity(RestTemplate restTemplate, HttpEntity<String> entity, String projectKey) {
        String complexityUrl = String.format("http://localhost:9000/api/measures/search?"
                + "projectKeys=%s"
                + "&metricKeys=%s", projectKey, "cognitive_complexity,complexity");

        ResponseEntity<QualityMetricReport> response = restTemplate.exchange(
                complexityUrl,
                HttpMethod.GET,
                entity,
                QualityMetricReport.class);

        QualityMetricReport qualityMetricReportDetails = response.getBody();

        double cognitiveComplexityValue = qualityMetricReportDetails.getMeasures().get(0).getValue();
        double cyclomaticComplexityValue = qualityMetricReportDetails.getMeasures().get(1).getValue();

        return List.of(cognitiveComplexityValue, cyclomaticComplexityValue);
    }

    /*
        Value is in minutes. Convert it to days, hour, minutes in the front end
     */
    private double fetchTechnicalDept(RestTemplate restTemplate, HttpEntity<String> entity, String projectKey) {
        String technicalDeptUrl = String.format("http://localhost:9000/api/measures/search?"
                + "projectKeys=%s"
                + "&metricKeys=%s", projectKey, "sqale_index");

        ResponseEntity<QualityMetricReport> response = restTemplate.exchange(
                technicalDeptUrl,
                HttpMethod.GET,
                entity,
                QualityMetricReport.class);

        QualityMetricReport qualityMetricReportDetails = response.getBody();

        return qualityMetricReportDetails.getMeasures().get(0).getValue();
    }
}

