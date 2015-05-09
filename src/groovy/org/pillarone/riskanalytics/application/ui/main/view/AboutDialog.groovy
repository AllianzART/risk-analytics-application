package org.pillarone.riskanalytics.application.ui.main.view

import com.ulcjava.base.application.event.IActionListener
import com.ulcjava.base.application.util.BorderedComponentUtilities
import com.ulcjava.base.application.util.Color
import com.ulcjava.base.application.util.Dimension
import com.ulcjava.base.application.util.Font
import org.apache.log4j.Logger
import org.pillarone.riskanalytics.application.ui.base.view.FollowLinkPane
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.application.util.LocaleResources
import org.pillarone.riskanalytics.core.util.PropertiesUtils
import com.ulcjava.base.application.*

class AboutDialog {

    private static Logger sLogger = Logger.getLogger(AboutDialog.class)

    private ULCDialog dialog
    private ULCBoxPane mainContent
    private ULCWindow rootPane
    private ULCTabbedPane tabbedPane
    Closure closeAction = { event -> dialog.visible = false; dialog.dispose() }

    public AboutDialog(ULCWindow rootPane) {
        this.rootPane = rootPane
        initComponents()
        layoutComponents()
    }

    private void initComponents() {
        dialog = new ULCDialog(rootPane, getText("title"), true)
        dialog.setLocationRelativeTo(rootPane)
        dialog.size = new Dimension(550, 600)

        tabbedPane = new ULCTabbedPane()
        mainContent = new ULCBoxPane(1, 2)
    }

    private void layoutComponents() {
        tabbedPane.addTab(getText("about"), createMainTab())
        tabbedPane.addTab(getText("license"), createLicenseTab())
        tabbedPane.addTab(getText("credits"), BorderedComponentUtilities.createBorderedComponent(createCreditsTab(), ULCBoxPane.BOX_EXPAND_EXPAND, BorderFactory.createEmptyBorder(5, 5, 5, 5)))
        tabbedPane.addTab(getText("usedLibraries"), createUsedLibrariesTab())
        tabbedPane.addTab(getText("sysProps"), createPropertiesTab())
        mainContent.add(tabbedPane, ULCBoxPane.BOX_EXPAND_EXPAND)
        ULCButton closeButton = new ULCButton(getText("close"))
        closeButton.addActionListener([actionPerformed: closeAction] as IActionListener)
        mainContent.add(closeButton, ULCBoxPane.BOX_CENTER_BOTTOM)
        dialog.setContentPane(mainContent)
    }

    private ULCComponent createMainTab() {
        ULCBoxPane pane = new ULCBoxPane(1, 6, 5, 5)
        pane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
        pane.add(new ULCLabel(UIUtils.getIcon("ArtisanLogo37k.png")), ULCBoxPane.BOX_RIGHT_TOP)
        Properties infoProperties = new PropertiesUtils().getProperties("/version.properties")
        String appName = "RiskAnalytics ${infoProperties.getProperty("version", "N/A")}"
        ULCLabel appNameLabel = new ULCLabel(appName)
        appNameLabel.font = appNameLabel.font.deriveFont(Font.BOLD)
        pane.add(appNameLabel, ULCBoxPane.BOX_CENTER_TOP)
        String buildDate = "${infoProperties.getProperty("build.date", "N/A")}"
        pane.add(new ULCLabel(buildDate), ULCBoxPane.BOX_CENTER_TOP)
        String buildNo = "${getText("build#")}: ${infoProperties.getProperty("build.no", "N/A")}"
        pane.add(new ULCLabel(buildNo), ULCBoxPane.BOX_CENTER_TOP)

        FollowLinkPane htmlPane = new FollowLinkPane()
        String url = getText("url")
        htmlPane.setText("<a href='${url}'>${url}</a>")
        pane.add(htmlPane, ULCBoxPane.BOX_CENTER_TOP)
        pane
    }

