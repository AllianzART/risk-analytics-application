package org.pillarone.riskanalytics.application.ui.simulation.model.impl.finished

import org.pillarone.riskanalytics.application.ui.UlcSessionScope
import org.pillarone.riskanalytics.application.ui.main.view.RiskAnalyticsMainModel
import org.pillarone.riskanalytics.application.ui.simulation.model.impl.queue.UlcSimulationRuntimeService
import org.pillarone.riskanalytics.core.simulation.engine.ISimulationRuntimeInfoListener
import org.pillarone.riskanalytics.core.simulation.engine.SimulationRuntimeInfo
import org.pillarone.riskanalytics.core.simulation.engine.SimulationRuntimeInfoAdapter
import org.pillarone.riskanalytics.core.simulation.engine.SimulationRuntimeService
import org.pillarone.riskanalytics.core.user.UserManagement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.annotation.Resource

@Scope(UlcSessionScope.ULC_SESSION_SCOPE)
@Component
class FinishedSimulationsViewModel {
    @Resource
    UlcSimulationRuntimeService ulcSimulationRuntimeService
    @Autowired
    SimulationRuntimeService simulationRuntimeService
    @Resource
    FinishedSimulationsTableModel finishedSimulationsTableModel
    @Resource
    RiskAnalyticsMainModel riskAnalyticsMainModel

    private final ISimulationRuntimeInfoListener infoListener = new MyInfoListener()

    @PostConstruct
    void initialize() {
        ulcSimulationRuntimeService.addSimulationRuntimeInfoListener(infoListener)
        finishedSimulationsTableModel.infos = simulationRuntimeService.finished
    }

    @PreDestroy
    void unregister() {
        ulcSimulationRuntimeService.removeSimulationRuntimeInfoListener(infoListener)
    }

    List<SimulationRuntimeInfo> getInfoAt(int[] selected) {
        selected.collect {
            finishedSimulationsTableModel.getInfoAt(it)
        }
    }

    void clearAll() {
        finishedSimulationsTableModel.infos = []
    }

    void removeAt(int[] selected) {
        finishedSimulationsTableModel.removeAt(selected)
    }

    private class MyInfoListener extends SimulationRuntimeInfoAdapter {

        @Override
        void finished(SimulationRuntimeInfo info) {
            if (isMine(info)) {
                finishedSimulationsTableModel.itemAdded(info)
            }
        }

        private boolean isMine(SimulationRuntimeInfo info) {
            UserManagement.currentUser?.username == info.offeredBy?.username
        }
    }
}