/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.profiles

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProfileStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private lateinit var dataDir: File
    private lateinit var store: ProfileStore

    @Before
    fun setUp() {
        dataDir = tmp.newFolder("data")
        File(dataDir, "files").mkdirs()
        File(dataDir, "files/places.sqlite").writeText("default history")
        File(dataDir, "shared_prefs").mkdirs()
        File(dataDir, "shared_prefs/fenix_preferences.xml").writeText("default prefs")
        File(dataDir, "lib").mkdirs()
        File(dataDir, "code_cache").mkdirs()
        store = ProfileStore(dataDir)
    }

    @Test
    fun `GIVEN no profiles yet THEN there is one active default profile`() {
        assertEquals(listOf(ProfileStore.Profile("default", "Default")), store.profiles())
        assertEquals("default", store.active().id)
    }

    @Test
    fun `WHEN switching to a new profile THEN it starts empty and the old data is kept aside`() {
        val work = store.create("Work")
        store.requestSwitch(work.id)

        assertTrue(store.applyPendingSwitch())

        assertEquals(work, store.active())
        assertFalse(File(dataDir, "files").exists())
        assertFalse(File(dataDir, "shared_prefs").exists())
        assertTrue(File(dataDir, "lib").exists())
        assertTrue(File(dataDir, "code_cache").exists())
    }

    @Test
    fun `WHEN switching away and back THEN each profile gets its own data back`() {
        val work = store.create("Work")
        store.requestSwitch(work.id)
        store.applyPendingSwitch()
        File(dataDir, "files").mkdirs()
        File(dataDir, "files/places.sqlite").writeText("work history")

        store.requestSwitch("default")
        assertTrue(store.applyPendingSwitch())
        assertEquals("default history", File(dataDir, "files/places.sqlite").readText())
        assertEquals("default prefs", File(dataDir, "shared_prefs/fenix_preferences.xml").readText())

        store.requestSwitch(work.id)
        store.applyPendingSwitch()
        assertEquals("work history", File(dataDir, "files/places.sqlite").readText())
        assertFalse(File(dataDir, "shared_prefs").exists())
    }

    @Test
    fun `GIVEN no switch requested THEN nothing moves`() {
        assertFalse(store.applyPendingSwitch())
        assertEquals("default history", File(dataDir, "files/places.sqlite").readText())
    }

    @Test
    fun `GIVEN a switch interrupted after moving the old data out WHEN the app starts again THEN it completes`() {
        val work = store.create("Work")
        store.requestSwitch(work.id)
        store.applyPendingSwitch()
        File(dataDir, "files").mkdirs()
        File(dataDir, "files/places.sqlite").writeText("work history")
        store.requestSwitch("default")

        // Simulate a crash after the "out" phase: work's data is parked, the journal says "in".
        val root = File(dataDir, ProfileStore.ROOT_DIR)
        File(root, work.id).mkdirs()
        assertTrue(File(dataDir, "files").renameTo(File(root, "${work.id}/files")))
        val stateFile = File(root, "profiles.properties")
        stateFile.writeText(stateFile.readText() + "\nphase=in\n")

        assertTrue(ProfileStore(dataDir).applyPendingSwitch())
        assertEquals("default history", File(dataDir, "files/places.sqlite").readText())
        assertEquals("work history", File(root, "${work.id}/files/places.sqlite").readText())
    }

    @Test
    fun `WHEN deleting an inactive profile THEN its data is removed`() {
        val work = store.create("Work")
        store.requestSwitch(work.id)
        store.applyPendingSwitch()
        store.requestSwitch("default")
        store.applyPendingSwitch()

        store.delete(work.id)

        assertEquals(listOf("default"), store.profiles().map { it.id })
        assertFalse(File(dataDir, "${ProfileStore.ROOT_DIR}/${work.id}").exists())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `WHEN deleting the active profile THEN it is refused`() {
        store.delete("default")
    }

    @Test
    fun `WHEN renaming THEN the new name is stored`() {
        store.rename("default", "  Personal ")
        assertEquals("Personal", ProfileStore(dataDir).active().name)
    }
}
