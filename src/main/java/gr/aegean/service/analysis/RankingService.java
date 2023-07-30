package gr.aegean.service.analysis;

import gr.aegean.entity.Preference;
import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.quality.TreeNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class RankingService {
    private final TreeBuildingService treeBuildingService;

    public double rankTree(EnumMap<QualityMetric, Double> qualityMetricsReport, List<Preference> preferences) {
        TreeNode root = treeBuildingService.buildTree();

        assignWeight(root, preferences);
        assignLeafNodeValue(root, qualityMetricsReport);
        assignParentNodeValue(root);

        return root.getValue();
    }

    private void assignWeight(TreeNode node, List<Preference> preferences) {
        /*
            Leaf node
         */
        if (isLeafNode(node)) {
            return;
        }

        /*
            If there is only 1 child node then it has the max weight.
         */
        if(node.getChildren().size() == 1) {
            node.getChildren().get(0).setWeight(1.0);

            return;
        }

        List<TreeNode> nodesWithoutWeight = new ArrayList<>();
        double sum = 1.0;

        for (TreeNode child : node.getChildren()) {
            Optional<Preference> matchingPreference = preferences.stream()
                    .filter(preference -> preference.getQualityAttribute().name().equals(child.getName()))
                    .findFirst();

            /*
                Case 1: The user submitted weight for the specific node.
                Case 2: The user did not submit weight for the specific node.
             */
            if (matchingPreference.isPresent()) {
                child.setWeight(matchingPreference.get().getWeight());
                sum -= child.getWeight();
            } else {
                nodesWithoutWeight.add(child);
            }
        }

        // TODO: 7/29/2023 This validation needs to be performed by the frontend before the analysis actually runs.
        if(sum > 1.0) {
            throw new IllegalArgumentException("The sum of the weight of all children should not be greater than 1");
        }

        double weightToDistribute = sum / nodesWithoutWeight.size();
        nodesWithoutWeight.forEach(nodeWithoutWeight -> nodeWithoutWeight.setWeight(weightToDistribute));

        // Recursive call for each child node
        for (TreeNode child : node.getChildren()) {
            assignWeight(child, preferences);
        }
    }

    /*
        Assigning the values to the leaf nodes(metrics), to later calculate the value of the parent node.
     */
    private void assignLeafNodeValue(TreeNode node, EnumMap<QualityMetric, Double> qualityMetricsReport) {
        if(isLeafNode(node)) {
            node.setValue(qualityMetricsReport.get(QualityMetric.valueOf(node.getName())));

            return;
        }

        for (TreeNode child : node.getChildren()) {
            assignLeafNodeValue(child, qualityMetricsReport);
        }
    }

    private void assignParentNodeValue(TreeNode node) {
        if (isLeafNode(node)) {
            return;
        }

        double parentValue = 0.0;
        for (TreeNode child : node.getChildren()) {
            assignParentNodeValue(child);
            parentValue += child.getWeight() * child.getValue();
        }

        node.setValue(parentValue);
    }

    private boolean isLeafNode(TreeNode node) {
        return node.getChildren().isEmpty();
    }
}
