package org.pillarone.riskanalytics.application.ui.simulation.model.impl.finished

import org.pillarone.riskanalytics.application.ui.UlcSessionScope
import org.pillarone.riskanalytics.application.ui.simulation.model.impl.queue.SimulationQueueTableModel
import org.pillarone.riskanalytics.application.ui.simulation.model.impl.queue.SimulationRowModel
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Scope(UlcSessionScope.ULC_SESSION_SCOPE)
@Component
class FinishedSimulationsTableModel extends SimulationQueueTableModel {

    @Override
    protected void sortColumnModels() {}

    void removeAt(int[] selected) {
        List<SimulationRowModel> toRemove = selected.collect {
            columnModels[it]
        }
        columnModels.removeAll(toRemove)
        assignRowsToColumnModels()
        List<Integer> selectedAsList = selected.toList()
        fireTableRowsDeleted(selectedAsList.min(), selectedAsList.max())
    }
}
