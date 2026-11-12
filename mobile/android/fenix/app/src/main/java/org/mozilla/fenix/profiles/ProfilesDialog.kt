/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.profiles

import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.mozilla.fenix.R

/**
 * lavalynx: the profile manager, a short chain of dialogs opened from Settings.
 *
 * @param context Themed context to show the dialogs in.
 * @param onChanged Called after a profile was created, renamed or deleted.
 */
class ProfilesDialog(private val context: Context, private val onChanged: () -> Unit) {
    private val store = ProfileSwitcher.store(context)

    /** Shows the profile list. */
    fun show() {
        val profiles = store.profiles()
        val active = store.active()
        val labels =
            profiles
                .map {
                    if (it.id == active.id) context.getString(R.string.lavalynx_profiles_active, it.name) else it.name
                }
                .toTypedArray()
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.lavalynx_profiles)
            .setItems(labels) { _, which -> showActions(profiles[which], profiles[which].id == active.id) }
            .setPositiveButton(R.string.lavalynx_profiles_new) { _, _ -> showNameInput(null) }
            .setNegativeButton(R.string.lavalynx_profiles_cancel, null)
            .show()
    }

    private fun showActions(profile: ProfileStore.Profile, isActive: Boolean) {
        val actions = buildList {
            if (!isActive) add(R.string.lavalynx_profiles_switch to { confirmSwitch(profile) })
            add(R.string.lavalynx_profiles_rename to { showNameInput(profile) })
            if (!isActive) add(R.string.lavalynx_profiles_delete to { confirmDelete(profile) })
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(profile.name)
            .setItems(actions.map { context.getString(it.first) }.toTypedArray()) { _, which ->
                actions[which].second()
            }
            .setNegativeButton(R.string.lavalynx_profiles_cancel, null)
            .show()
    }

    private fun showNameInput(existing: ProfileStore.Profile?) {
        val input =
            EditText(context).apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                hint = context.getString(R.string.lavalynx_profiles_name_hint)
                setText(existing?.name.orEmpty())
                setSingleLine()
            }
        val padding = context.resources.getDimensionPixelSize(R.dimen.lavalynx_profiles_dialog_padding)
        val container =
            FrameLayout(context).apply {
                setPadding(padding, padding / 2, padding, 0)
                addView(input)
            }
        MaterialAlertDialogBuilder(context)
            .setTitle(if (existing == null) R.string.lavalynx_profiles_new else R.string.lavalynx_profiles_rename)
            .setView(container)
            .setPositiveButton(
                if (existing == null) R.string.lavalynx_profiles_create else R.string.lavalynx_profiles_rename
            ) { _, _ ->
                val name = input.text.toString()
                if (existing == null) {
                    val created = store.create(name)
                    onChanged()
                    confirmSwitch(created)
                } else {
                    store.rename(existing.id, name)
                    onChanged()
                }
            }
            .setNegativeButton(R.string.lavalynx_profiles_cancel, null)
            .show()
    }

    private fun confirmSwitch(profile: ProfileStore.Profile) {
        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.lavalynx_profiles_switch_now, profile.name))
            .setMessage(R.string.lavalynx_profiles_switch_explanation)
            .setPositiveButton(R.string.lavalynx_profiles_switch_button) { _, _ ->
                ProfileSwitcher.switchTo(context, profile.id)
            }
            .setNegativeButton(R.string.lavalynx_profiles_cancel, null)
            .show()
    }

    private fun confirmDelete(profile: ProfileStore.Profile) {
        MaterialAlertDialogBuilder(context)
            .setTitle(profile.name)
            .setMessage(context.getString(R.string.lavalynx_profiles_delete_confirm, profile.name))
            .setPositiveButton(R.string.lavalynx_profiles_delete) { _, _ ->
                store.delete(profile.id)
                onChanged()
            }
            .setNegativeButton(R.string.lavalynx_profiles_cancel, null)
            .show()
    }
}
