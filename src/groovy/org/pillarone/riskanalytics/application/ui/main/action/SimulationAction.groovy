package org.pillarone.riskanalytics.application.ui.main.action

import com.ulcjava.base.application.ULCTableTree
import com.ulcjava.base.application.event.ActionEvent
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.main.view.RiskAnalyticsMainModel
import org.pillarone.riskanalytics.application.ui.main.view.item.SimulationUIItem
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.simulation.item.ResultConfiguration
import org.pillarone.riskanalytics.core.simulation.item.Simulation

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
class SimulationAction extends SelectionTreeAction {

    static Log LOG = LogFactory.getLog(SimulationAction)

    public SimulationAction(ULCTableTree tree, RiskAnalyticsMainModel model) {
        super("RunSimulation", tree, model)
    }

    public void doActionPerformed(ActionEvent event) {
        Model selectedModel = getSelectedModel()
        if (selectedModel) {
            Object selectedItem = getSelectedItem()
            Simulation simulation = new Simulation("Simulation")
            simulation.parameterization = selectedItem instanceof Parameterization ? selectedItem : null
            simulation.template = selectedItem instanceof ResultConfiguration ? selectedItem : null
            model.openItem(selectedModel, new SimulationUIItem(model, selectedModel, simulation))
        }
        else {
            LOG.debug("No selected model found. Action cancelled.")
        }
    }

}
