package org.pillarone.riskanalytics.application.ui.main.view

import com.ulcjava.base.application.*
import com.ulcjava.base.application.event.IActionListener
import com.ulcjava.base.application.util.Dimension
import org.pillarone.riskanalytics.application.ui.main.view.item.BatchUIItem
import org.pillarone.riskanalytics.application.ui.main.view.item.ModellingUIItem
import org.pillarone.riskanalytics.application.ui.main.view.item.ParameterizationUIItem
import org.pillarone.riskanalytics.application.ui.main.view.item.ResourceUIItem
import org.pillarone.riskanalytics.application.ui.main.view.item.SimulationResultUIItem
import org.pillarone.riskanalytics.application.ui.util.I18NAlert
import org.pillarone.riskanalytics.application.util.LocaleResources
import org.pillarone.riskanalytics.core.BatchRun
import org.pillarone.riskanalytics.core.ParameterizationDAO
import org.pillarone.riskanalytics.core.ResourceDAO
import org.pillarone.riskanalytics.core.output.ResultConfigurationDAO

class NodeNameDialog {

    private ModellingUIItem modellingUIItem
    private ULCWindow parent
    private ULCDialog dialog
    private ULCTextField nameInput
    private ULCButton okButton
    private ULCButton cancelButton

    Closure okAction
    String title

    NodeNameDialog(ULCWindow parent, ModellingUIItem item) {
        this.parent = parent
        this.modellingUIItem = item
        initComponents()
        layoutComponents()
        attachListeners()
    }

    private void initComponents() {
        dialog = new ULCDialog(parent, true)
        dialog.name = 'renameDialog'
        nameInput = new ULCTextField(modellingUIItem.name)
        nameInput.name = 'newName'
        okButton = new ULCButton(getText("okButton"))
        okButton.name = 'okButton'
        cancelButton = new ULCButton(getText("cancelButton"))

    }

    private void layoutComponents() {
        nameInput.preferredSize = new Dimension(200, 20)
        ULCBoxPane content = new ULCBoxPane(rows: 2, columns: 4)
        content.border = BorderFactory.createEmptyBorder(15, 15, 15, 15)
        content.add(ULCBoxPane.BOX_LEFT_CENTER, new ULCLabel(getText("name") + ":"))
        content.add(3, ULCBoxPane.BOX_EXPAND_CENTER, nameInput)
        content.add(ULCBoxPane.BOX_EXPAND_BOTTOM, new ULCFiller())
        content.add(ULCBoxPane.BOX_EXPAND_BOTTOM, new ULCFiller())
        okButton.preferredSize = new Dimension(120, 20)
        content.add(ULCBoxPane.BOX_RIGHT_BOTTOM, okButton)
        cancelButton.preferredSize = new Dimension(120, 20)
        content.add(ULCBoxPane.BOX_RIGHT_BOTTOM, cancelButton)

        dialog.add(content)
        dialog.locationRelativeTo = parent
        dialog.pack()
        dialog.resizable = false

    }

    private void attachListeners() {
        IActionListener action = [actionPerformed: { e ->
            if (!modellingUIItem.loaded) {
                modellingUIItem.load()
            }
            if (isUnique(modellingUIItem)) {
                okAction.call(nameInput.text); hide()
            } else {
                I18NAlert alert = new I18NAlert(parent, "UniquesNamesRequired")
                alert.show()
            }
        }] as IActionListener

        nameInput.addActionListener(action)
        okButton.addActionListener(action)
        cancelButton.addActionListener([actionPerformed: { e -> hide() }] as IActionListener)
    }

    protected boolean isUnique(SimulationResultUIItem simulationUIItem) {
        ResultConfigurationDAO.findByNameAndModelClassName(nameInput.text, simulationUIItem.item.modelClass.name) == null
    }

    protected boolean isUnique(ModellingUIItem modellingUIItem) {
        modellingUIItem.item.daoClass.findByNameAndModelClassName(nameInput.text, modellingUIItem.item.modelClass.name) == null
    }

    // AR-227 isUnique for given name, modelClassName, name *and* sandbox-vs-workflow (deduce from itemVersion)
    //        String name
    //        String modelClassName
    //        String itemVersion
    //
    protected boolean isUnique(ParameterizationUIItem parameterizationUiItem) {

        def c = ParameterizationDAO.createCriteria()
        // Do existing p14ns match new name / model class ?
        // Sorting means any sandbox models (v1, v2 etc) appear before workflows (vR1, vR2 etc)
        //
        def instanceList = ParameterizationDAO.findAllByNameAndModelClassName(
                nameInput.text,
                parameterizationUiItem.item.modelClass.name,
                [max:1, sort:"itemVersion", order:"asc"] )

        if( ! instanceList?.size() ){
            return true
        }

        // Return true if only workflow models exist - as we will save-as a sandbox model
        //
        return instanceList.first().itemVersion?.startsWith('R')
    }

    protected boolean isUnique(ResourceUIItem resource) {
        ResourceDAO.findByNameAndResourceClassName(nameInput.text, resource.item.modelClass.name) == null
    }

    protected boolean isUnique(BatchUIItem resource) {
        BatchRun.findByName(nameInput.text) == null
    }

    void show() {
        dialog.title = title
        dialog.visible = true
    }

    void hide() {
        dialog.visible = false
    }

    /**
     * Utility method to get resource bundle entries for this class
     *
     * @param key
     * @return the localized value corresponding to the key
     */
    String getText(String key) {
        return LocaleResources.getString("NodeNameDialog." + key);
    }
}