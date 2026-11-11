/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.messaging

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mozilla.components.service.nimbus.messaging.Message
import mozilla.components.service.nimbus.messaging.NimbusMessagingControllerInterface
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// lavalynx: the messaging controller wrapper that switches every Nimbus messaging surface off.
class NoMessagesControllerTest {
    private val message = mockk<Message>(relaxed = true)
    private val delegate =
        mockk<NimbusMessagingControllerInterface>(relaxed = true) {
            coEvery { getMessages() } returns listOf(message)
            coEvery { getNextMessage(any<String>()) } returns message
            every { getNextMessage(any(), any(), any()) } returns message
            coEvery { getMessage(any()) } returns message
        }
    private val controller = NoMessagesController(delegate)

    @Test
    fun `WHEN asking for messages THEN there are none`() = runTest {
        assertTrue(controller.getMessages().isEmpty())
        assertNull(controller.getNextMessage(FenixMessageSurfaceId.HOMESCREEN))
        assertNull(controller.getNextMessage(FenixMessageSurfaceId.NOTIFICATION, listOf(message)))
        assertNull(controller.getNextMessage(FenixMessageSurfaceId.SURVEY))
        assertNull(controller.getNextMessage(FenixMessageSurfaceId.MICROSURVEY))
        assertNull(controller.getMessage("any-id"))
    }

    @Test
    fun `WHEN a message is dismissed THEN the call is delegated`() = runTest {
        controller.onMessageDismissed(message)

        coVerify { delegate.onMessageDismissed(message) }
    }
}
