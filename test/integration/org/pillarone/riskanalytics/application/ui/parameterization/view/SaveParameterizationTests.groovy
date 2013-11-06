package org.pillarone.riskanalytics.application.ui.parameterization.view

import com.canoo.ulc.community.ulcclipboard.server.ULCClipboard
import com.ulcjava.base.application.ULCBoxPane
import com.ulcjava.base.application.ULCFrame
import com.ulcjava.base.application.event.IActionListener
import com.ulcjava.base.application.event.KeyEvent
import com.ulcjava.base.application.util.KeyStroke
import com.ulcjava.testframework.operator.ComponentByNameChooser
import com.ulcjava.testframework.operator.ULCFrameOperator
import com.ulcjava.testframework.operator.ULCTableTreeOperator
import models.core.CoreModel
import org.pillarone.riskanalytics.application.AbstractSimpleFunctionalTest
import org.pillarone.riskanalytics.application.dataaccess.item.ModellingItemFactory
import org.pillarone.riskanalytics.application.ui.parameterization.model.ParameterViewModel
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.core.ModelStructureDAO
import org.pillarone.riskanalytics.core.ParameterizationDAO
import org.pillarone.riskanalytics.core.fileimport.ModelStructureImportService
import org.pillarone.riskanalytics.core.fileimport.ParameterizationImportService
import org.pillarone.riskanalytics.core.simulation.item.ModelStructure
import org.pillarone.riskanalytics.core.simulation.item.ModellingItem
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.application.ui.main.view.item.ParameterizationUIItem
import org.pillarone.riskanalytics.application.ui.main.view.RiskAnalyticsMainModel
import org.pillarone.riskanalytics.application.ui.main.view.item.AbstractUIItem

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
class SaveParameterizationTests extends AbstractSimpleFunctionalTest {

    Parameterization parameterization

    protected void doStart() {
        new ParameterizationImportService().compareFilesAndWriteToDB(['Core'])
        new ModelStructureImportService().compareFilesAndWriteToDB(['Core'])

        ModellingItemFactory.clear()

        ULCFrame frame = new ULCFrame("test")
        frame.defaultCloseOperation = ULCFrame.TERMINATE_ON_CLOSE
        frame.name = "test"
        CoreModel model = new CoreModel()
        model.initComponents()

        def dao = ParameterizationDAO.findByName('CoreParameters')
        parameterization = ModellingItemFactory.getParameterization(dao)
        parameterization.load()
        parameterization.changed = true
        parameterization.metaClass.isUsedInSimulation = {-> return false}
        dao = ModelStructureDAO.findByModelClassName(model.class.name)
        ModelStructure structure = ModellingItemFactory.getModelStructure(dao)
        structure.load()
        ParameterViewModel parameterViewModel = new ParameterViewModel(model, parameterization, structure)
        parameterViewModel.mainModel = new RiskAnalyticsMainModel(null)
        parameterViewModel.mainModel.currentItem = new ParameterizationUIItem(parameterViewModel.mainModel, new CoreModel(), parameterization)

        ULCBoxPane content = new ParameterView(parameterViewModel, new RiskAnalyticsMainModel()).content
        IActionListener saveAction = content.getActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK, false))
        saveAction.metaClass.saveItem = {AbstractUIItem modellingItem ->
            parameterization.changed = false
        }
        frame.setContentPane(content)
        ULCClipboard.install()
        UIUtils.setRootPane(frame)
        frame.visible = true
    }

    public void testSave() {
        ULCFrameOperator frameOperator = new ULCFrameOperator(new ComponentByNameChooser("test"))
        ULCTableTreeOperator componentTree = new ULCTableTreeOperator(frameOperator, new ComponentByNameChooser("parameterTreeRowHeader"))

        assertNotNull componentTree


        componentTree.pushKey(KeyEvent.VK_S, KeyEvent.CTRL_MASK)

        assertFalse parameterization.changed
    }


}
