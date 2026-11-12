/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.profiles

import java.io.File
import java.io.IOException
import java.util.Properties

/**
 * lavalynx: browser profiles.
 *
 * The active profile's data is the app's data directory itself, exactly as Firefox lays it out, so nothing else in the
 * app (Gecko, Places, logins, preferences, caches) needs to know profiles exist. Every inactive profile is parked under
 * [ROOT_DIR]/<id>/. Switching records the target in [ROOT_DIR] and restarts the process; the next main process calls
 * [applyPendingSwitch] before anything opens a file, which moves the current data out and the target's in. The moves
 * are renames on one filesystem, and the switch is journalled, so a crash part-way resumes on the next start.
 *
 * @param dataDir The app's data directory (`applicationInfo.dataDir`).
 */
class ProfileStore(private val dataDir: File) {

    /**
     * A browser profile.
     *
     * @property id Stable identifier, also the name of its parking directory.
     * @property name Name shown to the user.
     */
    data class Profile(val id: String, val name: String)

    private val root = File(dataDir, ROOT_DIR)
    private val stateFile = File(root, STATE_FILE)

    /** All profiles, in creation order. */
    fun profiles(): List<Profile> = load().profiles

    /** The profile whose data is currently in use. */
    fun active(): Profile = load().let { state -> state.profiles.first { it.id == state.active } }

    /** Creates a new, empty profile named [name]. */
    fun create(name: String): Profile {
        val state = load()
        val id = generateSequence(1) { it + 1 }.map { "p$it" }.first { id -> state.profiles.none { it.id == id } }
        val profile = Profile(id, name.trim().ifEmpty { id })
        save(state.copy(profiles = state.profiles + profile))
        return profile
    }

    /** Renames the profile [id] to [name]. */
    fun rename(id: String, name: String) {
        val state = load()
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        save(state.copy(profiles = state.profiles.map { if (it.id == id) it.copy(name = trimmed) else it }))
    }

    /** Deletes the profile [id] and all of its data. The active profile cannot be deleted. */
    fun delete(id: String) {
        val state = load()
        require(id != state.active) { "cannot delete the active profile" }
        require(id != state.pending) { "cannot delete a profile that is being switched to" }
        File(root, id).deleteRecursively()
        save(state.copy(profiles = state.profiles.filterNot { it.id == id }))
    }

    /** Marks [id] to become the active profile on the next start of the main process. */
    fun requestSwitch(id: String) {
        val state = load()
        require(state.profiles.any { it.id == id }) { "unknown profile $id" }
        if (state.phase != null) return
        save(state.copy(pending = id.takeIf { it != state.active }))
    }

    /**
     * Completes a requested switch, if any. Must run in the main process before anything reads app data.
     *
     * @return Whether the active profile changed.
     */
    @Throws(IOException::class)
    fun applyPendingSwitch(): Boolean {
        var state = load()
        val target = state.pending ?: return false
        if (state.profiles.none { it.id == target }) {
            save(state.copy(pending = null, phase = null))
            return false
        }

        if (state.phase == null) {
            state = state.copy(phase = PHASE_OUT)
            save(state)
        }
        if (state.phase == PHASE_OUT) {
            val parked = File(root, state.active).apply { mkdirs() }
            for (entry in movableEntries()) {
                move(entry, File(parked, entry.name))
            }
            state = state.copy(phase = PHASE_IN)
            save(state)
        }
        val incoming = File(root, target)
        for (entry in incoming.listFiles().orEmpty()) {
            val destination = File(dataDir, entry.name)
            // Anything here was created after the old profile moved out, so it belongs to neither.
            if (destination.exists()) destination.deleteRecursively()
            move(entry, destination)
        }
        incoming.delete()
        save(state.copy(active = target, pending = null, phase = null))
        return true
    }

    private fun movableEntries(): List<File> = dataDir.listFiles().orEmpty().filterNot { it.name in KEPT_ENTRIES }

    private fun move(from: File, to: File) {
        if (!from.renameTo(to)) throw IOException("could not move $from to $to")
    }

    private data class State(
        val profiles: List<Profile>,
        val active: String,
        val pending: String? = null,
        val phase: String? = null,
    )

    private fun load(): State {
        val props = Properties()
        if (stateFile.exists()) stateFile.inputStream().use { props.load(it) }
        val ids = props.getProperty(KEY_IDS).orEmpty().split(',').filter { it.isNotBlank() }
        val profiles =
            ids.map { Profile(it, props.getProperty("$KEY_NAME_PREFIX$it") ?: it) }
                .ifEmpty {
                    listOf(Profile(DEFAULT_ID, DEFAULT_NAME))
                }
        val active = props.getProperty(KEY_ACTIVE)?.takeIf { id -> profiles.any { it.id == id } } ?: profiles.first().id
        return State(
            profiles = profiles,
            active = active,
            pending = props.getProperty(KEY_PENDING)?.takeIf { it.isNotBlank() },
            phase = props.getProperty(KEY_PHASE)?.takeIf { it.isNotBlank() },
        )
    }

    private fun save(state: State) {
        root.mkdirs()
        val props = Properties()
        props.setProperty(KEY_IDS, state.profiles.joinToString(",") { it.id })
        state.profiles.forEach { props.setProperty("$KEY_NAME_PREFIX${it.id}", it.name) }
        props.setProperty(KEY_ACTIVE, state.active)
        state.pending?.let { props.setProperty(KEY_PENDING, it) }
        state.phase?.let { props.setProperty(KEY_PHASE, it) }
        val tmp = File(root, "$STATE_FILE.tmp")
        tmp.outputStream().use {
            props.store(it, null)
            it.fd.sync()
        }
        if (!tmp.renameTo(stateFile)) throw IOException("could not write $stateFile")
    }

    companion object {
        /** Directory under the app's data directory that holds profile bookkeeping and inactive profiles. */
        const val ROOT_DIR = "lavalynx_profiles"
        private const val STATE_FILE = "profiles.properties"
        private const val DEFAULT_ID = "default"
        private const val DEFAULT_NAME = "Default"
        private const val KEY_IDS = "ids"
        private const val KEY_NAME_PREFIX = "name."
        private const val KEY_ACTIVE = "active"
        private const val KEY_PENDING = "pending"
        private const val KEY_PHASE = "phase"
        private const val PHASE_OUT = "out"
        private const val PHASE_IN = "in"

        // Entries that belong to the installed app rather than to a profile.
        private val KEPT_ENTRIES = setOf(ROOT_DIR, "lib", "code_cache")
    }
}