    private ULCComponent createCreditsTab() {
        ULCBoxPane pane = new ULCBoxPane(2, 0)
        pane.background = Color.white
        pane.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ULCLabel title = new ULCLabel(getText("contributors"))
        title.font = title.font.deriveFont(Font.BOLD, 14)
        pane.add(ULCBoxPane.BOX_LEFT_TOP, title); pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCFiller(1, 10)); pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Ansorge Matthias, Canoo Engineering AG"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Bardola Jon, FS Consulta AG"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Brendle Detlef, Canoo Engineering AG"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Cartier Sebastian, Intuitive Collaboration AG"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Dittrich Joerg, Munich Re Group"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Ginsberg Benjamin, Intuitive Collaboration AG"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Hartmann Stephan, Munich Re Group"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Huber Martin, Canoo Engineering AG"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Huber Matthias, Canoo Engineering AG"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Jaada Fouad, Intuitive Collaboration AG"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Koenig Dierk, Canoo Engineering AG"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Kunz Stefan, Intuitive Collaboration AG"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Kuschel Norbert, Munich Re Group"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Lord Katja, Munich Re Group"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Majidi Ali, Munich Re Group"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Meier Markus, Intuitive Collaboration AG"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Melchior Martin, UAS Northwestern Switzerland"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Noe Michael, Munich Re Group"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Parten Simon, Allianz Risk Transfer"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Porzelt Johannes, Canoo Engineering AG"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Rahman Fazl, Allianz Risk Transfer"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Spahn Michael, Intuitive Collaboration AG"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Stricker Markus, Intuitive Collaboration AG"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Terry Chris, Allianz Risk Transfer"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Walter Jessika, Intuitive Collaboration AG"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Wassmer Arnold, Munich Re Group"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Wyss Manuel, UAS Northwestern Switzerland"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Zetterstrom Bjorn, Allianz Risk Transfer"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, new ULCLabel("Zumsteg Stefan, Intuitive Collaboration AG"));
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())

        //new ULCScrollPane(pane)
        pane
    }

    private ULCComponent createLicenseTab() {
        ULCHtmlPane pane = new ULCHtmlPane()
        pane.text = new Scanner(getClass().getResource("/gpl3.html").openStream()).useDelimiter("\\Z").next()
        new ULCScrollPane(pane)
    }

    private ULCComponent createUsedLibrariesTab() {
        def usedLibs = [
                ["Apache Commons","http://commons.apache.org"],
                ["COLT","http://acs.lbl.gov/~hoschek/colt"],
                ["Grails","http://www.grails.org"],
                ["Groovy","http://groovy.codehaus.org"],
                ["Hibernate","http://www.hibernate.org"],
                ["JasperReports","http://www.jasperforge.org"],
                ["Java","http://java.sun.com"],
                ["java-wikipedia-parser","http://code.google.com/p/java-wikipedia-parser"],
                ["JFreechart","http://www.jfree.org"],
                ["JodaTime","http://joda.sourceforge.net"],
                ["Lucene","http://lucene.apache.org"],
                ["POI","http://poi.apache.org"],
                ["Spring<","http://www.springsource.org"],
                ["SSJ","http://www.iro.umontreal.ca/~simardr/ssj"],
                ["UltraLightClient (ULC)","http://canoo.com/ulc"]
        ]
        ULCBoxPane pane = new ULCBoxPane(2, 0)
        pane.background = Color.white
        pane.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        StringBuilder builder = new StringBuilder("");
        builder.append("<p><b>")
        builder.append(getText("usedLibraries"))
        builder.append("</b></p><br>")
        builder.append("<table>")
        usedLibs.each { name, url ->
            builder.append("<tr><td align='left' >$name</td>")
            builder.append("<td align='left' ><a href='$url'>$url</a></td> ")
            builder.append("</tr>")
        }
        builder.append("</table>")

        FollowLinkPane htmlPane = new FollowLinkPane()
        htmlPane.setText(builder.toString())
        pane.add(ULCBoxPane.BOX_LEFT_TOP, htmlPane);
        pane.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())

        pane
    }


    private ULCComponent createPropertiesTab() {
        Map props = System.properties
        int propCount = props.keySet().size()
        Object[][] model = new Object[propCount][2]
        int row = 0
        props.keySet().sort().each { Object key ->
            model[row][0] = key
            model[row][1] = props.get(key)
            row++
        }
        ULCTable table = new ULCTable(new PropertiesTableModel(model))
        table.tableHeader = null
        new ULCScrollPane(table)
    }


    public void setVisible(boolean visible) {
        dialog.visible = visible
    }

    /**
     * Utility method to get resource bundle entries for this class
     *
     * @param key
     * @return the localized value corresponding to the key
     */
    protected String getText(String key) {
        return LocaleResources.getString("AboutDialog." + key);
    }
}


