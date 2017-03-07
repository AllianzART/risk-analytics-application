package org.pillarone.riskanalytics.application.ui.main.action

import com.ulcjava.base.application.ULCTableTree
import com.ulcjava.base.application.event.ActionEvent
import grails.util.Holders
import org.apache.commons.lang.StringUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.main.eventbus.event.OpenDetailViewEvent
import org.pillarone.riskanalytics.application.ui.main.view.DetailViewManager
import org.pillarone.riskanalytics.application.ui.main.view.TagsListView
import org.pillarone.riskanalytics.application.ui.main.view.item.UploadBatchUIItem
import org.pillarone.riskanalytics.application.ui.result.model.SimulationNode
import org.pillarone.riskanalytics.application.ui.upload.view.UploadBatchView
import org.pillarone.riskanalytics.core.simulation.item.Simulation
import org.pillarone.riskanalytics.core.util.Configuration
import org.pillarone.riskanalytics.core.workflow.Status

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

class UploadSimulationAction extends SelectionTreeAction {
    private static final Log log = LogFactory.getLog(UploadSimulationAction)
    UploadBatchUIItem item = new UploadBatchUIItem()

    UploadSimulationAction(ULCTableTree tree) {
        super('UploadSimulationAction', tree)
    }
    @Override
    protected List allowedRoles() {
        return ['ROLE_REVIEWER']
    }

    @Override
    void doActionPerformed(ActionEvent event) {
        if(enabled){
            ArrayList<Simulation> qtrSims = validateSelectedSims()
            if(qtrSims.empty){
                return
            }

            riskAnalyticsEventBus.post(new OpenDetailViewEvent(item))
            UploadBatchView uploadBatchView = detailViewManager.openDetailView as UploadBatchView
            uploadBatchView.addSimulations(qtrSims)
        }
    }

    //[AR-122] exclude :
    // - non quarter-tagged sims
    // - AZRe sims
    // - sims whose model lacks qtr tag or not IN PRODUCTION
    //
    private List<Simulation> validateSelectedSims() {
        if(! TagsListView.quarterTagsAreSpecial){
            log.warn("Please note: quarterTagsAreSpecial is disabled. But uploads still check quarter tags..")
        }
        List<Simulation> selectedSims = getSimulations()
        selectedSims.each { sim ->
            if(!sim.isLoaded()){
               sim.load(true) // need the tags
        }}
        List<Simulation> qtrSims = selectedSims.findAll { sim ->
            sim?.tags?.any({ it.isQuarterTag() })
        }

        int badCount = selectedSims.size() - qtrSims.size()
        if (0 < badCount) {
            Simulation example = selectedSims.find { !qtrSims.contains(it) }
            String title = "Only tagged (non azre) sims can upload"
            String body = "Oops! ($badCount) non quarter-tagged sims skipped.\n(No quarter tag found on ${(badCount > 1) ? 'e.g. ' : ''} ${example})"
            showInfoAlert(title, body, true)
        }

        selectedSims.each { sim ->
            if(!sim.parameterization?.isLoaded()){
                sim.parameterization?.load(true) // need the tags
            }}
        List<Simulation> azReSims = qtrSims.findAll { sim ->
            sim?.getParameterization()?.tags?.any({ it.isAZReTag() })
        }

        badCount = azReSims.size()
        if (badCount > 0) {
            String title = "Only IT-Apps can upload AZRe sims"
            String body = "Oops! ($badCount) AZRe sims skipped.\nE.g. ${azReSims.first().parameterization.nameAndVersion} is an AZRe model."
            showInfoAlert(title, body, true)
            qtrSims.removeAll(azReSims)
        }

        // Do models carry the quarter tag ?
        //
        List<Simulation> modelsLackingQtrTag = qtrSims.findAll { final sim ->
            ! sim?.getParameterization()?.tags?.any({ pt -> pt == sim.tags.find{ st -> st.isQuarterTag() }})
        }
        badCount = modelsLackingQtrTag.size()
        if (badCount > 0) {
            String title = "Non-quarter-tagged models skipped"
            String body = "Oops! MODEL behind ($badCount) sims lack matching Quarter Tag.\n(E.g. ${modelsLackingQtrTag.first().parameterization.nameAndVersion})."
            showInfoAlert(title, body, true)
            qtrSims.removeAll(modelsLackingQtrTag)
        }

        // Are models in PRODUCTION state ?
        //
        List<Simulation> nonProdModels = qtrSims.findAll { sim ->
            Status.IN_PRODUCTION != sim?.getParameterization()?.status
        }
        badCount = nonProdModels.size()
        if (badCount > 0) {
            String title = "Non-Production models skipped"
            String body = "Oops! ($badCount) sims lack PRODUCTION model status.\nE.g. ${nonProdModels.first().parameterization.nameAndVersion} is not IN PRODUCTION."
            showInfoAlert(title, body, true)
            qtrSims.removeAll(nonProdModels)
        }

        // Skip already uploaded sims
        //
        if(Configuration.coreGetAndLogStringConfig("disableAlreadyUploadedSimCheck", "false", log).equalsIgnoreCase("false")){
            List<Simulation> alreadyUploaded = alreadyUploadedSims( qtrSims );
            badCount = (alreadyUploaded == null) ? 0 : alreadyUploaded.size();
            if (badCount > 0) {
                showInfoAlert(
                    "Cannot re-upload old sims",
                    "Oops! ($badCount) sims already uploaded.\nE.g. ${alreadyUploaded.first().nameAndVersion} is already uploaded.",
                    true
                )
                qtrSims.removeAll(alreadyUploaded)
            }
        }

        return qtrSims
    }

