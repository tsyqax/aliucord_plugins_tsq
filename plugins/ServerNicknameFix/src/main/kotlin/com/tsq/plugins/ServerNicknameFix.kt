package com.tsq.plugins

import android.content.Context

import com.aliucord.Logger
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.*

import java.lang.reflect.Field
import java.lang.reflect.Method

import com.discord.stores.StoreStream

import okhttp3.RequestBody
import okhttp3.Request

@AliucordPlugin
class ServerNicknameFix: Plugin() {
	override fun start(context: Context) {
		try {
			val buildMethod by lazy { Request.a::class.java.getDeclaredMethod("a") }

			patcher.patch(buildMethod, Hook { param ->
				val isCanChange = StoreStream.getUsers().getMe().getPremiumTier().ordinal == 4
				if (isCanChange) return@Hook
				
				val request = param.result as? Request
				if (request == null) return@Hook
				
				val url = request.b
				
				if (url != null && url.toString().contains("members/@me")) {
					val body = request.e
					if (body != null) {
						try {
							val dataField by lazy { body.javaClass.getDeclaredField("a") }
							dataField.isAccessible = true
							val dataObj = dataField.get(body)
							
							if (dataObj is okio.ByteString) {
								val content = dataObj.q()
								
								if (content != null) {
									val patched = try {
										val jsonObject = org.json.JSONObject(content)
										if (jsonObject.has("bio") && jsonObject.getString("bio") == "") {
											jsonObject.remove("bio")
										}
										jsonObject.toString()
									} catch (e: Exception) {
										content
									}

									val newBody = RequestBody.create(body.contentType(), patched)
									val newRequest = Request(
										url,
										request.c,
										request.d,
										newBody,
										request.f
									)
									
									param.result = newRequest
									logger.info("Static Patch Success: I like FriendFix!")
								}
							}
						} catch (e: Exception) {
							logger.error("Patch", e)
						}
					}
				}
			})
		} catch (e: Exception) {
			logger.error("Not Found", e)
		}
	}
	
	override fun stop(context: Context) { patcher.unpatchAll() }
}
