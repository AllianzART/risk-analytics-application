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
import org.pillarone.riskanalytics.core.workflow.Status

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

    //[AR-122] exclude :
    // - non quarter-tagged sims
    // - AZRe sims
    // - sims whose model lacks qtr tag or not IN PRODUCTION
    //
    private List<Simulation> validateSelectedSims() {
        if(! TagsListView.quarterTagsAreSpecial){
            log.warn("Please note: quarterTagsAreSpecial is disabled. But uploads still check quarter tags..")
        }
        List<Simulation> selectedSims = getSimulations()
        selectedSims.each { sim ->
            if(!sim.isLoaded()){
               sim.load(true) // need the tags
        }}
        List<Simulation> qtrSims = selectedSims.findAll { sim ->
            sim?.tags?.any({ it.isQuarterTag() })
        }

        int badCount = selectedSims.size() - qtrSims.size()
        if (0 < badCount) {
            Simulation example = selectedSims.find { !qtrSims.contains(it) }
            String title = "Only tagged (non azre) sims can upload"
            String body = "Oops! ($badCount) non quarter-tagged sims skipped.\n(No quarter tag found on ${(badCount > 1) ? 'e.g. ' : ''} ${example})"
            showInfoAlert(title, body, true)
        }

        selectedSims.each { sim ->
            if(!sim.parameterization?.isLoaded()){
                sim.parameterization?.load(true) // need the tags
            }}
        List<Simulation> azReSims = qtrSims.findAll { sim ->
            sim?.getParameterization()?.tags?.any({ it.isAZReTag() })
        }

        badCount = azReSims.size()
        if (badCount > 0) {
            String title = "Only IT-Apps can upload AZRe sims"
            String body = "Oops! ($badCount) AZRe sims skipped.\nE.g. ${azReSims.first().parameterization.nameAndVersion} is an AZRe model."
            showInfoAlert(title, body, true)
            qtrSims.removeAll(azReSims)
        }

        // Do models carry the quarter tag ?
        //
        List<Simulation> modelsLackingQtrTag = qtrSims.findAll { final sim ->
            ! sim?.getParameterization()?.tags?.any({ pt -> pt == sim.tags.find{ st -> st.isQuarterTag() }})
        }
        badCount = modelsLackingQtrTag.size()
        if (badCount > 0) {
            String title = "Non-quarter-tagged models skipped"
            String body = "Oops! MODEL behind ($badCount) sims lack matching Quarter Tag.\n(E.g. ${modelsLackingQtrTag.first().parameterization.nameAndVersion})."
            showInfoAlert(title, body, true)
            qtrSims.removeAll(modelsLackingQtrTag)
        }

        // Are models in PRODUCTION state ?
        //
        List<Simulation> nonProdModels = qtrSims.findAll { sim ->
            Status.IN_PRODUCTION != sim?.getParameterization()?.status
        }
        badCount = nonProdModels.size()
        if (badCount > 0) {
            String title = "Non-Production models skipped"
            String body = "Oops! ($badCount) sims lack PRODUCTION model status.\nE.g. ${nonProdModels.first().parameterization.nameAndVersion} is not IN PRODUCTION."
            showInfoAlert(title, body, true)
            qtrSims.removeAll(nonProdModels)
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