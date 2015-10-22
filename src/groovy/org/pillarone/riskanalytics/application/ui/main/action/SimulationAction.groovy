package org.pillarone.riskanalytics.application.ui.main.action

import com.ulcjava.base.application.ULCTableTree
import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.application.event.KeyEvent
import com.ulcjava.base.application.util.KeyStroke
import grails.util.Holders
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.main.eventbus.event.OpenDetailViewEvent
import org.pillarone.riskanalytics.application.ui.main.view.DetailViewManager
import org.pillarone.riskanalytics.application.ui.main.view.item.SimulationSettingsUIItem
import org.pillarone.riskanalytics.application.ui.simulation.view.impl.SimulationConfigurationView
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.simulation.item.ResultConfiguration
import org.pillarone.riskanalytics.core.simulation.item.Simulation
import org.pillarone.riskanalytics.core.util.Configuration

/**
 * @author fouad.jaada@intuitive-collaboration.com
 *
 * Note: This action executes off the "Run simulation..." menu on a p14n.  It opens up the simulation pane.
 * (The RunSimulationAction presumably executes off the Run button on the simulation pane.)
 */
class SimulationAction extends SelectionTreeAction {

    private final static Log LOG = LogFactory.getLog(SimulationAction)
    private final static boolean duplicateExistingSimSettings = Configuration.getBoolean("duplicateExistingSimSettings", false)

    SimulationAction(ULCTableTree tree) {
        super("RunSimulation", tree)
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0, true));
    }

    void doActionPerformed(ActionEvent event) {
        if (enabled) {
            boolean overrideDefaultSimSettings = false
            Model selectedModel = selectedModel
            Object selectedItem = selectedItem
            Simulation simulation = new Simulation('Simulation')
            simulation.modelClass = selectedModel.modelClass
            Parameterization parameterization = selectedItem instanceof Parameterization ? selectedItem : null
            ResultConfiguration template = selectedItem instanceof ResultConfiguration ? selectedItem : null
            if( selectedItem instanceof Simulation ){
                // AR-200 - if sim result is selected, try use it's Parameterization / ResultConfiguration
                //
                Simulation existingResult = (Simulation) selectedItem
                if(!existingResult.loaded){
                    existingResult.load(true) // need true if want to also copy the sim settings
                }
                LOG.info("Run new sim based on selected existing result: '${existingResult}'")
                assert !parameterization
                assert !template
                parameterization = existingResult.parameterization
                template = existingResult.template
                overrideDefaultSimSettings = true
                LOG.info("Using p14n: '${parameterization.nameAndVersion}' & template: '${template.nameAndVersion}'")
            }
            riskAnalyticsEventBus.post(new OpenDetailViewEvent(new SimulationSettingsUIItem(simulation)))
            SimulationConfigurationView view = detailViewManager.openDetailView as SimulationConfigurationView
            view.model.parameterization = parameterization
            view.model.template = template

            if( overrideDefaultSimSettings && duplicateExistingSimSettings ){
                LOG.info("AR-200 in principle we could copy settings to use from the existing sim..")
            }
        }
    }

    @Override
    boolean isEnabled() {
        selectedModel
    }

    DetailViewManager getDetailViewManager() {
        Holders.grailsApplication.mainContext.getBean('detailViewManager', DetailViewManager)
    }
}
