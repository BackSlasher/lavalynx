/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.profiles

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.MenuItem
import mozilla.components.compose.menu.data.MenuItemSummary
import mozilla.components.compose.menu.data.MenuItemTitle
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.store.MenuEvent
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.ui.icons.R as iconsR
import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.MenuHost
import org.mozilla.fenix.components.menu.MenuItemProvider
import org.mozilla.fenix.components.menu.MenuTarget

/**
 * lavalynx: [MenuItemProvider] for the menu item opening the profile manager.
 *
 * @param context Themed context to show the profile manager in.
 * @param target [MenuTarget] for which this menu item would be shown for.
 */
class ProfilesMenuItemProvider(private val context: Context, private val target: MenuTarget) : MenuItemProvider {
    private object OpenProfiles : MenuEvent

    override val itemsFlow: StateFlow<List<MenuItem>> =
        MutableStateFlow(
            listOf(
                StandardMenuItem(
                    title = MenuItemTitle(Text.Resource(R.string.lavalynx_profiles)),
                    summary =
                        MenuItemSummary(
                            Text.String(
                                context.getString(
                                    R.string.lavalynx_profiles_summary,
                                    ProfileSwitcher.store(context).active().name,
                                )
                            )
                        ),
                    icon = MenuItemIconRes(iconsR.drawable.mozac_ic_avatar_circle_24),
                    onClickEvent = OpenProfiles.takeIf { target != MenuTarget.CustomizeMenu },
                )
            )
        )

    override fun handles(event: MenuEvent) = target != MenuTarget.Preview && event == OpenProfiles

    override fun onEvent(event: MenuEvent, menu: MenuHost) {
        menu.dismiss()
        ProfilesDialog(context) {}.show()
    }
}
