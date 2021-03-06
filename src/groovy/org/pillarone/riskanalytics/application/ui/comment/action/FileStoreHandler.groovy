package org.pillarone.riskanalytics.application.ui.comment.action

import com.ulcjava.base.application.ULCAlert
import com.ulcjava.base.application.ULCComponent
import com.ulcjava.base.application.UlcUtilities
import com.ulcjava.base.application.util.IFileStoreHandler
import groovy.transform.CompileStatic
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.util.I18NAlert

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
@CompileStatic
class FileStoreHandler implements IFileStoreHandler {
    byte[] content
    ULCComponent source
    Log LOG = LogFactory.getLog(FileStoreHandler)

    public FileStoreHandler(byte[] content, ULCComponent source) {
        this.content = content
        this.source = source
    }

    void prepareFile(OutputStream outputStream) {
        outputStream.write content
    }

    void onSuccess(String filePath, String fileName) {
        LOG.info "file ${filePath} succesfully downloaded"
    }

    void onFailure(int reason, String description) {
        if (IFileStoreHandler.CANCELLED != reason) {
            LOG.error description
            ULCAlert alert = new I18NAlert(UlcUtilities.getWindowAncestor(source), "importError")
            alert.show()
        }
    }


}
