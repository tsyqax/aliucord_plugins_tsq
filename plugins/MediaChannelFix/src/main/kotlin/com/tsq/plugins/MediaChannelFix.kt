package com.tsq.plugins

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
import com.discord.api.channel.Channel

@AliucordPlugin
class MediaChannelFix: Plugin() {
	override fun start(context: Context) {
		val getTypeMethod = Channel::class.java.getDeclaredMethod("D")
		patcher.patch(getTypeMethod, Hook { param ->
			if (param.result as? Int == 16) {
				param.result = 15
			}
		})
	}

	override fun stop(context: Context) { patcher.unpatchAll() }
}
