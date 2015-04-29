package org.pillarone.riskanalytics.application.ui.view.viewlock

import models.application.ApplicationModel
import org.junit.Test
import org.pillarone.riskanalytics.application.ui.main.view.item.ParameterizationUIItem
import org.pillarone.riskanalytics.application.ui.main.view.item.ResourceUIItem
import org.pillarone.riskanalytics.core.example.component.ExampleResource
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.simulation.item.Resource

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class ViewLockServiceTests {
    @Test
    void testLock_successful() {
        ViewLockService viewLockService = new ViewLockService()


        def lockedNames = viewLockService.lock(new ParameterizationUIItem(new Parameterization("p", ApplicationModel)), "user1")

        assertEquals("wrong result size", 1, lockedNames.size())
        assertTrue("should be locked to user1", lockedNames.contains("user1"))
    }

    @Test
    void testLock_alreadyLocked() {
        ViewLockService viewLockService = new ViewLockService()
        ParameterizationUIItem item = new ParameterizationUIItem(new Parameterization("p", ApplicationModel))
        viewLockService.lock(item, "user1")

        Set<String> lockedNames = viewLockService.lock(item, "user2")

        assertEquals("wrong result size", 2, lockedNames.size())
        assertTrue("should be locked to user1", lockedNames.contains("user1"))
        assertTrue("should be locked to user2", lockedNames.contains("user2"))
    }

    @Test
    void testRelease() {
        ViewLockService viewLockService = new ViewLockService()
        ParameterizationUIItem item = new ParameterizationUIItem(new Parameterization("p", ApplicationModel))
        viewLockService.lock(item, "user1")

        viewLockService.release(item, "user1")
        Set<String> lockedNames = viewLockService.lock(item, "user2")

        assertEquals("wrong result size", 1, lockedNames.size())
        assertTrue("should be locked to user2", lockedNames.contains("user2"))
    }

    @Test
    void testReleaseAll_lockedByOneUser() {
        ViewLockService viewLockService = new ViewLockService()
        ParameterizationUIItem paramItem = new ParameterizationUIItem(new Parameterization("p", ApplicationModel))
        ResourceUIItem resourceItem = new ResourceUIItem(new Resource("R", ExampleResource))
        viewLockService.lock(paramItem, "user1")
        viewLockService.lock(resourceItem, "user1")

        viewLockService.releaseAll("user1")

        Set<String> paramItemLock = viewLockService.lock(paramItem, "user2")
        Set<String> resourceItemLock = viewLockService.lock(resourceItem, "user2")
        assertEquals("wrong result size", 1, paramItemLock.size())
        assertEquals("wrong result size", 1, resourceItemLock.size())
        assertTrue("paramItem should be locked to user2", paramItemLock.contains("user2"))
        assertTrue("resourceItem should be locked to user2", resourceItemLock.contains("user2"))
    }

    @Test
    void testReleaseAll_lockedByDifferentUsers() {
        ViewLockService viewLockService = new ViewLockService()
        ParameterizationUIItem paramItem = new ParameterizationUIItem(new Parameterization("p", ApplicationModel))
        ResourceUIItem resourceItem = new ResourceUIItem(new Resource("R", ExampleResource))
        viewLockService.lock(paramItem, "user1")
        viewLockService.lock(resourceItem, "user2")

        viewLockService.releaseAll("user1")

        Set<String> paramItemLock = viewLockService.lock(paramItem, "user3")
        assertEquals("wrong result size", 1, paramItemLock.size())
        assertTrue("should be locked to user3", paramItemLock.contains("user3"))

        Set<String> resourceItemLock = viewLockService.lock(resourceItem, "user3")
        assertEquals("wrong result size", 2, resourceItemLock.size())
        assertTrue("should be locked to user3", resourceItemLock.contains("user3"))
        assertTrue("should be locked to user2", resourceItemLock.contains("user2"))
    }
}
