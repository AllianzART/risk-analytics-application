package models.application

import org.pillarone.riskanalytics.core.output.AggregatedCollectingModeStrategy
import org.pillarone.riskanalytics.core.output.SingleValueCollectingModeStrategy

model = ApplicationModel
periodCount = 1

components {
    dynamicComponent {
        subsubcomponents {
            outFirstValue = AggregatedCollectingModeStrategy.IDENTIFIER
            outSecondValue = AggregatedCollectingModeStrategy.IDENTIFIER
        }
    }
    composedComponent {
        subDynamicComponent {
            subsubcomponents {
                outFirstValue = SingleValueCollectingModeStrategy.IDENTIFIER
                outSecondValue = SingleValueCollectingModeStrategy.IDENTIFIER
            }
        }
    }
}