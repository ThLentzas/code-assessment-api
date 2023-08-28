package gr.aegean.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import gr.aegean.config.AuthConfig;
import gr.aegean.config.security.SecurityConfig;
import gr.aegean.model.dto.analysis.RefreshRequest;
import gr.aegean.repository.UserRepository;
import gr.aegean.service.analysis.AnalysisService;
import gr.aegean.service.analysis.AsyncService;
import gr.aegean.config.security.JwtFilter;
import gr.aegean.exception.UnauthorizedException;
import gr.aegean.service.auth.JwtService;
import gr.aegean.service.auth.CookieService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.HttpServletRequest;


@WebMvcTest(AnalysisController.class)
@Import({JwtFilter.class,
        JwtService.class,
        AuthConfig.class,
        SecurityConfig.class})
class AnalysisControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AsyncService asyncService;
    @MockBean
    private AnalysisService analysisService;
    @MockBean
    private CookieService cookieService;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private UserRepository userRepository;
    private static final String ANALYSIS_PATH = "/api/v1/analysis";

    @Test
    void shouldReturnHTTP401WhenAnalysisIsCalledByUnauthenticatedUser() throws Exception {
        String requestBody = """
                {
                    "projectUrls": [
                        "https://github.com/repo1",
                        "https://github.com/repo2"
                    ],
                    "constraints": [
                        {
                            "qualityMetric": "CYCLOMATIC_COMPLEXITY",
                            "operator": ">",
                            "threshold": 0.85
                        }
                    ]
                }
                """;

        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenThrow(
                new UnauthorizedException("Unauthorized"));

        mockMvc.perform(post(ANALYSIS_PATH)
                        .servletPath(ANALYSIS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(asyncService);
    }

    /*
        @NullAndEmptySource only works for Strings, not for a List<String>
     */
    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP400WhenListOfProjectUrlsIsEmpty() throws Exception {
        String requestBody = """
                {
                    "projectUrls": []
                }
                """;

        mockMvc.perform(post(ANALYSIS_PATH)
                        .servletPath(ANALYSIS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(asyncService);
    }

    /*
        @NullAndEmptySource only works for Strings, not for a List<String>
     */
    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP400WhenListOfProjectUrlsIsNull() throws Exception {
        String requestBody = """
                {
                    "projectUrls": null
                }
                """;

        mockMvc.perform(post(ANALYSIS_PATH)
                        .servletPath(ANALYSIS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(asyncService);
    }

    @Test
    void shouldReturnHTTP401WhenGetAnalysisResultIsCalledByUnauthenticatedUser() throws Exception {
        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenThrow(
                new UnauthorizedException("Unauthorized"));

        mockMvc.perform(get(ANALYSIS_PATH + "/{analysisId}", 1)
                        .servletPath(ANALYSIS_PATH + "/1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(asyncService);
    }

    @Test
    void shouldReturnHTTP401WhenRefreshAnalysisResultIsCalledByUnauthenticatedUser() throws Exception {
        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenThrow(
                new UnauthorizedException("Unauthorized"));

        mockMvc.perform(put(ANALYSIS_PATH + "/{analysisId}", 1)
                        .servletPath(ANALYSIS_PATH + "/1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(asyncService);
    }

    @Test
    @WithMockUser(username = "test")
    void shouldReturnHTTP400WhenRefreshAnalysisRequestIsNull() throws Exception {
        when(analysisService.refreshAnalysisResult(any(Integer.class), any(RefreshRequest.class)))
                .thenThrow(new IllegalArgumentException("No refresh request was provided."));

        mockMvc.perform(put(ANALYSIS_PATH + "/{analysisId}", 1)
                        .servletPath(ANALYSIS_PATH + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnHTTP401WhenAnalysisResultIsCalledByUnauthenticatedUser() throws Exception {
        when(cookieService.getTokenFromCookie(any(HttpServletRequest.class))).thenThrow(
                new UnauthorizedException("Unauthorized"));

        mockMvc.perform(get(ANALYSIS_PATH + "/reports/{reportId}", 1)
                        .servletPath(ANALYSIS_PATH + "/reports/1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(asyncService);
    }
}
