package org.pillarone.riskanalytics.application.ui.main.action

import com.ulcjava.base.application.ULCTableTree
import com.ulcjava.base.application.event.ActionEvent
import grails.util.Holders
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.main.eventbus.event.OpenDetailViewEvent
import org.pillarone.riskanalytics.application.ui.main.view.DetailViewManager
import org.pillarone.riskanalytics.application.ui.main.view.TagsListView
import org.pillarone.riskanalytics.application.ui.main.view.item.UploadBatchUIItem
import org.pillarone.riskanalytics.application.ui.result.model.SimulationNode
import org.pillarone.riskanalytics.application.ui.upload.view.UploadBatchView
import org.pillarone.riskanalytics.core.simulation.item.Simulation

class UploadSimulationAction extends SelectionTreeAction {
    private static final Log log = LogFactory.getLog(UploadSimulationAction)
    UploadBatchUIItem item = new UploadBatchUIItem()

    UploadSimulationAction(ULCTableTree tree) {
        super('UploadSimulationAction', tree)
    }
    @Override
    protected List allowedRoles() {
        return ['ROLE_REVIEWER']
    }

    @Override
    void doActionPerformed(ActionEvent event) {
        if(enabled){
            ArrayList<Simulation> qtrSims = validateSelectedSims()
            if(qtrSims.empty){
                return
            }

            riskAnalyticsEventBus.post(new OpenDetailViewEvent(item))
            UploadBatchView uploadBatchView = detailViewManager.openDetailView as UploadBatchView
            uploadBatchView.addSimulations(qtrSims)
        }
    }

    //[AR-122] exclude non quarter-tagged sims and AZRe sims
    //
    private List<Simulation> validateSelectedSims() {
        if(! TagsListView.quarterTagsAreSpecial){
            log.warn("Please note: quarterTagsAreSpecial is disabled. But uploads still check quarter tags..")
        }
        List<Simulation> selectedSims = getSimulations()
        List<Simulation> qtrSims = selectedSims.findAll { sim ->
            sim?.tags?.any({ it.isQuarterTag() })
        }

        if (qtrSims.size() != selectedSims.size()) {
            int nonQtrCount = selectedSims.size() - qtrSims.size()
            Simulation example = selectedSims.find { !qtrSims.contains(it) }
            String title = "Only quarter-tagged sims can upload"
            String body = "($nonQtrCount) non quarter-tagged sims skipped.\n(No quarter tag found on ${(nonQtrCount > 1) ? 'e.g. ' : ''} ${example})"
            showInfoAlert(title, body)
        }

        List<Simulation> azReSims = qtrSims.findAll { sim ->
            sim?.getParameterization()?.tags?.any({ it.isAZReTag() })
        }

        int azReCount = azReSims.size()
        if (azReCount > 0) {
            String title = "Cannot upload AZRe sims "
            String body = "($azReCount) AZRe sims skipped.\nE.g. ${azReSims.first().parameterization.nameAndVersion} is an AZRe model."
            showInfoAlert(title, body)
            qtrSims.removeAll(azReSims)
        }
        return qtrSims
    }

    private List<Simulation> getSimulations() {
        List<SimulationNode> simulationNodes = getSelectedObjects(Simulation).findAll {
            it instanceof SimulationNode
        } as List<SimulationNode>
        return simulationNodes ? simulationNodes.itemNodeUIItem.item : []
    }

    private DetailViewManager getDetailViewManager() {
        Holders.grailsApplication.mainContext.getBean('detailViewManager', DetailViewManager)
    }
}