# Code Assessment Service
The proposed web application is a code quality and security assessment and evaluation tool for open source software repositories hosted on GitHub. The application can obtain the addresses of the repositories from the users, and the users through the interface can set constraints and preferences regarding the assessment of features and metrics of code quality and security. The main advantage is the automation of the process as all that is needed is the initial submission of repositories and optionally constraints and preferences by the user. The application automatically detects the programming languages and related build automation tools of each project repository and performs the analysis in an automated manner, simplifying the process and reducing the margin for error. A key advantage is the flexibility of the application in terms of assessment. By giving the user the ability to set the weight he wants on specific quality and safety features as well as constraints on them, the assessment is adjusted to better align with the goals of the user. The results of the assessment will not be static because the user is given the ability to change constraints and preferences on the fly as there is no need to re-analyze the relevant project but only to update the results based on the new preferences/restrictions of the user parameters. This the rest api of the application.

![Rank Tree](https://i.imgur.com/jIw67O1.png)

### Quality Metrics

| Metric Name                        | Description                                                                                                                      |
|------------------------------------|----------------------------------------------------------------------------------------------------------------------------------|
| **Comment Rate**                   | The proportion of comments in the source code, aiding in understanding code maintainability and documentation quality            |
| **Method Size**                    | Indicates the importance of having smaller, more manageable methods for better readability and maintenance                       |
| **Duplication**                    | Represents the percentage of duplicated code lines, essential for reducing maintenance costs and errors                          |
| **Bug Severity**                   | Indicates the severity and impact of bugs, categorizing them to prioritize fixing                                                |
| **Technical Debt Ratio**           | Measures the cost of remediating issues compared to the total implementation time, aiding in code standard compliance assessment |
| **Reliability Remediation Effort** | Estimates effort to fix all reliability-related issues, offering insights into code reliability improvement                      |
| **Cyclomatic Complexity**          | Counts the number of independent paths through the code, a key metric for maintainability                                        |
| **Cognitive Complexity**           | Indicates code understandability by considering breakpoints, nesting levels, and operators                                       |
| **Vulnerability Severity**         | Categorizes security vulnerabilities by severity, essential for prioritizing security issues                                     |
| **Hotspot Priority**               | Identifies error-prone code hotspots or areas needing refactoring, focusing attention on areas of concern                        |
| **Security Remediation Effort**    | Estimates effort to fix all security issues, aiding in planning and prioritizing security enhancements                           |

Quality metrics are the leaf nodes of the tree. You can provide constraints for the above metrics in the range of [0.0 - 1.0]. Two lists will be returned: 
- **Compliant:** repositories that are compliant to all the constraints
- **Non-compliant:** repositories that are non-compliant to at list one constraint
### Quality Attributes
Every node of the tree is a quality attribute. The user can provide preference for a specific quality attribute in the range of [0.0 - 1.0]. The following rule should be followed:
* If the user provides weight for all the child nodes of a parent node then the sum of the weights must be equal to 1.0 otherwise will be considered as an invalid request
# Features
Supports a user management system with the following user operations:

* Login/Sing up
* Reset password
* Update profile properties
* View history

For the analysis:

* Analysis request with public GitHub repositories and optional constraints and preferences
* Refresh analysis results by submitting new constraints and preferences
* Copy an analysis request, the initial repositories constraints and preferences to adjust and request a new one
* Delete analysis

# Technologies
* Java 17
* Spring Boot 3.1.0
* Spring Security
* PostgreSQL
* Docker
* SonarQube
* TestContainers
* Junit5
* GreenMail
* Mockito