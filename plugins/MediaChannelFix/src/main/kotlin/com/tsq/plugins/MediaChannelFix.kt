package com.tsq.plugins;

import android.content.Context
import android.widget.ImageView
import android.content.res.Resources
import androidx.core.content.res.ResourcesCompat

import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.*
import com.aliucord.Utils
import com.aliucord.utils.ReflectUtils

import com.discord.api.channel.Channel
import com.discord.databinding.WidgetChannelsListItemChannelBinding
import com.discord.widgets.channels.list.WidgetChannelsListAdapter
import com.discord.widgets.channels.list.items.ChannelListItem
import com.discord.widgets.channels.list.items.ChannelListItemTextChannel

@AliucordPlugin
class MediaChannelFix: Plugin() {
	private val isMine = ThreadLocal.withInitial { false }
	
	override fun start(context: Context) {
		val getTypeMethod by lazy { Channel::class.java.getDeclaredMethod("D") }
		patcher.patch(getTypeMethod, Hook { param ->
			val rawType = param.result as Int
			if (rawType == 16) {
				if (isMine.get() == true) { 
					param.result = 16 
				} else {
					param.result = 15 
				}
			}
		})
		
		val onConfigureMethod by lazy { WidgetChannelsListAdapter.ItemChannelText::class.java.getDeclaredMethod("onConfigure", Int::class.javaPrimitiveType, ChannelListItem::class.java) }
		patcher.patch(onConfigureMethod, Hook { param ->
			try {
				val channelItem = param.args[1] as ChannelListItemTextChannel
				isMine.set(true)
				val chType = channelItem.getChannel().D() // getType()
				isMine.set(false)

				if (chType == 16) {
					val _this = param.thisObject as WidgetChannelsListAdapter.ItemChannelText
					val binding = ReflectUtils.getField(_this, "binding") as WidgetChannelsListItemChannelBinding
					val channelIcon = binding.getRoot().findViewById(Utils.getResId("channels_item_channel_hash", "id")) as ImageView
						
					val customIconId = resources?.getIdentifier("media_channel", "drawable", "com.tsq.plugins")
						
					if (customIconId != 0) {
						val vectorDrawable = ResourcesCompat.getDrawable(resources!!, customIconId!!, null)
						if (vectorDrawable != null) {
							channelIcon.setImageDrawable(vectorDrawable)
						}
					}
				}
			} catch (e: Throwable) {
				logger.error("mediaChIcon", e)
			}
		})
	}

	override fun stop(context: Context) { patcher.unpatchAll() }
}
