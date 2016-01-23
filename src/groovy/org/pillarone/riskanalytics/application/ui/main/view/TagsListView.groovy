package org.pillarone.riskanalytics.application.ui.main.view

import com.ulcjava.base.application.BorderFactory
import com.ulcjava.base.application.ULCBoxPane
import com.ulcjava.base.application.ULCCheckBox
import com.ulcjava.base.application.ULCFiller
import com.ulcjava.base.application.ULCWindow
import com.ulcjava.base.application.event.IValueChangedListener
import com.ulcjava.base.application.event.ValueChangedEvent
import com.ulcjava.base.application.util.Color
import org.apache.commons.lang.StringUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.core.output.SimulationRun
import org.pillarone.riskanalytics.core.parameter.comment.Tag
import org.pillarone.riskanalytics.core.simulation.item.ModellingItem
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.simulation.item.Simulation
import org.pillarone.riskanalytics.core.simulation.item.VersionNumber
import org.pillarone.riskanalytics.core.simulation.item.parameter.comment.EnumTagType
import org.pillarone.riskanalytics.core.util.Configuration
import org.pillarone.riskanalytics.core.workflow.Status

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
class TagsListView extends AbstractView {

    private static Log LOG = LogFactory.getLog(TagsListView )

    // Set -DquarterTagsAreSpecial=true to enable Artisan Quarter Tag validations on Workflow p14ns etc
    //
    final static boolean quarterTagsAreSpecial =
        Configuration.coreGetAndLogStringConfig("quarterTagsAreSpecial","false").equalsIgnoreCase("true");

    private ULCWindow parent
    public ULCBoxPane content
    public List<Tag> itemTags
    private List<ULCCheckBox> tagsCheckBoxes
    private List<ModellingItem> modellingItems
    List<Tag> allTags
    String tagNameFilterRegex = null

    public TagsListView(List<ModellingItem> modellingItems, ULCWindow parent, String nameFilter = null) {
        this.modellingItems = modellingItems
        content = new ULCBoxPane(2, 0)
        this.parent = parent
        this.tagNameFilterRegex = nameFilter
    }

    public void createNewTag(String suppliedName) {

        String tagName = StringUtils.trim(suppliedName)
        if( StringUtils.isEmpty(tagName) ){
            LOG.warn("Not creating tag with empty name '$suppliedName' ")
            return
        }

        if( tagNameFilterRegex && !tagName.matches(tagNameFilterRegex ) ){
            LOG.warn("Not creating tag with name '$suppliedName' (doesn't match tag filter '$tagNameFilterRegex')")
            return
        }

        if ( !Tag.findByName(tagName)) {
            Tag newTag = new Tag(name: tagName, tagType: EnumTagType.PARAMETERIZATION)
            Tag.withTransaction {
                newTag.save()
                allTags.add(newTag)
            }
            content.removeAll()
            initComponents()
        }

    }

    @Override
    protected void initComponents() {
        allTags = getAllTags()
        itemTags = getAllModellingItemTags()

        tagsCheckBoxes = new ArrayList<ULCCheckBox>()
        allTags.each { Tag tag ->
            ULCCheckBox checkBox = new ULCCheckBox(tag.name)
            checkBox.name = tag.name
            setAppearance(checkBox, tag)
            checkBox.setSelected(itemTags.contains(tag))
            checkBox.addValueChangedListener([valueChanged: { ValueChangedEvent valueChangedEvent ->
                ULCCheckBox srcCheckBox = (ULCCheckBox) valueChangedEvent.source
                Tag newTag = allTags.find { it.name == srcCheckBox.getText() }
                if( !newTag ){
                    LOG.error("INSANITY Checkbox labelled with unknown tag ${srcCheckBox.text} fired event $valueChangedEvent ")
                }else{
                    if (srcCheckBox.isSelected()) {
                        addTagToItems(newTag)
                    } else {
                        removeTagFromItems(newTag)
                    }
                    setAppearance(srcCheckBox, newTag)
                }
            }] as IValueChangedListener)
            content.add(ULCBoxPane.BOX_LEFT_TOP, checkBox)
            content.add(ULCBoxPane.BOX_EXPAND_EXPAND, new ULCFiller())
        }
        content.add(ULCBoxPane.BOX_EXPAND_BOTTOM, new ULCFiller())
    }

