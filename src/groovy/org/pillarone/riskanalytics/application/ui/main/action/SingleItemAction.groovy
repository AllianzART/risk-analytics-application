package org.pillarone.riskanalytics.application.ui.main.action
import com.ulcjava.base.application.ULCTableTree
/**
 * @author fazl.rahman@art-allianz.com
 *
 * Introduce class to hold common behaviour of actions that
 * only operate on a single item.
 *
 */
abstract class SingleItemAction extends SelectionTreeAction {

    SingleItemAction(String name, ULCTableTree tree = null) {
        super(name, tree)
    }

    // Current approach to enable menu only when one item is selected :-
    //
    // 1) register menuitem itself as a tree selection listener
    // 2) menuitem query this method on tree selection events
    //
    // The EnabledCheckingMenuItem encapsulates this behaviour.
    //
    @Override
    boolean isEnabled() {
        if (getAllSelectedObjectsSimpler().size() != 1) {
            return false
        }
        return super.isEnabled()//generic checks like user roles
    }

}

