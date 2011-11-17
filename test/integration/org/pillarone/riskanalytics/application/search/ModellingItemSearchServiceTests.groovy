package org.pillarone.riskanalytics.application.search

import models.core.CoreModel
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.pillarone.riskanalytics.application.search.DocumentFactory.ItemType
import org.pillarone.riskanalytics.core.fileimport.FileImportService
import org.pillarone.riskanalytics.core.simulation.item.ModellingItem
import org.pillarone.riskanalytics.core.simulation.item.Parameterization

class ModellingItemSearchServiceTests extends GroovyTestCase {


    void setUp() {
        FileImportService.importModelsIfNeeded(['Core', 'Application'])
    }

    void testService() {
        ModellingItemSearchService modellingItemSearchService = ApplicationHolder.application.mainContext.getBean(ModellingItemSearchService)
        final List<ModellingItem> results = modellingItemSearchService.search("*Parameters")

        assertEquals(4, results.size())

        Parameterization parameterization = new Parameterization("MyParameters", CoreModel)
        parameterization.save()

        results = modellingItemSearchService.search("*Parameters")

        assertEquals(5, results.size())

        assertNotNull(results.find { it.name == parameterization.name})

        parameterization.delete()
        results = modellingItemSearchService.search("*Parameters")

        assertEquals(4, results.size())

        assertNull(results.find { it.name == parameterization.name})

        assertEquals(4, modellingItemSearchService.search("* itemType:" + ItemType.PARAMETERIZATION.toString()).size())
        assertEquals(3, modellingItemSearchService.search("Core* itemType:" + ItemType.PARAMETERIZATION.toString()).size())

    }
}
