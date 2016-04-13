<%@ page import="grails.util.Holders" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.codehaus.groovy.grails.plugins.GrailsPluginManager" %>
<%@ page contentType="application/x-java-jnlp-file" language="java" %>
<%
    GrailsPluginManager pluginManager = Holders.getGrailsApplication().getMainContext().getBean("pluginManager", GrailsPluginManager.class);
    String ulcPluginPath = pluginManager.getPluginPath("ulc");
%>
<?xml version="1.0" encoding="ISO-8859-1"?>
<jnlp spec="1.0+" codebase="${codebase}">
    <information>
        <title>ARTisan</title>
        <vendor>Allianz Risk Transfer</vendor>
        <description>A modelling toolbox for the ART community.</description>
        <homepage href="zh-jira-01.art-allianz.com:8090/display/AR/Artisan+Home"/>
        <icon href="/images/PillarOneLogoSmall.png" width="48" height="48"/>
    </information>

    <security>
        <all-permissions/>
    </security>

    <resources>
        <j2se version="1.5+"/>
        <jar href=".<%=ulcPluginPath%>/lib/ulc-core-client.jar"/>
        <g:each in="${clientFiles}" var="clientFile">
            <jar href="${clientFile}"/>
        </g:each>
    </resources>

    <resources os="Windows">
        <j2se version="1.5+"/>
    </resources>

    <application-desc main-class="org.pillarone.riskanalytics.application.environment.jnlp.P1RATJNLPLauncher">
        <argument>url-string=${ulcEndpoint}</argument>
        <argument>keep-alive-interval=60</argument>
        <argument>log-level=WARNING</argument>
        <argument>ViewFactory=org.pillarone.riskanalytics.application.environment.jnlp.P1RATFrameViewFactory</argument>
    </application-desc>

</jnlp>
