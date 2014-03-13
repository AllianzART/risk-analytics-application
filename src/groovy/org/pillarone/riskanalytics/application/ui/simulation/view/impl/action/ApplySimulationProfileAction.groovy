package org.pillarone.riskanalytics.application.ui.simulation.view.impl.action

import com.ulcjava.base.application.UlcUtilities
import com.ulcjava.base.application.event.ActionEvent
import org.pillarone.riskanalytics.application.ui.base.action.ResourceBasedAction
import org.pillarone.riskanalytics.application.ui.simulation.view.impl.SimulationProfileActionsPane
import org.pillarone.riskanalytics.application.ui.util.I18NAlert
import org.pillarone.riskanalytics.core.simulation.item.SimulationProfile

class ApplySimulationProfileAction extends ResourceBasedAction {

    public static final String APPLY_SIMULATION_PROFILE = 'ApplySimulationProfile'
    private final SimulationProfileActionsPane actionsPane

    ApplySimulationProfileAction(SimulationProfileActionsPane actionsPane) {
        super(APPLY_SIMULATION_PROFILE)
        this.actionsPane = actionsPane
    }

    @Override
    void doActionPerformed(ActionEvent event) {
        SimulationProfile item = actionsPane.model.loadSelectedProfile()
        if (!(item && item.id)) {
            new I18NAlert(UlcUtilities.getRootPane(actionsPane.content), 'ProfileNotExistent').show()
            return
        }
        actionsPane.model.apply(item)
    }

}
