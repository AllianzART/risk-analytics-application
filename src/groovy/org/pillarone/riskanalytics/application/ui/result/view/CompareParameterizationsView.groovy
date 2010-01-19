package org.pillarone.riskanalytics.application.ui.result.view

import com.canoo.ulc.community.fixedcolumntabletree.server.ULCFixedColumnTableTree
import com.ulcjava.base.application.ULCTabbedPane
import com.ulcjava.base.application.tabletree.ULCTableTreeColumn
import com.ulcjava.base.application.tree.TreePath
import org.pillarone.riskanalytics.application.ui.base.view.AbstractModellingTreeView
import org.pillarone.riskanalytics.application.ui.base.view.CompareComponentNodeTableTreeNodeRenderer
import org.pillarone.riskanalytics.application.ui.base.view.CompareParameterizationRenderer
import org.pillarone.riskanalytics.application.ui.base.view.PropertiesView
import org.pillarone.riskanalytics.application.ui.parameterization.model.CompareParameterViewModel
import org.pillarone.riskanalytics.application.ui.parameterization.view.CenteredHeaderRenderer
import org.pillarone.riskanalytics.application.ui.parameterization.view.SelectionTracker
import org.pillarone.riskanalytics.application.ui.util.UIUtils

/**
 * @author fouad jaada
 */

public class CompareParameterizationsView extends AbstractModellingTreeView {

    ULCTabbedPane tabbedPane
    PropertiesView propertiesView

    public CompareParameterizationsView(CompareParameterViewModel model) {
        super(model);
    }

    protected void initTree() {
        def treeModel = model.treeModel

        int treeWidth = UIUtils.calculateTreeWidth(treeModel.root)
        tree = new ULCFixedColumnTableTree(model.treeModel, 1, ([treeWidth] + ([150] * (model.columnCount - 1))) as int[])

        CompareParameterizationRenderer cRenderer = new CompareParameterizationRenderer()
        tree.viewPortTableTree.name = "parameterTreeContent"

        tree.viewPortTableTree.columnModel.getColumns().eachWithIndex {ULCTableTreeColumn it, int index ->

            it.setCellRenderer(cRenderer)
            it.setHeaderRenderer(new CenteredHeaderRenderer(index))
        }
        CompareComponentNodeTableTreeNodeRenderer renderer = new CompareComponentNodeTableTreeNodeRenderer(tree, model)


        tree.rowHeaderTableTree.columnModel.getColumns().each {ULCTableTreeColumn it ->
            it.setCellRenderer(renderer)
            it.setHeaderRenderer(new CenteredHeaderRenderer())
        }

        tree.rowHeaderTableTree.name = "parameterTreeRowHeader"
        tree.rowHeaderTableTree.columnModel.getColumn(0).headerValue = "Name"
        tree.cellSelectionEnabled = true
        // TODO (Mar 20, 2009, msh): Identified this as cause for PMO-240 (expand behaviour).

//         tree.viewPortTableTree.addActionListener(new MultiDimensionalTabStarter(this))


        model.treeModel.root.childCount.times {
            tree.expandPath new TreePath([model.treeModel.root, model.treeModel.root.getChildAt(it)] as Object[])
        }

        new SelectionTracker(tree)
    }

}
