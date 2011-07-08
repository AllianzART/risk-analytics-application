package org.pillarone.riskanalytics.application.reports.gira.model

import org.pillarone.riskanalytics.application.reports.gira.action.ResultPathParser
import org.pillarone.riskanalytics.core.simulation.item.Simulation
import org.pillarone.riskanalytics.application.reports.gira.action.PathType
import org.pillarone.riskanalytics.application.ui.result.model.ResultViewUtils
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
import org.pillarone.riskanalytics.core.simulation.item.parameter.comment.Comment
import org.pillarone.riskanalytics.application.ui.util.CommentUtils
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.application.reports.comment.action.CommentReportAction

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
class GiraReportHelper {

    protected Simulation simulation
    Map periodLabels = [:]

    public JRBeanCollectionDataSource getCommentsDataSource(String path, int periodIndex) {
        Collection currentValues = new ArrayList<Comment>()
        for (Comment comment: getComments(path, periodIndex)) {
            addCommentData(comment, currentValues)
        }

        JRBeanCollectionDataSource jrBeanCollectionDataSource = new JRBeanCollectionDataSource(currentValues);
        return jrBeanCollectionDataSource
    }

    public void addCommentData(Comment comment, Collection currentValues) {
        String boxTitle = CommentUtils.getCommentTitle(comment, simulation.modelClass)
        String commentInfo = CommentUtils.getCommentInfo(comment)
        String tags = CommentUtils.getTagsValue(comment).replaceAll("<br>", ", ")
        String addedFiles = UIUtils.getText(CommentReportAction.class, "attachments") + ": " + (comment.getFiles() as List).join(", ")
        currentValues << ["boxTitle": boxTitle, "commentInfo": commentInfo, "tags": tags, "addedFiles": addedFiles, "text": comment.getText()]
    }

    List<Comment> getComments(String path, int periodIndex) {
        List<Comment> comments = []
        AbstractReportModel.fieldNames.each {String fieldName ->
            String commentPath = path + ":" + fieldName
            Collection<Comment> pathFieldComments = simulation.comments.findAll {Comment comment ->
                comment.path == commentPath && (comment.period == -1 || comment.period == periodIndex)
            }
            comments.addAll(pathFieldComments)
        }
        return comments
    }

    String getPageTitle(ResultPathParser parser, String path, String type, int period) {
        String pageTitle = getComponentName(parser, path)
        pageTitle += ResultViewUtils.getResultNodesDisplayName(simulation?.modelClass, path)
        if (type)
            pageTitle += ", " + type
        String periodLabel = getPeriodLabel(period)
        pageTitle += "  Period starting at: " + periodLabel
        return pageTitle
    }

    String getComponentName(ResultPathParser parser, String path) {
        String pageTitle = ""
        PathType pathType = parser.getPathType(path)
        if (pathType)
            pageTitle += pathType.getDispalyName() + ": "
        return pageTitle
    }

    public String getPeriodLabel(int periodIndex) {
        String label
        if (periodLabels[periodIndex]) {
            label = periodLabels[periodIndex]
        } else {
            ResultViewUtils.initPeriodLabels(simulation.getSimulationRun(), periodLabels)
            label = periodLabels[periodIndex]
        }
        return label
    }

    public int getPeriodCount() {
        return simulation.periodCount
    }

    public static URL getReportFolder() {
        return GiraReportHelper.class.getResource("/reports")
    }
}
