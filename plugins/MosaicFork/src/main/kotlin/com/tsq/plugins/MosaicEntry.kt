package com.tsq.plugins

import kotlin.collections.MutableList
import com.discord.models.message.Message
import com.discord.api.message.attachment.MessageAttachment
import com.discord.widgets.chat.list.entries.ChatListEntry

class MosaicEntry(val images: MutableList<MessageAttachment>, var msg: Message) : ChatListEntry() {
    val MOSAIC_VIEW_TYPE = 1234
	val uniqueKey = msg.id.toString()

    override fun getType() = MOSAIC_VIEW_TYPE
    override fun getKey() = this.uniqueKey
}
