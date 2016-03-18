package org.pillarone.riskanalytics.application.ui.parameterization.model.popup.impl

import com.ulcjava.base.application.ULCMenuItem
import com.ulcjava.base.application.ULCPopupMenu
import com.ulcjava.base.application.ULCTableTree
import grails.util.Holders
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.main.action.*
import org.pillarone.riskanalytics.application.ui.main.view.EnabledCheckingMenuItem
import static org.pillarone.riskanalytics.application.ui.main.view.TagsListView.quarterTagsAreSpecial

import org.pillarone.riskanalytics.application.ui.parameterization.model.ParameterizationNode
import static org.pillarone.riskanalytics.core.parameter.comment.Tag.qtrTagMatcherRegex

/**
 * Allianz Risk Transfer  ATOM
 * User: bzetterstrom
 *
 * frahman 2014-01-02: Looks like this class builds (in its ctor) the context menu on a parameterization node.
 */
abstract class AbstractParameterNodePopupMenu extends ULCPopupMenu {
    private static Log LOG = LogFactory.getLog(AbstractParameterNodePopupMenu)
    private static int instanceCount =  0;

    public AbstractParameterNodePopupMenu(ULCTableTree tree, ParameterizationNode node) {
        super();
        ++instanceCount;
        LOG.info("Creating instance $instanceCount");
        name = "parameterNodePopUpMenu";
        add(new ULCMenuItem(new OpenItemAction(tree)));
        add(new EnabledCheckingMenuItem(new SimulationAction(tree))); //PMO-2764

        //External link
        add(new EnabledCheckingMenuItem(new OpenTransactionLinkAction(tree)));
        addSeparator();
        add(new EnabledCheckingMenuItem(new CompareParameterizationsAction(tree)));

//        add(createTagsMenu(tree))
        if( quarterTagsAreSpecial ){
            add(new ULCMenuItem(new TagsAction(tree, 'QuarterTagsAction', qtrTagMatcherRegex)))
        }
        add(new ULCMenuItem(new TagsAction(tree)));

        Boolean b = ((Boolean) Holders.grailsApplication?.config?.getProperty("useSetFilterToSelectionPopupMenu")) ?: Boolean.FALSE;
        if (b) {
            add(new ULCMenuItem(new SetFilterToSelection(tree)));
        }
        addSeparator();
        if (hasRenameAction()){
            add(new EnabledCheckingMenuItem(new RenameAction(tree))); //PMO-2764
        }
        add(new EnabledCheckingMenuItem(new SaveAsAction(tree)));
        if (hasCreateNewMajorVersionAction()){
            add(new EnabledCheckingMenuItem(new CreateNewMajorVersion(tree))); //PMO-2764
        }
        add(new ULCMenuItem(new ExportItemAction(tree)));
        add(new ULCMenuItem(new ImportParameterizationExcelAction(tree, 'ImportFromExcelAdditional')));
        addSeparator();
        add(new ULCMenuItem(new CreateBatchAction(tree)))
        add(new EnabledCheckingMenuItem(new AddToOpenBatchAction(tree)))
        add(new EnabledCheckingMenuItem(new RemoveFromOpenBatchAction(tree)))
        //Concrete subclasses add menus appropriate to current state.
        //Eg the InReviewPopupMenu adds menu items for send to production and reject workflow.
        //(Like a 'statemachine' encoded in class diagram, fixed at compile time)
        boolean separatorNeeded = addMenuItemsForWorkflowState(tree, node);

        node.addReportMenus(this, tree, separatorNeeded);
        //reach here on first opening of Parameterizations subtree in gui, hits 4 times for different pns.
        if (hasDeleteAction()) {
            addSeparator();
            add(new ULCMenuItem(new DeleteAction(tree)));
        }
        addSeparator();
        add(new ULCMenuItem(new ShowPropertiesAction(tree)));
    }

    protected abstract boolean hasRenameAction();

    protected abstract boolean hasCreateNewMajorVersionAction();

    protected abstract boolean hasDeleteAction();

    /**
     * Add menu items specific for this state
     * @param tree
     * @return true if menu items were added (and a trailing menu separator is required)
     */
    protected abstract boolean addMenuItemsForWorkflowState(ULCTableTree tree, ParameterizationNode node);
}