    @Override
    protected void layoutComponents() {
    }

    @Override
    protected void attachListeners() {
    }

    public List<Tag> getAllTags() {
        // For Quarter Tags Only case, want newest tags at top, so descending order by name
        String ascOrDesc = Tag.qtrTagMatcherRegex.equals(tagNameFilterRegex) ? 'desc' : 'asc'

        List<Tag> allTags =  Tag.findAll(" from ${Tag.class.name} as tag where tag.tagType =? and tag.name != ? order by tag.name $ascOrDesc", [EnumTagType.PARAMETERIZATION, Tag.LOCKED_TAG])
        if( tagNameFilterRegex ){
            return allTags.findAll {it.name.matches(tagNameFilterRegex)}
        }else{
            return allTags
        }
    }

    private List<Tag> getAllModellingItemTags() {
        Set<Tag> all = new HashSet<Tag>()
        for (ModellingItem item : modellingItems) {
            for (Tag tag : item.getTags()) {
                all.add(tag)
            }
        }
        return all as List
    }

    private void addTagToItems(Tag tag) {
        if (!itemTags.contains(tag)) itemTags << tag            // Some itemTags were only on subset of selected items
        for (ModellingItem modellingItem : modellingItems) {
            if (!modellingItem.getTags().contains(tag)) {
                if(!quarterTagsAreSpecial){
                    // Generic logic
                    modellingItem.getTags().add(tag)
                    modellingItem.setChanged(true)
                }else{
                    // ART-specific logic
                    if( !isQuarterTagCollision( modellingItem, tag, parent ) ){
                        modellingItem.getTags().add(tag)
                        modellingItem.setChanged(true)
                    }
                }
            }
        }
    }

    // PMO-2741
    //
    private static boolean isQuarterTagCollision( ModellingItem modellingItem, Tag tag, ULCWindow par ){

        // Only restricting quarter tags
        //
        if( ! tag.isQuarterTag() ){
            return false
        }

        if( modellingItem instanceof Parameterization ){
            // Only restricting qtr tags in workflow models
            //
            Parameterization parameterization = modellingItem as Parameterization
            if( parameterization.status == Status.NONE ){
                // TODO Protect against user accidentally quarter-tagging sandbox model too ?
                return false
            }

            return isTagOnWorkflow(parameterization, tag, par)

        } else if (modellingItem instanceof Simulation){

            Simulation simulation = modellingItem as Simulation
            return isSimAlreadyQtrTagged(simulation, tag, par) || // same sim can't be in two different quarters (AR-192)
                   isTagOnDeal(simulation, tag, par)            // qtr run can only have one sim for each deal

        } else {
            return false
        }
    }

