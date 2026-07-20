package com.tsq.plugins

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.NonNull

import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.*

import com.discord.stores.StoreStream
import com.discord.utilities.rest.RestAPI
import com.discord.widgets.friends.WidgetFriendsAddById

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.HashMap
import java.util.List

import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody


@AliucordPlugin(requiresRestart = false)
@SuppressWarnings("unused")
class FriendFix : Plugin() {
    //	val logger: Logger = Logger("FriendFix")

    override fun start(context: Context) {

		try {
			// Request.Builder.build
			val buildMethod = okhttp3.Request.a::class.java.getDeclaredMethod("a")

			patcher.patch(buildMethod, Hook { param ->
				val request = param.result as? okhttp3.Request
				if (request == null) return@Hook

				// use original field b(url) from JADX
				val url = request.b;
				if (url != null && url.toString().contains("relationships")) {
					
					// use original field e(body) from JADX
					val body = request.e
					if (body != null) {
						try {
							// find 'a'
							// This field is ByteString
							val dataField = body::class.java.getDeclaredField("a")
							dataField.isAccessible = true
							val dataObj = dataField.get(body)

							if (dataObj is okio.ByteString) {
								val byteString = dataObj
								
								val content = byteString.q()

								if (content != null && content.contains("12345")) {
									// 12345 -> "null"
									val patched = content.replace("\"12345\"", "null").replace("12345", "null")
									
									// RequestBody.create(MediaType, String)
									val newBody = okhttp3.RequestBody.create(body.contentType(), patched)

									// 7. Request Creator (b, c, d, e, f)
									val newRequest = okhttp3.Request(
										url,           // b
										request.c,     // method
										request.d,     // headers
										newBody,       // e
										request.f      // tags
									)

									param.result = newRequest
									logger.info("12345 -> \"null\"")
								}
							}
						} catch (e: Exception) {
							logger.error("Internal Patch Error", e)
						}
					}
				}
			});
		} catch (e: NoSuchMethodException) {
			logger.error("Target not found", e);
		}


       // UI Patch
		try {
			val resultConstructor = WidgetFriendsAddById.Companion.UserNameDiscriminator::class.java.getDeclaredConstructor(String::class.java, Integer::class.java)
			resultConstructor.isAccessible = true
			
			val extractMethod = WidgetFriendsAddById.Companion::class.java.getDeclaredMethod("extractUsernameAndDiscriminator", CharSequence::class.java)

			patcher.patch(extractMethod, InsteadHook { param -> 
				val input = param.args[0].toString()
				if (!input.contains("#")) {
					try {
						return@InsteadHook resultConstructor.newInstance(input, 12345) // null or "null"... maybe?
					} catch (e: Exception) {
						logger.error("UI Patch failed", e)
					}
				}
				
				try {
					val method = param.method as Method
					method.isAccessible = true
					return@InsteadHook method.invoke(param.thisObject, *param.args)
				} catch (e: Exception) {
					return@InsteadHook null
				}
			})
		} catch (e: Exception) {
			logger.error("UI Setup failed", e)
		}
	}
	
    override fun stop(context: Context) {
		patcher.unpatchAll()
	}
}
