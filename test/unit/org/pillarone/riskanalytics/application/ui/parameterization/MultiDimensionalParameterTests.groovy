package org.pillarone.riskanalytics.application.ui.parameterization

import models.application.ApplicationModel
import org.pillarone.riskanalytics.application.example.constraint.LinePercentage
import org.pillarone.riskanalytics.application.util.LocaleResources
import org.pillarone.riskanalytics.core.example.marker.ITestComponentMarker
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.parameterization.*

class MultiDimensionalParameterTests extends GroovyTestCase {

    protected void setUp() {
        super.setUp();
        ConstraintsFactory.registerConstraint(new SimpleConstraint())
        ConstraintsFactory.registerConstraint(new LinePercentage())
    }


    void testGetPossibleValues() {
        LocaleResources.setTestMode(true)

        AbstractMultiDimensionalParameter param = new ComboBoxMatrixMultiDimensionalParameter([0], ['hierarchyComponent'], ITestComponentMarker)
        Model model = new ApplicationModel()
        model.init()
        model.injectComponentNames()
        param.simulationModel = model

        def values = param.getPossibleValues(0, 1)
        assertEquals 1, values.size()
        assertTrue values.contains('hierarchyComponent')


        param = new ConstrainedMultiDimensionalParameter([['hierarchyComponent'], [0d]], ['line', 'percentage'], ConstraintsFactory.getConstraints(LinePercentage.IDENTIFIER))
        model = new ApplicationModel()
        model.init()
        model.injectComponentNames()
        param.simulationModel = model

        values = param.getPossibleValues(1, 0)
        assertEquals 1, values.size()
        assertTrue values.contains('hierarchyComponent')

        values = param.getPossibleValues(1, 1)
        assertFalse values instanceof Collection

        LocaleResources.setTestMode(false)
    }

    void testGetValuesAsObjects() {
        AbstractMultiDimensionalParameter param = new ComboBoxTableMultiDimensionalParameter(['hierarchyComponent', 'hierarchyComponent'], ['line'], ITestComponentMarker)
        Model model = new ApplicationModel()
        model.init()
        model.injectComponentNames()
        param.simulationModel = model

        List lobs = param.getValuesAsObjects(0, true)
        assertEquals 2, lobs.size()
        assertEquals 'hierarchyComponent', lobs.get(0).name
        assertEquals 'hierarchyComponent', lobs.get(1).name

        param = new ComboBoxTableMultiDimensionalParameter([['hierarchyComponent', 'hierarchyComponent'], ['hierarchyComponent', 'hierarchyComponent']], ['line 1', 'line 2'], ITestComponentMarker)
        model = new ApplicationModel()
        model.init()
        model.injectComponentNames()
        param.simulationModel = model

        lobs = param.getValuesAsObjects()
        assertEquals 2, lobs.size()

        assertEquals 'hierarchyComponent', lobs.get(0).get(0).name
        assertEquals 'hierarchyComponent', lobs.get(0).get(1).name
        assertEquals 'hierarchyComponent', lobs.get(1).get(0).name
        assertEquals 'hierarchyComponent', lobs.get(1).get(1).name
    }

}