package org.pillarone.riskanalytics.application.ui

import com.ulcjava.base.client.ClientEnvironmentAdapter
import com.ulcjava.base.client.ISessionStateListener
import com.ulcjava.base.client.UISession
import com.ulcjava.base.shared.logging.LogManager
import com.ulcjava.base.shared.logging.SimpleLogManager
import org.pillarone.riskanalytics.application.environment.shared.UIManagerHelper
import org.pillarone.riskanalytics.application.ui.util.SplashScreen
import org.pillarone.riskanalytics.application.ui.util.SplashScreenHandler

class P1RATStandaloneLauncher {

    static void main(String[] args) {
        runApp()
    }

    static void start() {
        start(null)
    }

    static void start(ISessionStateListener customSessionStateListener) {
        P1RATStandaloneRunner runner = runApp()
        UISession clientSession = runner.getClientSession()

        if (customSessionStateListener != null) {
            clientSession.addSessionStateListener(customSessionStateListener)
        }

        StandaloneSessionStateListener listener = new StandaloneSessionStateListener()
        clientSession.addSessionStateListener(listener)
        synchronized (listener) {
            listener.wait()
        }
    }

    private static P1RATStandaloneRunner runApp() {
        LogManager logManager = new SimpleLogManager()
        LogManager.setLogManager(logManager)

        UIManagerHelper.setLookAndFeel()

        SplashScreenHandler splashScreenHandler = new SplashScreenHandler(new SplashScreen());
        ClientEnvironmentAdapter.setMessageService(splashScreenHandler);
        splashScreenHandler.showSplashScreen();

        P1RATStandaloneRunner runner = new P1RATStandaloneRunner()
        runner.start()
        return runner
    }
}

class StandaloneSessionStateListener implements ISessionStateListener {

    void sessionEnded(UISession session) throws Exception {
        println("PillarOne application shutdown ... cleaning up")
        synchronized (this) {
            notifyAll()
        }
    }

    void sessionError(UISession session, Throwable reason) {
        println("PillarOne application error..." + reason.getMessage())

    }

    void sessionStarted(UISession session) throws Exception {
        println("PillarOne application started...")

    }
}
