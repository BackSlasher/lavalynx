/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.profiles

import android.content.Context
import android.content.Intent
import android.os.Process
import java.io.File
import java.io.IOException
import mozilla.components.support.base.log.logger.Logger

/** lavalynx: process-level glue for [ProfileStore]. */
object ProfileSwitcher {
    private val logger = Logger("ProfileSwitcher")

    /** The [ProfileStore] for this app's data directory. */
    fun store(context: Context): ProfileStore = ProfileStore(File(context.applicationInfo.dataDir))

    /** Completes a switch requested by the previous process. Call first thing in the main process. */
    fun applyPendingSwitch(base: Context) {
        try {
            if (store(base).applyPendingSwitch()) logger.info("Switched profile")
        } catch (e: IOException) {
            logger.error("Profile switch failed; it will be retried on the next start", e)
        }
    }

    /** Switches to the profile [id] by restarting the app. */
    fun switchTo(context: Context, id: String) {
        store(context).requestSwitch(id)
        val appContext = context.applicationContext
        val component = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)?.component
        appContext.startActivity(Intent.makeRestartActivityTask(component))
        Process.killProcess(Process.myPid())
    }
}