    // AR-192
    // Does the sim Janet wants to tag already have a quarter tag?
    //
    private static boolean isSimAlreadyQtrTagged( Simulation sim, Tag newTag, ULCWindow par ){
        if( !sim.loaded ){
            sim.load(true) //includes comments, params and **tags**
        }

        Tag oldTag = sim?.tags?.find {it.isQuarterTag()}  //collect {it.isQuarterTag()} //any { it.isQuarterTag() }
        if(oldTag){
            String firstLine= "Cant add ${newTag.name} tag to '${sim.name}' :"
            String secondLine = "Sim result already carries tag ${oldTag.name}"
            LOG.warn(firstLine + " " + secondLine)
            LOG.info("To disable qtr tag checks, override -DquarterTagsAreSpecial=false ")
            UIUtils.showWarnAlert(par, "Sim result cannot be in two different quarters", firstLine + "\n" + secondLine)
            return true
        }
        return false
    }
    // PMO-2741
    // Does proposed tag already exist for current deal ?
    // Or, should tag not be allowed owing to no deal ?
    //
    private static boolean isTagOnDeal( Simulation simulation, Tag tag, ULCWindow par ){

        if( !simulation.loaded ){
            simulation.load(false) //skips comments, params and tags
        }
        if( !simulation?.parameterization?.loaded){
            simulation?.parameterization?.load(false)
        }

        // If no deal, is sim off sandbox model -> should not be adding quarter tag..
        //
        Long dealId = simulation?.parameterization?.dealId
        if( ! dealId ){
            String firstLine= "Cant add ${tag.name} to '${simulation.name}' (sandbox model sim):"
            String secondLine = "Model '${simulation?.parameterization?.nameAndVersion}' is not a production workflow"
            LOG.warn(firstLine + " " + secondLine)
            LOG.info("To disable qtr tag checks, override -DquarterTagsAreSpecial=false ")
            UIUtils.showWarnAlert(par, "Cant quarter-tag sandbox sim-results", firstLine + "\n" + secondLine)
            return true
        }

        // Get names of only sims with tags (for this deal); can avoid masses of irrelevant untagged sims
        // Yields: array of maps (entries: simName, tagCount)
        // Figured this out via dumb luck + trial n error in debugger + http://docs.jboss.org/hibernate/core/3.6/reference/en-US/html/queryhql.html
        String query = "select new map( sim.name as simName, count(tags) as tagCount ) from SimulationRun sim left outer join sim.tags tags " +
                       "where sim.parameterization.dealId = :dealId " +
                       "group by sim.name " +
                       "having count(tags) > 0 " +
                       "order by sim.name desc"
        ArrayList results = SimulationRun.executeQuery(query, ["dealId": dealId], [readOnly: true])
        LOG.info("Found ${results.size()} sims on deal $dealId having tags")

        // This yields too much noise
        // List<SimulationRun> runsOnDeal = SimulationRun.executeQuery(" from SimulationRun sim where sim.parameterization.dealId = :dealId", ["dealId": dealId], [readOnly: true])

        // Check for quarter tag on the other sims (not self)
        //
        for( Map nameCountPair : results ){
            String simName = nameCountPair["simName"]
            if( simulation.name == simName ){
                continue
            }
            Simulation sim = new Simulation(simName)
            sim.load()
            for( Tag t : sim.tags ){
                if( tag.id == t.id ){
                    String firstLine= "Deal ${dealId}: Cant add ${tag.name} to '${simulation.name}' :"
                    String secondLine = "Tag already on '${sim.name}'."
                    LOG.warn(firstLine + " " + secondLine)
                    LOG.info("To disable qtr tag checks, override -DquarterTagsAreSpecial=false ")
                    UIUtils.showWarnAlert(par, "Duplicate quarter tag on deal $dealId", firstLine + "\n" + secondLine)
                    return true
                }
            }
        }

//      LOG.info("OK to add tag '${tag.name}' on sim: ${simulation.name} as no sim off deal $dealId has it yet.")
        return false
    }

    // Does supplied tag already exist on any other version p14n in workflow ?
    //
    private static boolean isTagOnWorkflow( Parameterization workflow, Tag tag, ULCWindow par ){

        for( VersionNumber versionNumber : VersionNumber.getExistingVersions(workflow)){

            if(workflow.versionNumber.toString().equals(versionNumber.toString())){
                continue
            }

            Parameterization otherP14n = new Parameterization(workflow.name,workflow.modelClass)
            otherP14n.versionNumber = versionNumber
            otherP14n.load();

            for( Tag t in otherP14n.tags ){
                if( tag.id == t.id ){
                    String firstLine = "Cannot tag ${workflow.nameAndVersion} with '${tag.name}'"
                    String secondLine= "(Tag already exists on v${otherP14n.versionNumber.toString()} of same workflow.)"
                    LOG.warn(firstLine + " " + secondLine)
                    LOG.info("To disable qtr tag checks, override -DquarterTagsAreSpecial=false ")
                    UIUtils.showWarnAlert(par, "Duplicate quarter tag in workflow", firstLine + "\n" + secondLine)
                    return true
                }
            }
        }
        return false
    }



    private void removeTagFromItems(Tag tag) {
        itemTags.remove(tag)
        for (ModellingItem modellingItem : modellingItems) {
            if (modellingItem.getTags().contains(tag)) {
                modellingItem.getTags().remove(tag)
                modellingItem.setChanged(true)
            }
        }
    }

    private void setAppearance(ULCCheckBox checkBox, Tag tag) {
        Color color = getColor(tag)
        checkBox.setForeground(color)
        checkBox.setBorder(BorderFactory.createLineBorder(color))
    }

    private Color getColor(Tag tag) {
        if (modellingItems.every { it.getTags().contains(tag) }) return Color.black
        if (modellingItems.any { it.getTags().contains(tag) }) return Color.gray
        return Color.black
    }

}
