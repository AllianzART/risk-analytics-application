package org.pillarone.riskanalytics.application.ui.view.viewlock

import org.apache.commons.lang.StringUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.main.view.item.ModellingUIItem

class ViewLockService {
    private Map<ModellingUIItem, String> lockMap = new HashMap<>()
    private static final Log LOG = LogFactory.getLog(ViewLockService)

    /**
     *
     * @param item to lock
     * @param session to lock the item for
     * @return null if the lock could be obtained, otherwise the users name to which the item is locked for.
     */
    public synchronized String lock(ModellingUIItem item, String username) {
        if(StringUtils.isEmpty(username) || item == null) {
            LOG.warn("Cannot acquire lock. Either username or item is null.")
            return
        }
        if (lockMap.containsKey(item)) {
            return lockMap.get(item)
        } else {
            lockMap.put(item, username)
            return null
        }
    }

    public synchronized void release(ModellingUIItem item) {
        if(lockMap.containsKey(item)) {
            lockMap.remove(item)
        }
    }

    public synchronized void releaseAll(String username) {
        if(StringUtils.isEmpty(username)) {
            return
        }
        Iterator<MapEntry> iterator = lockMap.iterator()
        while (iterator.hasNext()) {
            Map.Entry mapEntry = iterator.next()
            if(username.equals(mapEntry.getValue())) {
                iterator.remove()
            }
        }
    }
}
