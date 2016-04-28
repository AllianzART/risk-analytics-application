package org.pillarone.riskanalytics.application.ui.simulation.view.impl.queue.action

import com.ulcjava.base.application.event.ActionEvent
import org.pillarone.riskanalytics.application.ui.UlcSessionScope
import org.pillarone.riskanalytics.application.ui.base.action.ResourceBasedAction
import org.pillarone.riskanalytics.application.ui.simulation.view.impl.queue.SimulationQueueView
import org.pillarone.riskanalytics.core.simulation.engine.SimulationRuntimeService
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.annotation.Resource

@Scope(UlcSessionScope.ULC_SESSION_SCOPE)
@Component
class RegisterNotificationOnSimulationEntryAction extends ResourceBasedAction {

    @Resource
    SimulationRuntimeService simulationRuntimeService

    @Resource
    SimulationQueueView simulationQueueView


    RegisterNotificationOnSimulationEntryAction() {
        super('RegisterNotification')
    }

    @Override
    void doActionPerformed(ActionEvent event) {
        if (enabled) {
            simulationQueueView.selectedSimulations.each {
                simulationRuntimeService.registerForNotificationOnQueueEntry(it.id, it.getUsername())
            }
        }
    }

    @Override
    boolean isEnabled() {
        return simulationQueueView.selectedSimulations.size() > 0
    }

}
