package org.pillarone.riskanalytics.application.ui.base.model

import com.ulcjava.base.application.ULCPollingTimer
import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.application.event.IActionListener
import com.ulcjava.base.application.tabletree.DefaultTableTreeModel
import com.ulcjava.base.application.tabletree.ITableTreeNode
import com.ulcjava.base.application.tree.TreePath
import groovy.transform.CompileStatic

import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import org.apache.commons.lang.builder.HashCodeBuilder
import org.pillarone.riskanalytics.application.ui.parameterization.model.AbstractCommentableItemTableTreeModel

@CompileStatic
abstract class AsynchronTableTreeModel extends AbstractCommentableItemTableTreeModel {
    ULCPollingTimer timer
    Map cellValues = [:]
    PollingAction pollingAction
    ExecutorService service
    LinkedBlockingQueue queue

    protected AsynchronTableTreeModel() {
        pollingAction = new PollingAction(this)

        queue = new LinkedBlockingQueue<Runnable>()
        service = new ThreadPoolExecutor(1, 3, 0L, TimeUnit.MILLISECONDS, queue)
    }

    ULCPollingTimer getPollingTimer() {
        if (!timer) {
            timer = new ULCPollingTimer(1000, pollingAction)
            timer.repeats = true
            timer.syncClientState = false
        }
        return timer
    }

    public final Object getValueAt(Object node, int column) {
        NodeIdentifier identifier = new NodeIdentifier(node: node as ITableTreeNode, columnName: getColumnName(column), columnIndex: column)
        if (!cellValues.containsKey(identifier)) {
            if (loadAsynchronous(column, node)) {
                Runnable callback = {
                    cellValues[identifier] = getAsynchronValue(node, column)
                    pollingAction.addNode(identifier)
                }
                service.submit(callback)

                if (!pollingTimer.running) {
                    pollingTimer.start()
                }
                return "..."
            } else {
                cellValues[identifier] = getAsynchronValue(node, column)
            }
        }
        return cellValues[identifier]
    }

    boolean hasPendingRequests() {
        return !queue.isEmpty()
    }

    abstract def getAsynchronValue(Object node, int column)

    protected boolean loadAsynchronous(int column, def node) {
        true
    }

    void clearCache() {
        cellValues.clear()
    }
}

@CompileStatic
class PollingAction implements IActionListener {
    AsynchronTableTreeModel model
    List requestedNodes = []

    public PollingAction(AsynchronTableTreeModel model) {
        this.model = model
    }


    public void actionPerformed(ActionEvent event) {
        List consumedNodes = []
        for (int i = 0; i < requestedNodes.size(); i++) {
            NodeIdentifier nodeIdentifier = requestedNodes[i]
            model.nodeChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(nodeIdentifier.node) as Object[]), nodeIdentifier.columnIndex)
            consumedNodes << nodeIdentifier
        }
        consumedNodes.each {
            requestedNodes.remove(it)
        }
        if (!model.hasPendingRequests()) {
            model.pollingTimer.stop()
        }
    }

    public addNode(NodeIdentifier identifier) {
        requestedNodes << identifier
    }
}

@CompileStatic
class NodeIdentifier {
    ITableTreeNode node
    String columnName
    //not used for identification, but needed for the nodeChanged event when the value was retrieved for the first time
    int columnIndex

    public boolean equals(Object obj) {
        if (!obj instanceof NodeIdentifier) {return false}
        return ((NodeIdentifier)obj).node == node && ((NodeIdentifier)obj).columnName == columnName
    }

    public int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder()
        hcb.append(node)
        hcb.append(columnName)
        return hcb.toHashCode()
    }


}