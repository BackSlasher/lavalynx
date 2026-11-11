/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.messaging

import mozilla.components.service.nimbus.messaging.Message
import mozilla.components.service.nimbus.messaging.MessageSurfaceId
import mozilla.components.service.nimbus.messaging.NimbusMessagingControllerInterface

/**
 * lavalynx: a messaging controller that never has a message to show.
 *
 * Every Nimbus messaging surface (the home screen message card, push notification messages, the full-screen survey and
 * microsurveys) asks the controller for its next message, so answering "none" here switches all of them off, whether
 * the message is built in or pushed remotely. Everything else is delegated, so callers keep working.
 *
 * @param delegate The real controller, used for anything that is not a message lookup.
 */
class NoMessagesController(delegate: NimbusMessagingControllerInterface) :
    NimbusMessagingControllerInterface by delegate {
    override suspend fun getMessages(): List<Message> = emptyList()

    override fun getNextMessage(
        surfaceId: MessageSurfaceId,
        messages: List<Message>,
        triggerContext: Map<String, Any?>?,
    ): Message? = null

    override suspend fun getNextMessage(surfaceId: MessageSurfaceId): Message? = null

    override suspend fun getMessage(id: String): Message? = null
}
