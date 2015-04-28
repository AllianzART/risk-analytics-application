package org.pillarone.riskanalytics.application.ui.view.viewlock

import models.application.ApplicationModel
import org.junit.Test
import org.pillarone.riskanalytics.application.ui.main.view.item.ParameterizationUIItem
import org.pillarone.riskanalytics.application.ui.main.view.item.ResourceUIItem
import org.pillarone.riskanalytics.core.example.component.ExampleResource
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.simulation.item.Resource

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull

class ViewLockServiceTests {
    @Test
    void testLock_successful() {
        ViewLockService viewLockService = new ViewLockService()

        assertNull("lock result should be null", viewLockService.lock(new ParameterizationUIItem(new Parameterization("p", ApplicationModel)), "user1"))
    }

    @Test
    void testLock_alreadyLocked() {
        ViewLockService viewLockService = new ViewLockService()
        ParameterizationUIItem item = new ParameterizationUIItem(new Parameterization("p", ApplicationModel))
        assertNull("lock result should be null", viewLockService.lock(item, "user1"))

        assertEquals("should be locked to user1", "user1", viewLockService.lock(item, "user2"))
    }

    @Test
    void testRelease() {
        ViewLockService viewLockService = new ViewLockService()
        ParameterizationUIItem item = new ParameterizationUIItem(new Parameterization("p", ApplicationModel))
        viewLockService.lock(item, "user1")

        viewLockService.release(item)

        assertNull("lock result should be null", viewLockService.lock(item, "user2"))
    }

    @Test
    void testReleaseAll() {
        ViewLockService viewLockService = new ViewLockService()
        ParameterizationUIItem paramItem = new ParameterizationUIItem(new Parameterization("p", ApplicationModel))
        ResourceUIItem resourceItem = new ResourceUIItem(new Resource("R", ExampleResource))
        viewLockService.lock(paramItem, "user1")
        viewLockService.lock(resourceItem, "user1")

        viewLockService.releaseAll("user1")

        assertNull("lock result should be null", viewLockService.lock(paramItem, "user2"))
        assertNull("lock result should be null", viewLockService.lock(resourceItem, "user2"))
    }
}
