package gr.aegean.service.analysis;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.aegean.exception.ServerErrorException;
import gr.aegean.entity.AnalysisReport;
import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.sonarqube.HotspotsReport;
import gr.aegean.model.analysis.sonarqube.IssuesReport;
import gr.aegean.model.analysis.sonarqube.QualityMetricReport;
import gr.aegean.model.analysis.sonarqube.Rule;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static org.awaitility.Awaitility.await;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class SonarService {
    @Value("${sonar.token}")
    private String authToken;
    @Value("${sonar.baseUrl}")
    private String baseUrl;
    private static final Logger LOG = LoggerFactory.getLogger(SonarService.class);
    private static final String SERVER_ERROR_MSG = "The server encountered an internal error and was unable to " +
            "complete your request. Please try again later";

    public void analyzeProject(String projectKey, String projectDirectory) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("sonar-scanner.bat",
                "-Dsonar.projectKey=" + projectKey,
                "-Dsonar.sources=.",
                "-Dsonar.host.url=http://localhost:9000",
                "-Dsonar.token=" + authToken
        );

        /*
            Setting the directory of the command execution to be the projects directory, so we can use
            sources=.
         */
        try {
            processBuilder.directory(new File(projectDirectory));
            Process process = processBuilder.start();

            /*
                Each process builder has an associated output buffer. We have to keep reading from those buffers, as
                the process writes enough data to them, the buffers can fill up, and the process will block, waiting for
                space to become available.
             */
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                LOG.info(line);
            }
            /*
                Waiting for the analysis to end, not to upload the analysis to the server.
             */
            process.waitFor();
        } catch (IOException ioe) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ServerErrorException(SERVER_ERROR_MSG);
        }
    }

    public AnalysisReport fetchAnalysisReport(String projectKey) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        /*
            Wait for the key to be created on the server.
         */
        while (!projectExists(restTemplate, entity, projectKey)) {
            await().pollDelay(Duration.ofSeconds(8)).until(() -> true);
        }

        /*
            Wait for the project to be uploaded.
         */
        await().pollDelay(Duration.ofSeconds(8)).until(() -> true);

        IssuesReport issuesReport = fetchIssues(restTemplate, entity, projectKey);
        HotspotsReport hotspotsReport = fetchHotspots(restTemplate, entity, projectKey);
        Map<String, Rule> rulesDetails = mapRuleToRuleDetails(restTemplate, entity, issuesReport, hotspotsReport);
        Map<QualityMetric, Double> qualityMetricReport = getQualityMetrics(
                restTemplate,
                entity,
                projectKey);

        return new AnalysisReport(issuesReport, hotspotsReport, rulesDetails, qualityMetricReport);
    }

    private boolean projectExists(RestTemplate restTemplate, HttpEntity<String> entity, String projectKey) {
        String projectUrl = String.format("%s/projects/search?projects=%s", baseUrl, projectKey);

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
                throw new ServerErrorException(SERVER_ERROR_MSG);
            }

            JSONObject responseObject = new JSONObject(response.getBody());
            JSONArray components = responseObject.getJSONArray("components");

            return components.length() != 0;
        } catch (JSONException e) {
            throw new ServerErrorException(SERVER_ERROR_MSG);
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
            String issuesUrl = String.format("%s/issues/search?componentKeys=%s&p=%d&ps=%d",
                    baseUrl,
                    projectKey,
                    page,
                    pageSize);
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

            totalNumberOfPages = (issuesReport.getIssues().size() / pageSize) + 1;
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
            String hotspotUrl = String.format("%s/hotspots/search?projectKey=%s&p=%d&ps=%d",
                    baseUrl,
                    projectKey,
                    page,
                    pageSize);
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

            totalNumberOfPages = (hotspotsReport.getHotspots().size() / pageSize) + 1;
            page++;
        }

        return hotspotsReport;
    }

    private Map<String, Rule> mapRuleToRuleDetails(RestTemplate restTemplate,
                                                   HttpEntity<String> entity,
                                                   IssuesReport issuesReport,
                                                   HotspotsReport hotspotsReport) {
        Map<String, Rule> rulesDetails = new HashMap<>();

        /*
            For each HOTSPOT rule we are getting the rule details.
         */
        hotspotsReport.getHotspots().stream()
                .map(HotspotsReport.HotspotDetails::getRuleKey)
                .forEach(ruleKey -> {
                    String ruleUrl = String.format("%s/rules/show?key=%s", baseUrl, ruleKey);
                    ResponseEntity<Rule> response = restTemplate.exchange(
                            ruleUrl,
                            HttpMethod.GET,
                            entity,
                            Rule.class
                    );

                    rulesDetails.put(ruleKey, response.getBody());
                });

        /*
            Similar to HOTSPOTS now for every BUG, CODE SMELL, VULNERABILITY rule we are getting the rule details.
         */
        issuesReport.getIssues().stream()
                .map(IssuesReport.IssueDetails::getRule)
                .forEach(rule -> {
                    String ruleUrl = String.format("%s/rules/show?key=%s", baseUrl, rule);
                    ResponseEntity<Rule> response = restTemplate.exchange(
                            ruleUrl,
                            HttpMethod.GET,
                            entity,
                            Rule.class
                    );

                    rulesDetails.put(rule, response.getBody());
                });

        return rulesDetails;
    }

    private Map<QualityMetric, Double> getQualityMetrics(RestTemplate restTemplate,
                                                             HttpEntity<String> entity,
                                                             String projectKey) {
        Map<QualityMetric, Double> metricsReport = new EnumMap<>(QualityMetric.class);

        for(QualityMetric metric: QualityMetric.values()) {
            double value = fetchQualityMetricValue(restTemplate, entity, projectKey, metric);
            metricsReport.put(metric, value);
        }

        return metricsReport;
    }

    /*
        For BUG_SEVERITY, VULNERABILITY_SEVERITY, HOTSPOT_PRIORITY the initial value will be 0 because we don't fetch
        any value from Sonarqube and will get the actual value when we apply the relative utf.
     */
    private double fetchQualityMetricValue(RestTemplate restTemplate,
                                           HttpEntity<String> entity,
                                           String projectKey,
                                           QualityMetric metric) {
        return switch (metric) {
            case LINES_OF_CODE -> fetchLinesOfCode(restTemplate, entity, projectKey);
            case COMMENT_RATE -> fetchCommentRate(restTemplate, entity, projectKey);
            case METHOD_SIZE -> fetchMethodSize(restTemplate, entity, projectKey);
            case DUPLICATION -> fetchDuplication(restTemplate, entity, projectKey);
            case TECHNICAL_DEBT_RATIO -> fetchTechnicalDebtRatio(restTemplate, entity, projectKey);
            case RELIABILITY_REMEDIATION_EFFORT -> fetchReliabilityRemediationEffort(restTemplate, entity, projectKey);
            case COGNITIVE_COMPLEXITY -> fetchCognitiveComplexity(restTemplate, entity, projectKey);
            case CYCLOMATIC_COMPLEXITY -> fetchCyclomaticComplexity(restTemplate, entity, projectKey);
            case SECURITY_REMEDIATION_EFFORT -> fetchSecurityRemediationEffort(restTemplate, entity, projectKey);
            case BUG_SEVERITY, VULNERABILITY_SEVERITY, HOTSPOT_PRIORITY -> 0.0;
        };
    }

    private double fetchCommentRate(RestTemplate restTemplate, HttpEntity<String> entity, String projectKey) {
        String commentRateUrl = String.format("%s/measures/search?projectKeys=%s"
                + "&metricKeys=comment_lines_density", baseUrl, projectKey);
        ResponseEntity<QualityMetricReport> response = restTemplate.exchange(
                commentRateUrl,
                HttpMethod.GET,
                entity,
                QualityMetricReport.class);

        QualityMetricReport qualityMetricReport = response.getBody();

        return qualityMetricReport.getMeasures().get(0).getValue();
    }

    private double fetchMethodSize(RestTemplate restTemplate, HttpEntity<String> entity, String projectKey) {
        String methodSizeUrl = String.format("%s/measures/search?projectKeys=%s&metricKeys=functions,ncloc",
                baseUrl,
                projectKey);
        ResponseEntity<QualityMetricReport> response = restTemplate.exchange(
                methodSizeUrl,
                HttpMethod.GET,
                entity,
                QualityMetricReport.class);

        QualityMetricReport qualityMetricReport = response.getBody();
        double totalMethods = qualityMetricReport.getMeasures().get(0).getValue();
        double linesOfCode = qualityMetricReport.getMeasures().get(1).getValue();

        return linesOfCode / totalMethods;
    }

    private double fetchDuplication(RestTemplate restTemplate, HttpEntity<String> entity, String projectKey) {
        String duplicationUrl = String.format("%s/measures/search?projectKeys=%s"
                + "&metricKeys=duplicated_lines_density", baseUrl, projectKey);
        ResponseEntity<QualityMetricReport> response = restTemplate.exchange(
                duplicationUrl,
                HttpMethod.GET,
                entity,
                QualityMetricReport.class);

        QualityMetricReport qualityMetricReport = response.getBody();

        /*
            We are getting a percentage back and converting to the [0.0, 1.0] range by dividing with 100.
         */
        return qualityMetricReport.getMeasures().get(0).getValue() / 100;
    }

    private double fetchTechnicalDebtRatio(RestTemplate restTemplate, HttpEntity<String> entity, String projectKey) {
        String technicalDebtRatioUrl = String.format("%s/measures/search?projectKeys=%s&metricKeys=sqale_debt_ratio",
                baseUrl,
                projectKey);
        ResponseEntity<QualityMetricReport> response = restTemplate.exchange(
                technicalDebtRatioUrl,
                HttpMethod.GET,
                entity,
                QualityMetricReport.class);

        QualityMetricReport qualityMetricReport = response.getBody();

        /*
            We are getting a percentage back and converting to the [0.0, 1.0] range by dividing with 100.
         */
        return qualityMetricReport.getMeasures().get(0).getValue() / 100;
    }

    private double fetchReliabilityRemediationEffort(RestTemplate restTemplate,
                                                     HttpEntity<String> entity,
                                                     String projectKey) {
        String reliabilityRemediationEffortUrl = String.format("%s/measures/search?projectKeys=%s"
                + "&metricKeys=reliability_remediation_effort", baseUrl, projectKey);
        ResponseEntity<QualityMetricReport> response = restTemplate.exchange(
                reliabilityRemediationEffortUrl,
                HttpMethod.GET,
                entity,
                QualityMetricReport.class);

        QualityMetricReport qualityMetricReport = response.getBody();

        return qualityMetricReport.getMeasures().get(0).getValue();
    }

    private double fetchCognitiveComplexity(RestTemplate restTemplate, HttpEntity<String> entity, String projectKey) {
        String cognitiveComplexityUrl = String.format("%s/measures/search?projectKeys=%s"
                + "&metricKeys=cognitive_complexity", baseUrl, projectKey);
        ResponseEntity<QualityMetricReport> response = restTemplate.exchange(
                cognitiveComplexityUrl,
                HttpMethod.GET,
                entity,
                QualityMetricReport.class);

        QualityMetricReport qualityMetricReport = response.getBody();

        return qualityMetricReport.getMeasures().get(0).getValue();
    }

    private double fetchCyclomaticComplexity(RestTemplate restTemplate, HttpEntity<String> entity, String projectKey) {
        String cyclomaticComplexityUrl = String.format("%s/measures/search?projectKeys=%s"
                + "&metricKeys=complexity", baseUrl, projectKey);
        ResponseEntity<QualityMetricReport> response = restTemplate.exchange(
                cyclomaticComplexityUrl,
                HttpMethod.GET,
                entity,
                QualityMetricReport.class);

        QualityMetricReport qualityMetricReport = response.getBody();

        return qualityMetricReport.getMeasures().get(0).getValue();
    }

    private double fetchSecurityRemediationEffort(RestTemplate restTemplate,
                                                  HttpEntity<String> entity,
                                                  String projectKey) {
        String fetchSecurityRemediationEffortUrl = String.format("%s/measures/search?projectKeys=%s"
                + "&metricKeys=security_remediation_effort", baseUrl, projectKey);
        ResponseEntity<QualityMetricReport> response = restTemplate.exchange(
                fetchSecurityRemediationEffortUrl,
                HttpMethod.GET,
                entity,
                QualityMetricReport.class);

        QualityMetricReport qualityMetricReport = response.getBody();

        return qualityMetricReport.getMeasures().get(0).getValue();
    }

    private double fetchLinesOfCode(RestTemplate restTemplate,
                                    HttpEntity<String> entity,
                                    String projectKey) {
        String linesOfCodeUrl = String.format("%s/measures/search?projectKeys=%s&metricKeys=ncloc",
                baseUrl,
                projectKey);
        ResponseEntity<QualityMetricReport> response = restTemplate.exchange(
                linesOfCodeUrl,
                HttpMethod.GET,
                entity,
                QualityMetricReport.class);

        QualityMetricReport qualityMetricReport = response.getBody();

        return qualityMetricReport.getMeasures().get(0).getValue();
    }
}

