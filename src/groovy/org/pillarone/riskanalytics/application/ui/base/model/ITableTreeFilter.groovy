package org.pillarone.riskanalytics.application.ui.base.model

import com.ulcjava.base.application.tabletree.ITableTreeNode
import org.pillarone.riskanalytics.application.ui.parameterization.model.ParameterizationNode
import org.pillarone.riskanalytics.application.ui.parameterization.model.WorkflowParameterizationNode
import org.pillarone.riskanalytics.application.ui.result.model.ResultStructureTableTreeNode

interface ITableTreeFilter {

    boolean acceptNode(ITableTreeNode node)

}

class NodeNameFilter implements ITableTreeFilter {

    String nodeName

    public NodeNameFilter(nodeName) {
        this.nodeName = nodeName;
    }

    public boolean acceptNode(ITableTreeNode node) {
        node ? internalAcceptNode(node) : false
    }

    boolean internalAcceptNode(ITableTreeNode node) {
        return acceptNode(node.parent)
    }

    boolean internalAcceptNode(ComponentTableTreeNode node) {
        return nodeName ? nodeName == node.displayName || acceptNode(node.parent) : true
    }

    boolean internalAcceptNode(DynamicComposedComponentTableTreeNode node) {
        return nodeName ? nodeName == node.displayName || acceptNode(node.parent) : true
    }

    boolean internalAcceptNode(ResultStructureTableTreeNode node) {
        return nodeName ? nodeName == node.displayName || acceptNode(node.parent) : true
    }

}

class ParameterizationNodeFilter implements ITableTreeFilter {
    List values
    int column

    public ParameterizationNodeFilter(List values, int column) {
        this.values = values;
        this.column = column
    }

    public boolean acceptNode(ITableTreeNode node) {
        return node ? internalAcceptNode(node) : false;
    }

    boolean internalAcceptNode(ParameterizationNode node) {
        if (column == -1) return true
        boolean contains = values?.contains(node.values[column])
        return contains
    }

    boolean internalAcceptNode(WorkflowParameterizationNode node) {
        if (column == -1) return true
        boolean contains = values?.contains(node.values[column])
        return contains
    }

    boolean internalAcceptNode(ITableTreeNode node) {
        return true
    }

}