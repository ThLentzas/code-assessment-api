package gr.aegean.service.assessment;

import gr.aegean.entity.Preference;
import gr.aegean.model.analysis.quality.TreeNode;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;


@Service
public class TreeService {

    public TreeNode buildTree() {
        TreeNode root = new TreeNode("Rank");
        TreeNode quality = new TreeNode("QUALITY");
        TreeNode security = new TreeNode("SECURITY");
        TreeNode comprehension = new TreeNode("COMPREHENSION");
        TreeNode simplicity = new TreeNode("SIMPLICITY");
        TreeNode maintainability = new TreeNode("MAINTAINABILITY");
        TreeNode reliability = new TreeNode("RELIABILITY");
        TreeNode complexity = new TreeNode("COMPLEXITY");
        TreeNode commentRate = new TreeNode("COMMENT_RATE");
        TreeNode methodSize = new TreeNode("METHOD_SIZE");
        TreeNode duplication = new TreeNode("DUPLICATION");
        TreeNode technicalDebtRatio = new TreeNode("TECHNICAL_DEBT_RATIO");
        TreeNode bugSeverity = new TreeNode("BUG_SEVERITY");
        TreeNode reliabilityRemediationEffort = new TreeNode("RELIABILITY_REMEDIATION_EFFORT");
        TreeNode cyclomaticComplexity = new TreeNode("CYCLOMATIC_COMPLEXITY");
        TreeNode cognitiveComplexity = new TreeNode("COGNITIVE_COMPLEXITY");
        TreeNode vulnerabilitySeverity = new TreeNode("VULNERABILITY_SEVERITY");
        TreeNode hotSpotPriority = new TreeNode("HOTSPOT_PRIORITY");
        TreeNode securityRemediationEffort = new TreeNode("SECURITY_REMEDIATION_EFFORT");

        addChildren(root, List.of(quality, security));
        addChildren(quality, List.of(comprehension, simplicity, maintainability, reliability, complexity));
        addChildren(security, List.of(vulnerabilitySeverity, hotSpotPriority, securityRemediationEffort));
        addChildren(comprehension, List.of(commentRate));
        addChildren(simplicity, List.of(methodSize));
        addChildren(maintainability, List.of(duplication, technicalDebtRatio));
        addChildren(reliability, List.of(bugSeverity, reliabilityRemediationEffort));
        addChildren(complexity, List.of(cyclomaticComplexity, cognitiveComplexity));

        return root;
    }

    public void validateChildNodesWeightsSum(TreeNode node, List<Preference> preferences) {
        int childNodesWithWeight = 0;
        double sum = 0.0;
        Optional<Preference> matchingPreference = Optional.empty();

        for (TreeNode child : node.getChildren()) {
            matchingPreference = preferences.stream()
                    .filter(preference -> preference.getQualityAttribute().name().equals(child.getName()))
                    .findFirst();

            if(matchingPreference.isPresent()) {
                childNodesWithWeight++;
                sum+=matchingPreference.get().getWeight();

                /*
                    Case: provided sum of weights for the child nodes is greater than 1.0
                 */
                if (sum > 1.0) {
                    throw new IllegalArgumentException("The combined weights of " +
                            matchingPreference.get()
                                    .getQualityAttribute()
                                    .toString() + " node's child nodes must not exceed 1");
                }
            }
        }

        /*
            Case: child nodes of a parent node have all defined weight, meaning the user provided weight for all the
            child nodes, and the sum is not equal to 1.0
         */
        if((childNodesWithWeight == node.getChildren().size()) && sum != 0.0) {
            throw new IllegalArgumentException("The combined weights of all " +
                    matchingPreference.get()
                            .getQualityAttribute()
                            .toString() + " node's child nodes must not be less than 1");
        }

    }

    public boolean isLeafNode(TreeNode node) {
        return node.getChildren().isEmpty();
    }

    private void addChildren(TreeNode parent, List<TreeNode> children) {
        for(TreeNode child : children) {
            parent.addChild(child);
        }
    }
}
