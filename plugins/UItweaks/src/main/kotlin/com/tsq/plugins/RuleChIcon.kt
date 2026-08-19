package com.tsq.plugins

import android.content.Context
import android.widget.ImageView
import android.content.res.Resources
import androidx.core.content.res.ResourcesCompat

import com.discord.databinding.WidgetChannelsListItemChannelBinding
import com.discord.stores.StoreStream
import com.discord.widgets.channels.list.WidgetChannelsListAdapter
import com.discord.widgets.channels.list.items.ChannelListItem
import com.discord.widgets.channels.list.items.ChannelListItemTextChannel

import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.patcher.*;
import com.aliucord.settings.*;
import com.aliucord.api.PatcherAPI
import com.aliucord.utils.ReflectUtils
import com.aliucord.wrappers.ChannelWrapper

object RuleChIcon {
	
	fun init(context: Context, patcher: PatcherAPI, logger: Logger, resources: Resources?) {
	
		// part of BetterChannelIcons by wingio
		val onConfigureMethod by lazy { WidgetChannelsListAdapter.ItemChannelText::class.java.getDeclaredMethod("onConfigure", Int::class.javaPrimitiveType, ChannelListItem::class.java)	}
		
		patcher.patch(onConfigureMethod, Hook { param ->
			try {
				val channelItem = param.args[1] as ChannelListItemTextChannel
				val apiChannel = channelItem.getChannel() 
				val channel = ChannelWrapper(apiChannel)
				
				val channelId = channel.id
				val guildId = channel.guildId

				val guild = StoreStream.getGuilds().getGuilds().get(guildId)
				
				if (guild != null && guild.getRulesChannelId() != null) {
					if (guild.getRulesChannelId() == channelId) {
						
						val _this = param.thisObject as WidgetChannelsListAdapter.ItemChannelText
						val binding = ReflectUtils.getField(_this, "binding") as WidgetChannelsListItemChannelBinding
						val channelIcon = binding.getRoot().findViewById(Utils.getResId("channels_item_channel_hash", "id")) as ImageView
						
						val customIconId = resources?.getIdentifier("ic_rules_24dp", "drawable", "com.tsq.plugins")
						
						if (customIconId != 0) {
							val vectorDrawable = ResourcesCompat.getDrawable(resources!!, customIconId!!, null)
							if (vectorDrawable != null) {
							channelIcon.setImageDrawable(vectorDrawable)
							}
						}
					}
				}
			} catch (e: Throwable) {
				logger.error("ruleChIcon", e)
			}
		})
	}
}
