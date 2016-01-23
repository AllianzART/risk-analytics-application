package org.pillarone.riskanalytics.application.ui.main.action
import com.ulcjava.base.application.ULCTableTree
import com.ulcjava.base.application.event.ActionEvent
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.main.view.AddTagDialog
import org.pillarone.riskanalytics.application.ui.main.view.item.ModellingUIItem
/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
class TagsAction extends SelectionTreeAction {
    private static Log LOG = LogFactory.getLog(TagsAction)

    private String tagNameFilterRegex = null
    private static int countInstances = 0

    TagsAction(ULCTableTree tree,  String actionPropertyRoot="TagsAction", String tagNameFilterRegex = null) {
        super(actionPropertyRoot, tree);
        this.tagNameFilterRegex = tagNameFilterRegex
        ++countInstances
        LOG.info("Created instance nr: $countInstances (actionPropertyRoot=$actionPropertyRoot, onlyQuarterTags=$tagNameFilterRegex)")
    }

    void doActionPerformed(ActionEvent event) {
        AddTagDialog dialog = new AddTagDialog(tree, (List<ModellingUIItem>) getSelectedUIItems(),  tagNameFilterRegex )
        dialog.init()
        dialog.dialog.visible = true
    }
}
