package org.pillarone.riskanalytics.application.ui.result.view

import com.canoo.ulc.community.fixedcolumntabletree.server.ULCFixedColumnTableTree
import com.canoo.ulc.detachabletabbedpane.server.ITabListener
import com.canoo.ulc.detachabletabbedpane.server.TabEvent
import com.canoo.ulc.detachabletabbedpane.server.ULCCloseableTabbedPane
import com.canoo.ulc.detachabletabbedpane.server.ULCDetachableTabbedPane
import com.ulcjava.base.application.ULCButton
import com.ulcjava.base.application.ULCComponent
import com.ulcjava.base.application.ULCToolBar
import com.ulcjava.base.application.event.KeyEvent
import com.ulcjava.base.application.tabletree.ULCTableTreeColumn
import com.ulcjava.base.application.tree.ULCTreeSelectionModel
import org.pillarone.riskanalytics.application.dataaccess.function.Mean
import org.pillarone.riskanalytics.application.ui.base.view.AbstractModellingFunctionView
import org.pillarone.riskanalytics.application.ui.main.model.P1RATModel
import org.pillarone.riskanalytics.application.ui.parameterization.view.CenteredHeaderRenderer
import org.pillarone.riskanalytics.application.ui.result.action.PercisionAction
import org.pillarone.riskanalytics.application.ui.result.model.ResultTableTreeColumn
import org.pillarone.riskanalytics.application.ui.result.model.ResultViewModel
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.application.util.LocaleResources
import com.ulcjava.base.application.ULCComboBox
import com.ulcjava.base.application.util.Dimension
import org.pillarone.riskanalytics.application.ui.result.action.ApplySelectionAction
import com.ulcjava.base.application.ULCBoxPane
import com.ulcjava.base.application.ULCLabel

class ResultView extends AbstractModellingFunctionView {

    ULCCloseableTabbedPane tabbedPane
    P1RATModel p1ratModel
    ULCComboBox selectView

    public static int space = 3

    public ResultView(ResultViewModel model) {
        super(model)
        tabbedPane = new ULCDetachableTabbedPane()
        tabbedPane.addTabListener([tabClosing: {TabEvent event -> event.getClosableTabbedPane().closeCloseableTab(event.getTabClosingIndex())}] as ITabListener)
        tabbedPane.registerKeyboardAction(ctrlaction, KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK), ULCComponent.WHEN_IN_FOCUSED_WINDOW)
    }

    protected void initTree() {

        int treeWidth = UIUtils.calculateTreeWidth(model.treeModel.root)

        tree = new ULCFixedColumnTableTree(model.treeModel, 1, ([treeWidth] + ([100] * (model.treeModel.columnCount - 1))) as int[], true, false)
        tree.viewPortTableTree.name = "resultDescriptorTreeContent"
        tree.rowHeaderTableTree.name = "resultDescriptorTreeRowHeader"
        tree.rowHeaderTableTree.columnModel.getColumn(0).headerValue = getText("NameColumnHeader")
        tree.setCellSelectionEnabled true

        tree.rowHeaderTableTree.columnModel.getColumns().each {ULCTableTreeColumn it ->
            it.setCellRenderer(new ResultViewTableTreeNodeCellRenderer(tabbedPane, model.treeModel.simulationRun, tree, model, this))
            it.setHeaderRenderer(new CenteredHeaderRenderer())
        }

        tree.rowHeaderTableTree.selectionModel.setSelectionMode(ULCTreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION)
        model.periodCount.times {int index ->
            ULCTableTreeColumn column = new ResultTableTreeColumn(index + 1, tree.viewPortTableTree, new Mean())
            column.setMinWidth(110)
            column.setHeaderRenderer(new CenteredHeaderRenderer())
            tree.viewPortTableTree.addColumn column
        }
    }

    protected void addPrecisionFunctions(ULCToolBar toolbar) {
        selectionToolbar.addSeparator()
        selectionToolbar.add new ULCButton(new PercisionAction(model, -1, "reducePrecision"))
        selectionToolbar.add new ULCButton(new PercisionAction(model, +1, "increasePrecision"))
    }

    public ULCBoxPane createSelectionPane() {
        selectView = new ULCComboBox(model.selectionViewModel)
        selectView.name = "selectView"
        selectView.setPreferredSize(new Dimension(120, 20))
        selectView.addActionListener(new ApplySelectionAction(model, this))

        filterSelection = new ULCComboBox()
        filterSelection.name = "filter"
        filterSelection.addItem(getText("all"))
        model.nodeNames.each {
            filterSelection.addItem it
        }

        filterLabel = new ULCLabel(UIUtils.getIcon("filter-active.png"))

        ULCBoxPane filters = new ULCBoxPane(3, 1)
        filters.add(ULCBoxPane.BOX_EXPAND_CENTER, selectView)
        filters.add(filterLabel)
        filters.add(filterSelection)
        return filters
    }

    /**
     * Utility method to get resource bundle entries for this class
     *
     * @param key
     * @return the localized value corresponding to the key
     */
    protected String getText(String key) {
        return LocaleResources.getString("ResultView." + key);
    }

}
