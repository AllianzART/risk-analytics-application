package org.pillarone.riskanalytics.application.ui.view.viewlock

import org.apache.commons.lang.StringUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.main.view.item.ModellingUIItem

class ViewLockService {
    private final Map<ModellingUIItem, Set<String>> lockMap = new HashMap<>()
    private static final Log LOG = LogFactory.getLog(ViewLockService)

    /**
     *
     * @param item to lock
     * @param session to lock the item for
     * @return the users names to which the item is locked for (including new user). Null if either username or item was null
     */
    public Set<String> lock(ModellingUIItem item, String username) {
        if (item == null) {
            throw new IllegalArgumentException("Item must not be null")
        }
        if (StringUtils.isEmpty(username)) {
            LOG.warn("Cannot acquire lock. Either username or item is null.")
            return new HashSet<String>()
        }
        synchronized (lockMap) {
            if (lockMap.containsKey(item)) {
                Set<String> names = lockMap.get(item)
                names.add(username)
                return names
            } else {
                Set<String> names = new HashSet<String>()
                names.add(username)
                lockMap.put(item, names)
                return names
            }
        }
    }

    public void release(ModellingUIItem item, String username) {
        synchronized (lockMap) {
            if (item != null && lockMap.containsKey(item)) {
                Set<String> names = lockMap.get(item)
                if (names.contains(username)) {
                    if (names.size() == 1) {
                        lockMap.remove(item)
                    } else {
                        names.remove(username)
                    }
                }
            }
        }
    }

    public void releaseAll(String username) {
        if(StringUtils.isEmpty(username)) {
            return
        }
        synchronized (lockMap) {
            Iterator<MapEntry> iterator = lockMap.iterator()
            while (iterator.hasNext()) {
                Map.Entry<ModellingUIItem, Set<String>> mapEntry = iterator.next()
                Set<String> names = (Set<String>)mapEntry.getValue()
                if (names != null && names.contains(username)) {
                    if (names.size() == 1) {
                        iterator.remove()
                    } else {
                        names.remove(username)
                    }
                }
            }
        }
    }
}