    // Get a boring old style connection and use it across the whole list
    //
    private List<Simulation> alreadyUploadedSims( List<Simulation> sims ){
        Connection conn = null;
        try{
            conn = connectUploadsDB();
            List<Simulation> alreadyUploaded = sims.findAll { sim -> isAlreadyUploaded(sim, conn) }
            return  alreadyUploaded;
        }catch( Exception e ){
            log.warn("Failure querying already-uploaded sims\n"+e.getMessage(), e);
        }finally{
            if(conn != null){
              conn.close(); // no txn, just querying data
            }
        }
    }

    private static boolean isAlreadyUploaded(Simulation sim, Connection conn){
        log.info(String.format("Doing isAlreadyUploaded() sim=%s (id=%d)", sim.getName(), sim.id));
        final String sql =
            "select * from ArtisanImport.dbo.PricemodellingSimulationNumber where simulationId=" + sim.id;

        Statement statement = null;
        try{
            statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            return resultSet.next();
        }catch( SQLException sqx){
            final String failed ="FAILED to query sim upload: ["+sql+"]";
            log.info(failed, sqx);
            throw new IllegalStateException(failed+"\n"+sqx.getMessage());
        }finally{
            if(statement!=null){
                statement.close(); // also closes resultset etc
            }

        }
    }

    private Connection connectUploadsDB() {
        final String connectionString = "jdbc:jtds:sqlserver://ART-SQL-APPS-PROD.art-allianz.com/ArtisanImport;integratedSecurity=true;";
        final String JDBC_DRIVER = "net.sourceforge.jtds.jdbc.Driver";
        final String AUTH_CONN_SUFFIX = ";useNTLMv2=tru‌​e;domain=ART-ALLIANZ";
        final String username = "ArtisanUser";
        final String password = Configuration.coreGetAndLogStringConfig("ArtisanDBPwd","", log);
        try{
            if( connectionString != null ){
                loadJDBCDriver(JDBC_DRIVER);
                if(StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)){
                    connectionString += AUTH_CONN_SUFFIX;
                    log.info("Getting auth conn: " + connectionString);
                    return DriverManager.getConnection(connectionString,username,password);
                }else{
                    log.info("Getting simple (no credentials) conn: " + connectionString);
                    return DriverManager.getConnection( connectionString );
                }
            }else{ // null connnectionString
                throw new IllegalArgumentException("null connnectionString");
            }

        }catch(ClassNotFoundException cnf){
            log.info("FAILED to load JDBC driver: "+JDBC_DRIVER, cnf);
            throw new IllegalArgumentException("Failed to connect to DB", cnf);
        }catch(SQLException sqx){
            log.info("FAILED to connect with: "+connectionString, sqx);
            throw new IllegalArgumentException("Failed to connect to DB\n"+sqx.getMessage(), sqx);
        }
    }

    private void loadJDBCDriver(String name) throws ClassNotFoundException {
        Class.forName(name);
        log.info("Loaded JDBC Driver: " + name);
    }
    
    

    private List<Simulation> getSimulations() {
        List<SimulationNode> simulationNodes = getSelectedObjects(Simulation).findAll {
            it instanceof SimulationNode
        } as List<SimulationNode>
        return simulationNodes ? simulationNodes.itemNodeUIItem.item : []
    }

    private DetailViewManager getDetailViewManager() {
        Holders.grailsApplication.mainContext.getBean('detailViewManager', DetailViewManager)
    }
}