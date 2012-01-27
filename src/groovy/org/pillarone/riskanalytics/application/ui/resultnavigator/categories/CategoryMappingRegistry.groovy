package org.pillarone.riskanalytics.application.ui.resultnavigator.categories

import org.pillarone.riskanalytics.application.ui.resultnavigator.examples.PodraModelOutputCategories
import org.pillarone.riskanalytics.core.output.SimulationRun

/**
 * @author martin.melchior
 */
class CategoryMappingRegistry {

    static Map<SimulationRun, CategoryMapping> cache = [:] // should probably be changed to thread-local

    static CategoryMapping getCategoryMapping(SimulationRun run) {
        if (cache.containsKey(run)) {
            return cache[run]
        }
        if (run.getModel()) {
            // Model model =  // TODO: instantiate the model here and get the closure from a suitable method (?)
            def mappingClosure = new PodraModelOutputCategories().mappingClosure
            Map<String, ICategoryResolver> categories = MapCategoriesBuilder.getCategories (mappingClosure)
            CategoryMapping mapping = new CategoryMapping(categories)
            cache[run] = mapping
            return mapping
        } else {
            CategoryMapping mapping = new CategoryMapping()
            cache[run] = mapping
            return mapping
        }
    }
}
