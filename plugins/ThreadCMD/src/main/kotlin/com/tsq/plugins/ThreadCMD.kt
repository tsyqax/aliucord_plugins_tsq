package com.tsq.plugins

import android.content.Context
import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.CommandsAPI
import com.aliucord.entities.Plugin
import com.discord.api.commands.ApplicationCommandType
import com.discord.utilities.rest.RestAPI
import okhttp3.MultipartBody


@AliucordPlugin
//@Suppress("unused")

class ThreadCMD: Plugin() {
	override fun start(context: Context) {
		commands.registerCommand(
            "thread",
            "Make thread here!",
            listOf(
				Utils.createCommandOption(
					ApplicationCommandType.STRING, 
					"name", 
					"Name", 
					null, 
					true
				)
			),
			{ ctx ->
				try {
					val threadName = ctx.getRequiredString("name")
					val channelId = ctx.channelId
					
					RestAPI.api.createThreadWithMessage(
						channelId,
						threadName,
						"01010",
						emptyList<Long>(),
						emptyList<Long>(),
						11,
						1440,
						arrayOf<MultipartBody.Part>()
					).`V` {}
					
					CommandsAPI.CommandResult("DONE!\n(If not, maybe permission issue!)", null, false)
				} catch (t: Throwable) {
					logger.error("ThreadGen", t)
					CommandsAPI.CommandResult("Error: '${t.message}'", null, false)
				}
			}
		)
	}

    override fun stop(context: Context) { patcher.unpatchAll() }
}
