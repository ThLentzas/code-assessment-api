package gr.aegean.service.assessment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import gr.aegean.entity.Preference;
import gr.aegean.model.analysis.quality.QualityMetric;
import gr.aegean.model.analysis.quality.TreeNode;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class RankingService {
    private final TreeService treeService;

    /*
        If any empty list of preferences is provided, the weights are equally distributed among the child nodes.
     */
    public double rankTree(Map<QualityMetric, Double> qualityMetricsReport, List<Preference> preferences) {
        TreeNode root = treeService.buildTree();

        assignWeight(root, preferences);
        assignLeafNodeValue(root, qualityMetricsReport);
        assignParentNodeValue(root);

        return root.getValue();
    }

    private void assignWeight(TreeNode node, List<Preference> preferences) {
        if (treeService.isLeafNode(node)) {
            return;
        }

        /*
            If there is only 1 child node then it has the max weight.
         */
        if (node.getChildren().size() == 1) {
            node.getChildren().get(0).setWeight(1.0);

            return;
        }

        List<TreeNode> childNodesWithoutWeight = new ArrayList<>();
        double sum = 1.0;
        Optional<Preference> matchingPreference;

        for (TreeNode child : node.getChildren()) {
            matchingPreference = preferences.stream()
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
                childNodesWithoutWeight.add(child);
            }
        }

        /*
            Weight is distributed dynamically to the child nodes without weight
         */
        double weightToDistribute = sum / childNodesWithoutWeight.size();
        childNodesWithoutWeight.forEach(nodeWithoutWeight -> nodeWithoutWeight.setWeight(weightToDistribute));

        for (TreeNode child : node.getChildren()) {
            assignWeight(child, preferences);
        }
    }

    /*
        Assigning the values to the leaf nodes(metrics), to calculate the value of the parent node.
     */
    private void assignLeafNodeValue(TreeNode node, Map<QualityMetric, Double> qualityMetricsReport) {
        if (treeService.isLeafNode(node)) {
            node.setValue(qualityMetricsReport.get(QualityMetric.valueOf(node.getName())));

            return;
        }

        for (TreeNode child : node.getChildren()) {
            assignLeafNodeValue(child, qualityMetricsReport);
        }
    }

    /*
        The value of a parent node is the sum of the products of each child node's weight and value
     */
    private void assignParentNodeValue(TreeNode node) {
        if (treeService.isLeafNode(node)) {
            return;
        }

        double parentValue = 0.0;
        for (TreeNode child : node.getChildren()) {
            assignParentNodeValue(child);
            parentValue += child.getWeight() * child.getValue();
        }

        node.setValue(parentValue);
    }
}
