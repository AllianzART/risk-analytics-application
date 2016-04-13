package org.pillarone.riskanalytics.application.environment.shared;

import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;
import org.pillarone.riskanalytics.application.client.MetalTextFieldUI;
import org.pillarone.riskanalytics.application.client.WindowsTextFieldUI;
import org.pillarone.riskanalytics.core.util.Configuration;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.text.html.parser.ParserDelegator;
import java.awt.*;

public class UIManagerHelper {

    public static final int TOOLTIP_DISMISS_DELAY = 7000;

    public static void setLookAndFeel() {
        final boolean useNimbusTheme = "true".equalsIgnoreCase(System.getProperty("useNimbusTheme", "true"));
        if(useNimbusTheme) {
            System.out.println("using NimbusLookAndFeel ");
            System.err.println("using NimbusLookAndFeel ");
            setNimbusLookAndFeel();
        } else {
            System.out.println("Not using NimbusLookAndFeel ");
            System.err.println("Not using NimbusLookAndFeel ");
            if (isWindowsOS()) {
                setWindowsLookAndFeel();
            } else if (isLinux()) {
                setLinuxLookAndFeel();
            } else if (isMacOS()) {
                setMacLookAndFeel();
            } else setSystemLookAndFeel();
        }


        ToolTipManager.sharedInstance().setDismissDelay(TOOLTIP_DISMISS_DELAY);

        //workaround for http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6993073
        new ParserDelegator();
    }


    private static boolean isWindowsOS() {
        return getOS().contains("windows");
    }

    private static boolean isMacOS() {
        return getOS().startsWith("mac");
    }

    private static String getOS() {
        return System.getProperty("os.name").toLowerCase();
    }


    private static boolean isLinux() {
        return getOS().contains("linux");
    }


    private static void setLookAndFeel(LookAndFeel lookAndFeel) {
        try {
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setMacLookAndFeel() {
        setNimbusLookAndFeel();
    }

    private static void setWindowsLookAndFeel() {
        setLookAndFeel(new WindowsLookAndFeel());
        UIManager.put("TextFieldUI", WindowsTextFieldUI.class.getName());
    }

    public static void setLinuxLookAndFeel() {
        setLookAndFeel(new MetalLookAndFeel());
    }

    public static void setNimbusLookAndFeel() {
        UIManager.put("Table.showGrid", true);
        UIManager.put("Tree.drawHorizontalLines", true);
        UIManager.put("Tree.drawVerticalLines", true);
        UIManager.put("TextFieldUI", MetalTextFieldUI.class.getName());
        UIManager.put("TabbedPane.tabInsets", new Insets(0, 0, 0, 0));
        setLookAndFeel(new NimbusLookAndFeel());
    }

    public static void setTooltipDismissDelay() {
        ToolTipManager.sharedInstance().setDismissDelay(TOOLTIP_DISMISS_DELAY);
    }

    public static void setTextFieldUI() {
        if (isWindowsOS()) {
            UIManager.put("TextFieldUI", WindowsTextFieldUI.class.getName());
        } else if (isLinux()) {
            UIManager.put("TextFieldUI", MetalTextFieldUI.class.getName());
        } else {
            UIManager.put("TextFieldUI", MetalTextFieldUI.class.getName());
        }
    }

    public static void setParserDelegator() {
        //workaround for http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6993073
        new ParserDelegator();
    }
}
