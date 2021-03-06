package org.pillarone.riskanalytics.functional.main

import com.ulcjava.base.application.event.KeyEvent
import com.ulcjava.testframework.operator.ULCTableTreeOperator
import com.ulcjava.testframework.operator.ULCTextFieldOperator
import grails.util.Holders
import org.pillarone.riskanalytics.application.ui.simulation.view.impl.SimulationActionsPane
import org.pillarone.riskanalytics.core.fileimport.ParameterizationImportService
import org.pillarone.riskanalytics.core.queue.QueueListener
import org.pillarone.riskanalytics.core.simulation.engine.SimulationQueueEntry
import org.pillarone.riskanalytics.core.simulation.engine.SimulationQueueService
import org.pillarone.riskanalytics.functional.AbstractFunctionalTestCase

import java.util.concurrent.CountDownLatch

class RunSimulationTests extends AbstractFunctionalTestCase {

    private SimulationQueueListener listener = new SimulationQueueListener()

    void setUp() {
        new ParameterizationImportService().compareFilesAndWriteToDB(['Core'])
        super.setUp();
        simulationQueueService.addQueueListener(listener)
    }

    void tearDown() {
        super.tearDown()
        simulationQueueService.removeQueueListener(listener)
        listener.offered.clear()
    }

    private getSimulationQueueService() {
        Holders.grailsApplication.mainContext.getBean('simulationQueueService', SimulationQueueService)
    }

    public void testRunSimulation() {
        ULCTableTreeOperator tableTree = selectionTableTreeRowHeader
        pushKeyOnPath(tableTree, tableTree.findPath(["Core", "Parameterization"] as String[]), KeyEvent.VK_F9, 0)
        ULCTextFieldOperator iterations = getTextFieldOperator("iterations")
        iterations.typeText("11")
        getButtonOperator("${SimulationActionsPane.simpleName}.run").clickMouse()

        assert listener.offered.any {
            it.context.simulationTask.simulation.numberOfIterations == 11
        }
        listener.waitTillFinished()
    }
}

class SimulationQueueListener implements QueueListener<SimulationQueueEntry> {
    CountDownLatch latch

    void waitTillFinished() {
        latch.await()
    }

    List<SimulationQueueEntry> offered = []

    @Override
    void starting(SimulationQueueEntry entry) {

    }

    @Override
    void finished(SimulationQueueEntry entry) {
        latch.countDown()
    }

    @Override
    void removed(UUID id) {

    }

    @Override
    void offered(SimulationQueueEntry entry) {
        latch = new CountDownLatch(1)
        offered.add(entry)
    }
}