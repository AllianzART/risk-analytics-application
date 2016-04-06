package org.pillarone.riskanalytics.application.ui.upload.queue.view.action

import com.ulcjava.base.application.event.ActionEvent
import org.pillarone.riskanalytics.application.ui.UlcSessionScope
import org.pillarone.riskanalytics.application.ui.base.action.ResourceBasedAction
import org.pillarone.riskanalytics.application.ui.upload.queue.view.UploadQueueView
import org.pillarone.riskanalytics.core.upload.UploadRuntimeService
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.annotation.Resource

@Scope(UlcSessionScope.ULC_SESSION_SCOPE)
@Component
class RegisterNotificationOnUploadEntryAction extends ResourceBasedAction {

    @Resource
    UploadRuntimeService uploadRuntimeService

    @Resource
    UploadQueueView uploadQueueView


    RegisterNotificationOnUploadEntryAction() {
        super('RegisterNotification')
    }

    @Override
    void doActionPerformed(ActionEvent event) {
        if (enabled) {
            uploadQueueView.selectedUploads.each {
                uploadRuntimeService.registerForNotificationOnQueueEntry(it.id, it.getUsername())
            }
        }
    }

    @Override
    boolean isEnabled() {
        return uploadQueueView.selectedUploads.size() > 0
    }
}
