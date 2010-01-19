package org.pillarone.riskanalytics.application.reports

import net.sf.jasperreports.engine.JRChartCustomizer
import org.jfree.chart.JFreeChart
import net.sf.jasperreports.engine.JRChart
import org.jfree.chart.block.BlockBorder

public class RemoveLegendBorderCustomizer implements JRChartCustomizer{

    public void customize(JFreeChart jFreeChart, JRChart jrChart) {
        jFreeChart?.getLegend()?.frame = BlockBorder.NONE
    }

}