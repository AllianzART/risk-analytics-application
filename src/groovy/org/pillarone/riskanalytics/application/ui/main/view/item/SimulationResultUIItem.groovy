package org.pillarone.riskanalytics.application.ui.main.view.item

import com.google.common.base.Joiner
import com.ulcjava.base.application.tabletree.IMutableTableTreeNode
import com.ulcjava.base.application.util.ULCIcon
import org.apache.commons.lang.builder.HashCodeBuilder
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.base.model.ItemNode
import org.pillarone.riskanalytics.application.ui.base.model.TableTreeBuilderUtils
import org.pillarone.riskanalytics.application.ui.main.view.IDetailView
import org.pillarone.riskanalytics.application.ui.main.view.TagsListView
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.core.parameter.comment.Tag
import org.pillarone.riskanalytics.core.simulation.item.Simulation
/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
abstract class SimulationResultUIItem<T extends IDetailView> extends ModellingUiItemWithModel<T> {

    private static final Log LOG = LogFactory.getLog(SimulationResultUIItem )

    SimulationResultUIItem(Simulation simulation) {
        super(simulation)
    }

    String taglist2Csv( List<Tag> list){
        return Joiner.on(',').join( list.collect {it.name} )
    }
    // [AR-97] Prevent deleting sims carrying a quarter tag
    // Only if the special quarter tag behaviour is not suppressed (by -DquarterTagsAreSpecial=false)
    @Override
    boolean isDeletable() {
        if( TagsListView.quarterTagsAreSpecial ){
            final Simulation  simulation = item as Simulation
            final ArrayList<Tag> tags = simulation.getTags();
            if ( tags.any({ it.isQuarterTag() })  ) {
                LOG.info( "Not DELETABLE: ${simulation.getName()} as qtr-tagged; tags: ${taglist2Csv(tags)} (hint: quarterTagsAreSpecial)" )
                return false
            }
        }
        return true;
    }

    @Override
    void rename(String newName) {
        ItemNode itemNode = TableTreeBuilderUtils.findNodeForItem(navigationTableTreeModel.root as IMutableTableTreeNode, item)
        itemNode.userObject = newName
    }

    @Override
    ULCIcon getIcon() {
        return UIUtils.getIcon("results-active.png")
    }

    @Override
    boolean equals(Object obj) {
        if (!(obj instanceof ModellingUIItem)) {
            return false
        }
        ModellingUIItem mObj = (obj as ModellingUIItem)
        return item.modelClass == mObj.item.modelClass && item.name == mObj.item.name
    }

    @Override
    int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder()
        hcb.append(item.modelClass.toString())
        hcb.append(item.modelClass.name)
        return hcb.toHashCode()
    }

    @Override
    Simulation getItem() {
        return super.item as Simulation
    }
}
