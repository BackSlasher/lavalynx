/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

/** A single source for setting feature flags that are mostly based on build type. */
object FeatureFlags {

    /**
     * Enables custom extension collection feature, This feature does not only depend on this flag. It requires the AMO
     * collection override to be enabled which is behind the Secret Settings.
     */
    val customExtensionCollectionFeature = Config.channel.isNightlyOrDebug || Config.channel.isBeta

    /** Pull-to-refresh allows you to pull the web content down far enough to have the page to reload. */
    const val PULL_TO_REFRESH_ENABLED = true

    /** Allows users to enable Firefox Suggest. */
    const val FX_SUGGEST = true

    /**
     * lavalynx: whether any Firefox Sync / Mozilla account UI is offered: the Synced Tabs page in the tab manager, the
     * account entry in the main menus, the sign-in rows in Settings, the sync preferences under Passwords and Autofill,
     * the sign-in onboarding and setup-checklist steps, "Send to device" and the bookmarks sign-in prompt.
     *
     * The account machinery itself stays in place so signed-in state, if any, is left untouched; only the entry points
     * are hidden.
     */
    const val SYNC_UI_ENABLED = false

    /**
     * Disables the Onboarding feature for debug builds by default. Set this to `true` if you need to access the
     * Onboarding feature for development purposes.
     *
     * ⚠️ DO NOT MODIFY THIS FLAG IN PRODUCTION.
     */
    val onboardingFeatureEnabled = !Config.channel.isDebug

    /** Enables haptic feedback during onboarding. */
    val onboardingHapticsEnabled = Config.channel.isNightlyOrDebug

    /** Shows the animated kit illustration instead of the static one on the onboarding terms of use page. */
    val onboardingKitAnimationEnabled = Config.channel.isNightlyOrDebug
}
