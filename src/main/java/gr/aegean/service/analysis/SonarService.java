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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import gr.aegean.exception.ServerErrorException;
import gr.aegean.entity.AnalysisReport;
import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.sonarqube.HotspotsReport;
import gr.aegean.model.analysis.sonarqube.IssuesReport;
import gr.aegean.model.analysis.sonarqube.QualityMetricReport;

import lombok.RequiredArgsConstructor;

import static org.awaitility.Awaitility.await;


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
        processBuilder.command(
                "sonar-scanner.bat",
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
        Map<QualityMetric, Double> qualityMetricReport = getQualityMetrics(
                restTemplate,
                entity,
                projectKey);

        return new AnalysisReport(issuesReport, hotspotsReport, qualityMetricReport);
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
            String issuesUrl = String.format(
                    "%s/issues/search?componentKeys=%s&p=%d&ps=%d",
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
            String hotspotUrl = String.format(
                    "%s/hotspots/search?projectKey=%s&p=%d&ps=%d",
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

    /*
        For BUG_SEVERITY, VULNERABILITY_SEVERITY, HOTSPOT_PRIORITY the initial value will be 0 because we don't fetch
        any value from Sonarqube and will get the actual value when we apply the relative utf.
     */
    private Map<QualityMetric, Double> getQualityMetrics(RestTemplate restTemplate,
                                                         HttpEntity<String> entity,
                                                         String projectKey) {
        Map<String, Double> metrics = fetchQualityMetricValues(restTemplate, entity, projectKey);
        Map<QualityMetric, Double> metricsReport = new EnumMap<>(QualityMetric.class);

        metricsReport.put(QualityMetric.COMMENT_RATE, metrics.get("comment_lines_density"));
        metricsReport.put(QualityMetric.METHOD_SIZE, metrics.get("ncloc") / metrics.get("functions"));
        metricsReport.put(QualityMetric.DUPLICATION, metrics.get("duplicated_lines_density") / 100);
        metricsReport.put(QualityMetric.BUG_SEVERITY, 0.0);
        metricsReport.put(QualityMetric.TECHNICAL_DEBT_RATIO, metrics.get("sqale_debt_ratio") / 100);
        metricsReport.put(QualityMetric.RELIABILITY_REMEDIATION_EFFORT, metrics.get("reliability_remediation_effort"));
        metricsReport.put(QualityMetric.CYCLOMATIC_COMPLEXITY, metrics.get("complexity"));
        metricsReport.put(QualityMetric.COGNITIVE_COMPLEXITY, metrics.get("cognitive_complexity"));
        metricsReport.put(QualityMetric.VULNERABILITY_SEVERITY, 0.0);
        metricsReport.put(QualityMetric.HOTSPOT_PRIORITY, 0.0);
        metricsReport.put(QualityMetric.SECURITY_REMEDIATION_EFFORT, metrics.get("security_remediation_effort"));
        metricsReport.put(QualityMetric.LINES_OF_CODE, metrics.get("ncloc"));

        return metricsReport;
    }

    private Map<String, Double> fetchQualityMetricValues(RestTemplate restTemplate,
                                                         HttpEntity<String> entity,
                                                         String projectKey) {
        final String[] METRICS = new String[]{
                "comment_lines_density",
                "functions",
                "duplicated_lines_density",
                "sqale_debt_ratio",
                "reliability_remediation_effort",
                "cognitive_complexity",
                "complexity",
                "security_remediation_effort",
                "ncloc"
        };
        String metricKeys = String.join(",", METRICS);
        String metricsUrl = String.format(
                "%s/measures/search?projectKeys=%s&metricKeys=%s",
                baseUrl,
                projectKey,
                metricKeys);

        ResponseEntity<QualityMetricReport> response = restTemplate.exchange(
                metricsUrl,
                HttpMethod.GET,
                entity,
                QualityMetricReport.class);
        QualityMetricReport qualityMetricReport = response.getBody();

        Map<String, Double> metrics = new HashMap<>();
        for (QualityMetricReport.Measures measures : qualityMetricReport.getMeasures()) {
            metrics.put(measures.getMetric(), measures.getValue());
        }

        return metrics;
    }
}

