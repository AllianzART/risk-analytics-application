package org.pillarone.riskanalytics.application.ui.chart.view

import com.ulcjava.base.application.ULCFrame
import com.ulcjava.testframework.operator.ComponentByNameChooser
import com.ulcjava.testframework.operator.ULCCheckBoxOperator
import com.ulcjava.testframework.operator.ULCComponentOperator
import com.ulcjava.testframework.operator.ULCFrameOperator

import groovy.mock.interceptor.StubFor
import org.pillarone.riskanalytics.application.dataaccess.function.ResultFunction
import org.pillarone.riskanalytics.core.output.SimulationRun
import org.pillarone.riskanalytics.application.ui.base.model.SimpleTableTreeNode
import org.pillarone.riskanalytics.application.ui.chart.model.PDFGaussKernelEstimateChartViewModel

import org.pillarone.riskanalytics.application.ui.result.model.ResultTableTreeNode
import org.pillarone.riskanalytics.core.dataaccess.ResultAccessor
import org.pillarone.riskanalytics.application.AbstractSimpleFunctionalTest

class ChartViewTests extends AbstractSimpleFunctionalTest {
    PDFGaussKernelEstimateChartViewModel model

    protected void doStart() {
        ULCFrame frame = new ULCFrame("test")
        frame.defaultCloseOperation = ULCFrame.TERMINATE_ON_CLOSE
        frame.name = "test"
        List<SimpleTableTreeNode> nodes = new ArrayList()
        StubFor resultAccessorStub = new StubFor(ResultAccessor)
        StubFor resultFunctionStub = new StubFor(ResultFunction)


        resultFunctionStub.demand.getPath(160..160) {node -> node.name}
        resultFunctionStub.demand.getAttributeName(20..20) {node -> "foo"}

        resultAccessorStub.demand.hasDifferentValues(20..20) {SimulationRun simulationRun, int periodIndex, String path, String c, String f -> true}
        resultAccessorStub.demand.getValues(60..60) {run, period, path, c, f ->
            if ("TEST_NODE0".equals(path)) {return [1d, 3d, 5d, 7d]} else return [1d, 2d, 3d, 4d, 5d, 6d]
        }
        resultAccessorStub.demand.getMin(20..20) {SimulationRun simulationRun, int periodIndex, String path, String c, String f -> 1}
        resultAccessorStub.demand.getMax(20..20) {SimulationRun simulationRun, int periodIndex, String path, String c, String f -> 5}
        resultAccessorStub.demand.getMean(20..20) {SimulationRun simulationRun, int periodIndex, String path, String c, String f -> 3}
        resultAccessorStub.demand.getStdDev(20..20) {SimulationRun simulationRun, int periodIndex, String path, String c, String f -> 2}
        resultAccessorStub.demand.getPercentile(20..20) {SimulationRun simulationRun, int periodIndex, String path, String c, String f, double percentile -> 6}
        resultAccessorStub.demand.getPercentile(20..20) {SimulationRun simulationRun, int periodIndex, String path, String c, String f, double percentile -> 7}


        ResultTableTreeNode node = new ResultTableTreeNode("test_node0")
        node.collector = "testCollector"
        ResultTableTreeNode node1 = new ResultTableTreeNode("test_node1")
        node.collector = "testCollector"


        resultFunctionStub.use {
            resultAccessorStub.use {
                SimulationRun run = new SimulationRun()
                run.periodCount = 4
                model = new PDFGaussKernelEstimateChartViewModel("TEST", run, [node, node1])
                model.showPeriodLabels = false
                frame.contentPane = new ChartView(model).content
                frame.visible = true
            }
        }

    }

    void testChartVisible() {
        ULCFrameOperator frame = new ULCFrameOperator("test")
        assertNotNull frame
        ULCComponentOperator chartBox = new ULCComponentOperator(frame, new ComponentByNameChooser("ChartBox"))
        assertNotNull chartBox
        assertTrue chartBox.getULCComponent().getComponentCount() > 0
    }

    void testSelectPeriod() {
        ULCFrameOperator frame = new ULCFrameOperator("test")
        assertNotNull frame
        ULCCheckBoxOperator period0Box = new ULCCheckBoxOperator(frame, new ComponentByNameChooser("Seriestest_node0Period0CheckBox"))
        ULCCheckBoxOperator period3Box = new ULCCheckBoxOperator(frame, new ComponentByNameChooser("Seriestest_node1Period3CheckBox"))
        assertNotNull period3Box


        assertTrue(model.showLine[[0, 0]])
        assertTrue(model.showLine[[1, 0]])
        period3Box.clickMouse()
        assertTrue(model.showLine[[0, 0]])
        assertTrue(model.showLine[[1, 0]])
        assertTrue(model.showLine[[1, 3]])
        period0Box.clickMouse()
        assertFalse(model.showLine[[0, 0]])
        assertTrue(model.showLine[[1, 0]])
        assertTrue(model.showLine[[1, 3]])
    }

}