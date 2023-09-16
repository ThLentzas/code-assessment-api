package gr.aegean.service.assessment;

import gr.aegean.model.analysis.quality.TreeNode;
import gr.aegean.entity.Preference;
import gr.aegean.model.analysis.quality.QualityAttribute;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class TreeServiceTest {
    private TreeService underTest;

    @BeforeEach
    void setup() {
        underTest = new TreeService();
    }

    @Test
    void shouldBuildTree() {
        TreeNode root = underTest.buildTree();

        assertThat(root.getName()).isEqualTo("Rank");

        TreeNode quality = root.getChildren().get(0);
        TreeNode security = root.getChildren().get(1);

        assertThat(quality.getChildren())
                .extracting(TreeNode::getName)
                .containsExactly(
                        "COMPREHENSION",
                        "SIMPLICITY",
                        "MAINTAINABILITY",
                        "RELIABILITY",
                        "COMPLEXITY");

        assertThat(security.getChildren())
                .extracting(TreeNode::getName)
                .containsExactly(
                        "VULNERABILITY_SEVERITY",
                        "HOTSPOT_PRIORITY",
                        "SECURITY_REMEDIATION_EFFORT");

        TreeNode comprehension = quality.getChildren().get(0);
        TreeNode simplicity = quality.getChildren().get(1);
        TreeNode maintainability = quality.getChildren().get(2);
        TreeNode reliability = quality.getChildren().get(3);
        TreeNode complexity = quality.getChildren().get(4);

        assertThat(comprehension.getChildren())
                .extracting(TreeNode::getName)
                .containsExactly("COMMENT_RATE");

        assertThat(simplicity.getChildren())
                .extracting(TreeNode::getName)
                .containsExactly("METHOD_SIZE");

        assertThat(maintainability.getChildren())
                .extracting(TreeNode::getName)
                .containsExactly("DUPLICATION", "TECHNICAL_DEBT_RATIO");

        assertThat(reliability.getChildren())
                .extracting(TreeNode::getName)
                .containsExactly("BUG_SEVERITY", "RELIABILITY_REMEDIATION_EFFORT");

        assertThat(complexity.getChildren())
                .extracting(TreeNode::getName)
                .containsExactly("CYCLOMATIC_COMPLEXITY", "COGNITIVE_COMPLEXITY");
    }

    /*
        isLeafNode() is also tested indirectly.
     */
    @Test
    void shouldThrowIllegalArgumentExceptionWhenTheSumOfTheWeightsOfChildNodesIsGreaterThanOne() {
        //Arrange
        TreeNode root = underTest.buildTree();
        List<Preference> preferences = new ArrayList<>();
        preferences.add(new Preference(QualityAttribute.QUALITY, 0.4));
        preferences.add(new Preference(QualityAttribute.SECURITY, 0.7));

        //Act Assert
        assertThatThrownBy(() -> underTest.validateChildNodesWeightsSum(root, preferences))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The combined weights of Security node's child nodes must not exceed 1");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenTheSumOfTheWeightsForAllChildNodesIsLessThanOne() {
        //Arrange
        TreeNode root = underTest.buildTree();
        List<Preference> preferences = new ArrayList<>();
        preferences.add(new Preference(QualityAttribute.QUALITY, 0.4));
        preferences.add(new Preference(QualityAttribute.SECURITY, 0.3));

        //Act Assert
        assertThatThrownBy(() -> underTest.validateChildNodesWeightsSum(root, preferences))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The combined weights of all Security node's child nodes must not be less than 1");
    }
}
