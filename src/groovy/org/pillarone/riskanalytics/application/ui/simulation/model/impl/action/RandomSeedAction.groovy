package org.pillarone.riskanalytics.application.ui.simulation.model.impl.action
import com.ulcjava.base.application.ULCCheckBox
import com.ulcjava.base.application.ULCTextField
import com.ulcjava.base.application.event.IValueChangedListener
import com.ulcjava.base.application.event.ValueChangedEvent
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.simulation.model.impl.SimulationSettingsPaneModel
import org.pillarone.riskanalytics.application.ui.util.I18NAlert
import org.pillarone.riskanalytics.application.util.prefs.UserPreferences
import org.pillarone.riskanalytics.application.util.prefs.UserPreferencesFactory
/**
 * Configured by SimulationSettingsPane.initConfigProperties() where this object is
 * added as value change listener to the ULCTextfield random seed.
 *
 * A IValueChangedListener which is used for the random seed check box and text field.
 * If the event is fired from the TextField the new value is written to the model.
 * If it is fired from the checkbox, the model value is set to null (if disabled) or to the last entered value (if any) if enabled.
 */
class RandomSeedAction implements IValueChangedListener {

    private static Log LOG = LogFactory.getLog(RandomSeedAction)
    private final static Integer ONE = 1;  // TODO is this a bad idea in context of a VM ? Will it hinder GC ?

    private SimulationSettingsPaneModel model;
    private Integer oldRandomSeed
    private UserPreferences userPreferences = UserPreferencesFactory.userPreferences
    ULCTextField randomSeed

    public RandomSeedAction(SimulationSettingsPaneModel model) {
        this.model = model;
    }

    void setUserDefinedRandomSeedCheckBox(ULCCheckBox ulcCheckBox) {
        ulcCheckBox.addValueChangedListener(this)
        // set the initial value
        handleEvent(ulcCheckBox)
    }

    void valueChanged(ValueChangedEvent event) {
        handleEvent(event.source)
    }

    private handleEvent(ULCCheckBox checkBox) {
        if (checkBox.selected) {
            model.randomSeed = recentRandomSeed()
            randomSeed.value = model.randomSeed
        } else {
            LOG.info("User defined random seed disabled, setting to null.")
            oldRandomSeed = model.randomSeed
            model.randomSeed = null
        }
    }

    private handleEvent(ULCTextField textField) {
        try {
            Integer enteredRandomSeed = Integer.valueOf(textField?.text)
            if (enteredRandomSeed > 0) {
                model.randomSeed = enteredRandomSeed
                userPreferences.putPropertyValue(UserPreferences.RANDOM_SEED_USER_VALUE, "" + enteredRandomSeed)
                LOG.info("User defined random seed changed to ${enteredRandomSeed}.")
                // Mission accomplished
                //
                return
            }
        } catch( NumberFormatException e ){
            LOG.info("Caught '${e.toString()}'")
        }
        LOG.info("Resetting seed to 1.")
        new I18NAlert("NoInteger").show()
        model.randomSeed = ONE                  //Sometimes model propagates new value to textField
        if(! ONE.equals(textField.getValue())){
            textField.setValue( ONE )           //But sometimes this is needed too
        }
    }

    private Integer recentRandomSeed() {
        if (!oldRandomSeed) {
            String value = userPreferences.getPropertyValue(UserPreferences.RANDOM_SEED_USER_VALUE)
            if (value){
                try {
                    return Integer.valueOf(value)
                }catch(NumberFormatException e){
                }
            }
            // Fix bad number stored
            LOG.info("userPreferences for ${UserPreferences.RANDOM_SEED_USER_VALUE} was: '$value' - will return 1 to avoid any recursive shenanighans")
            userPreferences.putPropertyValue(UserPreferences.RANDOM_SEED_USER_VALUE, "" + ONE)
            return ONE
        }
        return oldRandomSeed
    }
}
