package org.pillarone.riskanalytics.application.ui.batch.model

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.simulation.model.impl.queue.IOrderChangedListener
import org.pillarone.riskanalytics.application.ui.simulation.model.impl.queue.SortableTableModel
import org.pillarone.riskanalytics.application.ui.simulation.model.impl.queue.SortedEvent
import org.pillarone.riskanalytics.application.ui.simulation.model.impl.queue.UlcSimulationRuntimeService
import org.pillarone.riskanalytics.core.batch.BatchRunService
import org.pillarone.riskanalytics.core.simulation.engine.ISimulationRuntimeInfoListener
import org.pillarone.riskanalytics.core.simulation.engine.SimulationRuntimeInfo
import org.pillarone.riskanalytics.core.simulation.engine.SimulationRuntimeInfoAdapter
import org.pillarone.riskanalytics.core.simulation.item.Batch
import org.pillarone.riskanalytics.core.simulation.item.SimulationProfile
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import javax.annotation.Resource

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
class SimulationParameterizationTableModel extends SortableTableModel<BatchRowInfoColumnModel> {

    private final static Log LOG = LogFactory.getLog(SimulationParameterizationTableModel)

    private Batch batch
    private ISimulationRuntimeInfoListener simulationRuntimeInfoListener

    @Resource
    UlcSimulationRuntimeService ulcSimulationRuntimeService

    @Resource
    BatchRunService batchRunService

    private final IOrderChangedListener listener

    SimulationParameterizationTableModel() {
        super([])
        listener = new MyOrderListener()
        addOrderChangedListener(listener)
    }

    void setBatch(Batch batch) {
        infos = createBatchRowInfos(batch)
        this.batch = batch
    }

    @PostConstruct
    void initialize() {
        simulationRuntimeInfoListener = new MyRuntimeListener()
        ulcSimulationRuntimeService.addSimulationRuntimeInfoListener(simulationRuntimeInfoListener)
    }

    void destroy() {
        ulcSimulationRuntimeService.removeSimulationRuntimeInfoListener(simulationRuntimeInfoListener)
        simulationRuntimeInfoListener = null
    }

    @Override
    String getColumnName(int column) {
        BatchRowInfoColumnModel.COLUMN_NAMES[column]
    }

    @Override
    int getColumnCount() {
        BatchRowInfoColumnModel.SIZE
    }

    @Override
    Object getValueAt(int row, int column) {
        backedList[row].getValueAt(column)
    }

    void setInfos(List<BatchRowInfo> infos) {
        List<BatchRowInfoColumnModel> newModels = []
        infos.eachWithIndex { BatchRowInfo info, int row ->
            newModels << new BatchRowInfoColumnModel(row, this, info, columnCount)
        }
        backedList = newModels
        fireTableDataChanged()
    }

    private List<BatchRowInfo> createBatchRowInfos(Batch batch) {
        Map<Class, SimulationProfile> byModelClass = batchRunService.getSimulationProfilesGroupedByModelClass(batch.simulationProfileName)
        batch.parameterizations.collect {
            new BatchRowInfo(parameterization: it, simulationProfile: byModelClass[it.modelClass])
        }
    }

    void simulationProfileNameChanged() {
        Map<Class, SimulationProfile> byModelClass = batchRunService.getSimulationProfilesGroupedByModelClass(batch.simulationProfileName)
        backedList.each {
            it.object.simulationProfile = byModelClass[it.object.modelClass]
            it.update()
        }
    }

    private void assignRowsToColumnModels() {
        backedList.eachWithIndex { BatchRowInfoColumnModel columnModel, int row ->
            columnModel.row = row
        }
    }

    List<BatchRowInfo> getBatchRowInfos() {
        backedList.object
    }


    private class MyOrderListener implements IOrderChangedListener {
        @Override
        void orderChanged(SortedEvent event) {
            assignRowsToColumnModels()
        }
    }

    private class MyRuntimeListener extends SimulationRuntimeInfoAdapter {
        @Override
        void starting(SimulationRuntimeInfo info) {
            update(getColumnModel(info), info)
        }

        @Override
        void finished(SimulationRuntimeInfo info) {
            update(getColumnModel(info), info)

        }

        @Override
        void removed(SimulationRuntimeInfo info) {
            update(getColumnModel(info), null)
        }

        @Override
        void offered(SimulationRuntimeInfo info) {
            update(getColumnModel(info), info)
        }

        @Override
        void changed(SimulationRuntimeInfo info) {
            update(getColumnModel(info), info)
        }

        private update(BatchRowInfoColumnModel columnModel, SimulationRuntimeInfo info) {
            LOG.debug("trying to update info $info")
            if (columnModel) {
                LOG.debug("updating")
                columnModel.object.simulationRuntimeInfo = info
                columnModel.update()
            }
        }

        private BatchRowInfoColumnModel getColumnModel(SimulationRuntimeInfo info) {
            if (!isRelevant(info)) {
                return null
            }
            BatchRowInfoColumnModel columnModel = getBackedList().find { BatchRowInfoColumnModel columnModel ->
                columnModel.object.parameterization == info.parameterization
            }
            if (!columnModel) {
                throw new IllegalStateException("info $info belongs to our batch. But there is no column model for it!")
            }
            return columnModel
        }

        private boolean isRelevant(SimulationRuntimeInfo info) {
            if (!batch) {
                return false
            }
            info.simulation?.batch == batch
        }
    }
}
