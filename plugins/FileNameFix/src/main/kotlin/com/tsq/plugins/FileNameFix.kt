package com.tsq.plugins

import android.content.Context
import android.util.Base64

import com.aliucord.Logger
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.*

import com.discord.utilities.rest.SendUtils.FileUpload
import com.discord.utilities.io.NetworkUtils
import com.lytefast.flexinput.model.Attachment

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.ArrayList
import java.util.List
import java.util.UUID

import okhttp3.Headers
import okhttp3.MultipartBody
import okhttp3.RequestBody
import rx.Observable


@AliucordPlugin
class FileNameFix: Plugin() {
	private var name2 = ""
	
	override fun start(context: Context) {
		val dnMethod = Attachment::class.java.getDeclaredMethod("getDisplayName")
		val fuConstructor = FileUpload::class.java.getDeclaredConstructor(String::class.java, Long::class.javaPrimitiveType, MultipartBody.Part::class.java, String::class.java, Observable::class.java)
		
		patcher.patch(dnMethod, Hook { param -> 
			val original = param.result as String
			val lastDotIndex = original.lastIndexOf('.')
			
			val nameWithoutExt = if (lastDotIndex == -1) original else original.substring(0, lastDotIndex)
			val ext = if (lastDotIndex == -1)  "" else original.substring(lastDotIndex + 1)
				
			name2 = original
			//String cleanName = UUID.randomUUID().toString().substring(0, 8)
			//param.setResult(cleanName + (ext.isEmpty() ? "" : "." + ext))
		})
		
		patcher.patch(fuConstructor, PreHook { param ->
			try {
				val nameField = param.args[0] as String
				val contentLengthField = param.args[1]
				val partField = param.args[2]
				val mimeTypeField = param.args[3]
				
				if (nameField.startsWith("SPOILER_")) {
					param.args[0] = "SPOILER_" + name2
				} else {
					param.args[0] = name2
				}
				
				logger.info("before: " + nameField + " || after: " + param.args[0])
				
			} catch (e: Exception) {
				logger.error("fileUpload", e)
			}
		})
	}
		
	override fun stop(context: Context) {
		patcher.unpatchAll()
	}
}
