package org.pillarone.riskanalytics.application.ui.main.view

import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.application.event.IActionListener
import com.ulcjava.base.application.util.Dimension
import org.pillarone.riskanalytics.application.dataaccess.item.ModellingItemFactory
import org.pillarone.riskanalytics.application.ui.main.view.item.ModellingUIItem
import org.pillarone.riskanalytics.application.ui.main.view.item.UIItemFactory
import org.pillarone.riskanalytics.application.ui.util.I18NAlert
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.simulation.item.ModellingItem
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.simulation.item.ResultConfiguration
import com.ulcjava.base.application.*

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
class OpenItemDialog {
    private ULCWindow parent
    ULCTableTree tree
    Model model
    RiskAnalyticsMainModel mainModel
    ULCDialog dialog
    ModellingUIItem modellingUIItem

    private ULCButton createCopyButton
    private ULCButton readOnlyButton
    private ULCButton deleteDependingResultsButton
    private ULCButton cancelButton
    private Dimension buttonDimension = new Dimension(160, 20)

    Closure closeAction = {event -> dialog.visible = false; dialog.dispose()}


    public OpenItemDialog(ULCTableTree tree, Model model, RiskAnalyticsMainModel mainModel, ModellingUIItem modellingUIItem) {
        this.tree = tree
        this.model = model
        this.modellingUIItem = modellingUIItem
        this.mainModel = mainModel
    }

    public void init() {
        initComponents()
        layoutComponents()
        attachListeners()
    }

    private void initComponents() {
        if (tree)
            this.parent = UlcUtilities.getWindowAncestor(tree)
        dialog = new ULCDialog(parent, UIUtils.getText(this.class, "title"), true)
        dialog.name = 'OpenItemDialog'
        createCopyButton = new ULCButton(UIUtils.getText(this.class, "createCopy"))
        createCopyButton.setPreferredSize(buttonDimension)
        createCopyButton.name = "OpenItemDialog.createCopy"
        readOnlyButton = new ULCButton(UIUtils.getText(this.class, "OpenReadOnly"))
        readOnlyButton.setPreferredSize(buttonDimension)
        readOnlyButton.name = "OpenItemDialog.readOnly"
        deleteDependingResultsButton = new ULCButton(UIUtils.getText(this.class, "DeleteDependingResults"))
        deleteDependingResultsButton.name = "OpenItemDialog.deleteDependingResultsButton"
        deleteDependingResultsButton.setPreferredSize(new Dimension(200, 20))
        cancelButton = new ULCButton(UIUtils.getText(this.class, "cancel"))
        cancelButton.setPreferredSize(buttonDimension)
        cancelButton.name = "OpenItemDialog.cancelButton"
    }

    private void layoutComponents() {
        ULCBoxPane content = new ULCBoxPane(rows: 1, columns: isWorkflowItem() ? 3 : 4)
        content.border = BorderFactory.createEmptyBorder(15, 15, 15, 15)
        if (!isWorkflowItem()) {
            content.add(ULCBoxPane.BOX_EXPAND_CENTER, createCopyButton)
        }
        content.add(ULCBoxPane.BOX_EXPAND_CENTER, readOnlyButton)
        content.add(ULCBoxPane.BOX_EXPAND_CENTER, deleteDependingResultsButton)
        content.add(ULCBoxPane.BOX_EXPAND_CENTER, cancelButton)

        dialog.add(content)
        dialog.setLocationRelativeTo(parent)
        dialog.pack()
        dialog.resizable = false
    }

    private void attachListeners() {
        createCopyButton.addActionListener([actionPerformed: {ActionEvent event ->
            closeAction.call()
            ModellingItem modellingItem = null
            if (!modellingUIItem.isLoaded()) {
                modellingUIItem.load()
            }
            ModellingUIItem newModellingUIItem
            modellingUIItem.item.daoClass.withTransaction {status ->
                if (!item.isLoaded())
                    item.load()
                modellingItem = ModellingItemFactory.incrementVersion(item)
                mainModel.fireModelChanged()
                newModellingUIItem = UIItemFactory.createItem(modellingItem, null, mainModel)
                mainModel.navigationTableTreeModel.addNodeForItem(newModellingUIItem)
            }
            if (newModellingUIItem)
                mainModel.openItem(model, newModellingUIItem)
        }] as IActionListener)

        readOnlyButton.addActionListener([actionPerformed: {ActionEvent event ->
            closeAction.call()
            if (!modellingUIItem.isLoaded())
                modellingUIItem.load()
            mainModel.notifyOpenDetailView(model, modellingUIItem)
        }] as IActionListener)

        deleteDependingResultsButton.addActionListener([actionPerformed: {ActionEvent event ->
            closeAction.call()
            if (modellingUIItem.deleteDependingResults(model)) {
                mainModel.openItem(model, modellingUIItem)
            } else {
                new I18NAlert(UlcUtilities.getWindowAncestor(parent), "DeleteAllDependentRunsError").show()
            }
        }] as IActionListener)

        cancelButton.addActionListener([actionPerformed: {ActionEvent event ->
            closeAction.call()
        }] as IActionListener)
    }

    public void setVisible(boolean visible) {
        dialog.setVisible(visible)
    }

    private boolean isWorkflowItem() {
        if (item instanceof Parameterization || item instanceof ResultConfiguration) {
            return item.versionNumber.workflow
        }

        return false
    }

    ModellingItem getItem() {
        return modellingUIItem.item
    }
}
