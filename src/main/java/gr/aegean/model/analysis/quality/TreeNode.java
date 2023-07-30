package gr.aegean.model.analysis.quality;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TreeNode {
    private String name;
    private Double weight;
    private List<TreeNode> children;
    private Double value;

    public TreeNode(String name) {
        this.name = name;
        children = new ArrayList<>();
    }

    public void addChild(TreeNode child) {
        this.children.add(child);
    }
}
