package org.pillarone.riskanalytics.application.ui.main.action

import com.ulcjava.base.application.IAction
import com.ulcjava.base.application.ULCAlert
import com.ulcjava.base.application.UlcUtilities
import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.application.event.KeyEvent
import com.ulcjava.base.application.util.KeyStroke
import org.apache.commons.lang.StringUtils
import org.pillarone.riskanalytics.application.ui.base.action.ResourceBasedAction
import org.pillarone.riskanalytics.application.ui.base.view.DynamicComponentNameDialog
import org.pillarone.riskanalytics.application.ui.parameterization.model.ParameterViewModel
import org.pillarone.riskanalytics.application.ui.util.ComponentUtils
import org.pillarone.riskanalytics.application.ui.util.ExceptionSafe
import org.pillarone.riskanalytics.application.ui.util.I18NAlert
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.core.components.Component
import org.pillarone.riskanalytics.core.components.NonUniqueComponentNameException
import org.pillarone.riskanalytics.core.util.Configuration

class AddDynamicSubComponent extends ResourceBasedAction {

    private static boolean suppressAR107Fix = (Configuration.coreGetAndLogStringConfig("suppressAR107Fix","false") == "true");
    def tree
    ParameterViewModel model

    public AddDynamicSubComponent(def tree, ParameterViewModel model) {
        super("AddDynamicSubComponent")
        this.tree = tree
        this.model = model
        putValue(IAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0, true));
    }


    public void doActionPerformed(ActionEvent event) {
        if (model.paramterTableTreeModel.readOnly) return
        def node = tree.selectedPath.lastPathComponent
        if (!node || !ComponentUtils.isDynamicComposedComponent(node)) return;

        if (isNotEditable(node)) {
            return
        }
        DynamicComponentNameDialog dialog = new DynamicComponentNameDialog(UlcUtilities.getWindowAncestor(tree))
        dialog.title = UIUtils.getText(this.class, "newDynamicSubComponent") + ": " + (node ? node.getDisplayName() : "dynamic component")
        dialog.okAction = {
            ExceptionSafe.protect {
                String name = dialog.nameInput.text.trim()
                name = ComponentUtils.getSubComponentName(name)

                if (name.length() == 0 || !StringUtils.isAlphanumericSpace(name)) {
                    ULCAlert alert = new I18NAlert(UlcUtilities.getWindowAncestor(tree), "IllegalSubComponentName")
                    alert.show()
                    return
                }
                try {
                    // AR-207 Don't even create a component nor name it till you know the name is OK!
                    //
                    String basePath = [ComponentUtils.removeModelFromPath(node.path, model.model), name].join(":")
                    if(!suppressAR107Fix){ // remove test next release if no issues found
                        if(model.parametrizedItem.notDeletedParameterHolders.find { it.path.contains(basePath+':')} ){
                            throw new NonUniqueComponentNameException("A non-deleted parameter starting with '${name}' already exists! \n(Clicking before looking?)")
                        }
                    }
                    Component component = node.component.createDefaultSubComponent()
                    component.name = name
                    model.parametrizedItem.addComponent(basePath, component)
                } catch (NonUniqueComponentNameException e) {
                    ULCAlert alert = new I18NAlert(UlcUtilities.getWindowAncestor(tree), "UniqueSubComponent")
                    alert.show()
                }
            }
        }
        dialog.show()
    }

    @Override
    String logContent() {
        "Adding component with name:"
    }

    public boolean isEnabled() {
        return super.isEnabled() && !model.paramterTableTreeModel.readOnly;
    }

    private boolean isNotEditable(def node) {
        def model = model.paramterTableTreeModel
        model.readOnly || !model.isNodeInEditablePaths(node)
    }
}
