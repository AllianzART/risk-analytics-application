package org.pillarone.riskanalytics.application.ui.main.action

import com.ulcjava.base.application.ULCComponent
import com.ulcjava.base.application.UlcUtilities
import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.application.event.IWindowListener
import com.ulcjava.base.application.event.WindowEvent
import grails.util.Holders
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.base.action.ResourceBasedAction
import org.pillarone.riskanalytics.application.ui.main.eventbus.event.CloseDetailViewEvent
import org.pillarone.riskanalytics.application.ui.main.view.DetailViewManager
import org.pillarone.riskanalytics.application.ui.main.view.NewVersionCommentDialog
import org.pillarone.riskanalytics.application.ui.main.view.item.*
import org.pillarone.riskanalytics.application.ui.util.I18NAlert
import org.pillarone.riskanalytics.core.simulation.item.ModellingItem
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.workflow.Status
import org.pillarone.riskanalytics.core.workflow.StatusChangeService

class SaveAction extends ResourceBasedAction {

    private static final Log LOG = LogFactory.getLog(SaveAction)

    ModellingUIItem currentItem
    ULCComponent parent

    public SaveAction(ULCComponent parent) { //nb directly called by HeaderView.initComponents()
        super("Save")
        this.parent = parent
    }

    public SaveAction(ULCComponent parent, ModellingItem currentItem) {
        this(parent)
        this.currentItem = UIItemFactory.createItem(currentItem)
    }

    public void doActionPerformed(ActionEvent event) {
        save(currentItem ?: detailViewManager.currentUIItem)
    }

    private DetailViewManager getDetailViewManager() {
        Holders.grailsApplication.mainContext.getBean('detailViewManager', DetailViewManager)
    }

    // Work with the type system and avoid instanceof ..
    //
    void save(ModellingUIItem modellingUIItem) {

        boolean changedAndUsed = modellingUIItem.item.changed && modellingUIItem.usedInSimulation

        if( changedAndUsed ){
            LOG.info("Cannot save ${modellingUIItem.nameAndVersion} (already used in a simulation).")
            I18NAlert alert = new I18NAlert(UlcUtilities.getWindowAncestor(parent), "SaveItemAlreadyUsed")
            alert.addWindowListener([windowClosing: { WindowEvent e -> handleEvent(alert, modellingUIItem) }] as IWindowListener)
            alert.show()
        } else {
            LOG.info("Saving ${modellingUIItem.nameAndVersion}.")
            saveItem(modellingUIItem)
        }

    }
    void save(AbstractUIItem argItem) {
        // NOP
    }


    void saveItem(ModellingUIItem modellingUIItem) {
        modellingUIItem.save()
    }

    /**
     * save a changed item by creating  a new version or deleting all dependent simulations
     */
    private void handleEvent(I18NAlert alert, ModellingUIItem modellingUIItem) {

        synchronized (modellingUIItem) {
            if (alert.value.equals(alert.firstButtonLabel)) {
                LOG.info("Saving changes of ${modellingUIItem.nameAndVersion} into a new version.")
                handleNewVersion(modellingUIItem) //PMO-2054
            } else if (alert.value.equals(alert.secondButtonLabel)) {
                boolean deleted = modellingUIItem.deleteDependingResults()
                if (deleted) {
                    LOG.info("Deleting depending results of ${modellingUIItem.nameAndVersion} before saving.")
                    saveItem(modellingUIItem)
                } else {
                    String errorKey = UIItemUtils.isUsedInRunningSimulation(modellingUIItem.item) ? "DeleteAllDependentRunningsError" : "DeleteAllDependentRunsError"
                    new I18NAlert(UlcUtilities.getWindowAncestor(parent), errorKey).show()
                }
            } else {
                LOG.info("Save aborted.")
            }
        }
    }

    private void handleNewVersion(ModellingUIItem item) {
        item.createNewVersion(false)
        riskAnalyticsEventBus.post(new CloseDetailViewEvent(item))
    }

    private void handleNewVersion(ParameterizationUIItem item) {
        Closure okAction = { String commentText ->
            if (!item.isLoaded()) {
                item.load()
            }
            createNewVersion(item, commentText)
            riskAnalyticsEventBus.post(new CloseDetailViewEvent(item))
        }

        NewVersionCommentDialog versionCommentDialog = new NewVersionCommentDialog(okAction)
        versionCommentDialog.show()
    }

    private void createNewVersion(ParameterizationUIItem item, String commentText) {
        Parameterization originalParameterization = item.item as Parameterization
        if (originalParameterization.status != Status.NONE) {
            Parameterization parameterization = StatusChangeService.service.changeStatus(originalParameterization, Status.DATA_ENTRY)
            parameterization.save()
        } else {
            item.createNewVersion(commentText, false)
        }
    }

    @Override
    boolean isEnabled() {
        if (detailViewManager.currentUIItem) {
            return itemChanged(detailViewManager.currentUIItem)
        }
        return false
    }

    boolean itemChanged(ModellingUIItem item) {
        return item.item.changed
    }

    boolean itemChanged(ParameterizationUIItem parameterizationUIItem) {
        return parameterizationUIItem.item.changed || parameterizationUIItem.item.commentHasChanged()
    }

    boolean itemChanged(Object item) {
        return false
    }

    @Override
    String toString() {
        if (currentItem){
            return "currentItem: $currentItem"
        }

        // Avoid all those "currenItem: null" log debug lines
        //
        DetailViewManager dvm = getDetailViewManager()
        if( dvm ){
            return "currentItem: ${dvm.getCurrentUIItem()}"
        }
        return ""
    }
}
