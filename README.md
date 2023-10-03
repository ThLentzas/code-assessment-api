# Code Assessment Service
Automate and personalize the evaluation of code quality and security for open-source repositories hosted on GitHub with the Code Assessment Service.

![Rank Tree](https://i.imgur.com/jIw67O1.png)

## Table of Contents
- [Overview](#overview)
- [Quality Metrics](#quality-metrics)
- [Quality Attributes](#quality-attributes)
- [Features](#features)
- [Technologies](#technologies)

## Overview
Easily submit GitHub repository addresses and set tailored constraints and preferences for a comprehensive code assessment. The service offers real-time adjustments of assessment parameters, delivering dynamic and customized results. [UML Diagrams & Testing Documentation](docs) are available for a deeper dive into the architecture.

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

Quality metrics are the leaf nodes of the tree. You can provide constraints for the above metrics in the range of [0.0 - 1.0]. The repositories will be filtered in two lists: 
- **Compliant:** repositories that are compliant to all the constraints
- **Non-compliant:** repositories that are non-compliant to at least one constraint
### Quality Attributes
Every node of the tree is a quality attribute. The user can provide weight for a specific quality attribute in the range of [0.0 - 1.0]. The following rule should be followed:
* If the user provides weight for all the child nodes of a parent node then the sum of the weights must be equal to 1.0 otherwise will be considered as an invalid request

Note: If the user did not provide any weights, then weights will be distributed dynamically to all nodes in the tree
# Features
Supports a user management system with the following user operations:

* Login/Sing up
* Reset password
* Update profile
* View history
* Delete account

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
* Thymeleaf