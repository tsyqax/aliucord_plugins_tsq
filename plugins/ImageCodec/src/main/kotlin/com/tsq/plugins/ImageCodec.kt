package com.tsq.plugins

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder

import android.os.Build
import android.os.FileUtils
import android.net.Uri

import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.patcher.*
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin

import com.discord.utilities.rest.AttachmentRequestBody
import com.discord.utilities.rest.SendUtils

import java.io.File
import java.io.FileOutputStream
import java.io.BufferedOutputStream
import java.io.OutputStream

import com.lytefast.flexinput.model.Attachment


@AliucordPlugin(requiresRestart = false)
class ImageCodec: Plugin() {
	private val attachmentField by lazy { AttachmentRequestBody::class.java.getDeclaredField("attachment").apply { isAccessible = true }}

	override fun start(context: Context) {
		try {
			val cacheDir = context.cacheDir
			val garbage = cacheDir.listFiles { _, name -> name.startsWith("imgcdc_") }
			var count = 0

			garbage?.forEach { f ->
				if (f.delete()) { 
					count++ 
				}
			}
			
			logger.info("Removed $count cache images")
			
		} catch (e: Exception) {
			logger.error("Error", e)
		}
		
		val getPart by lazy { SendUtils::class.java.getDeclaredMethod("getPart", Attachment::class.java, ContentResolver::class.java, String::class.java) }
		patcher.patch(getPart, PreHook { param ->
			val attachment = param.args[0] as? Attachment<*>
			if (attachment == null) return@PreHook
			
			val checking = attachment.getDisplayName().lowercase()
			
			var newDsName: String? = null
			var tempFile: File? = null

			if (checking.endsWith(".heic") || checking.endsWith(".heif") || checking.endsWith(".hif") || checking.endsWith(".bmp") || checking.endsWith(".avif") || checking.endsWith(".dib")) {
				newDsName = getNewName(attachment, "jpg")
				tempFile = convertToJpg(context, attachment) 
			}
			
			if (checking.endsWith(".jfif") || checking.endsWith(".jfi") || checking.endsWith(".jpe") || checking.endsWith(".pjpeg") || checking.endsWith(".pjpg")) {
				newDsName = getNewName(attachment, "jpg")
				tempFile = renameToNew(context, attachment, "jpg") 
			}
			
			if (checking.endsWith(".apng")) {
				newDsName = getNewName(attachment, "png")
				tempFile = renameToNew(context, attachment, "png") 
			}
			
			if (tempFile == null) return@PreHook
			
			val newAttachment = Attachment(attachment.id, Uri.fromFile(tempFile), newDsName, attachment.data, attachment.spoiler)
			tempFile.deleteOnExit()

			param.args[0] = newAttachment
            param.args[2] = newDsName
		})
    }
	
	private fun getAttachment(body: AttachmentRequestBody): Attachment<*> {
        return attachmentField.get(body) as Attachment<*>
    }
	
	private fun getNewName(attachment: Attachment<*>?, newExt: String): String {
		if (attachment == null || attachment.displayName == null) {
			return "image." + newExt
		}
		
		val displayName = attachment.displayName
		var baseName = displayName.replace("(?i)\\.[^.]+$".toRegex(), "")
		
		if (baseName.isEmpty()) {
			baseName = "image"
		}
		
		return baseName + "." + newExt
	}
	
	private fun compressAttachmentToStream(context: Context, attachment: Attachment<*>?, targetStream: OutputStream?): Boolean {
		if (attachment == null || attachment.uri == null || targetStream == null) return false

		try {
			val contentResolver = context.contentResolver

			return contentResolver.openInputStream(attachment.uri)?.use { inputStream ->
				var bitmap: Bitmap? = null
				
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
					bitmap = BitmapFactory.decodeStream(inputStream)
				} else {
					// Referenced mantikafasi's HeicImageConvertor
					val source = ImageDecoder.createSource(context.contentResolver, attachment.uri)
					bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ -> 
						decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE) 
					}
				}
				if (bitmap == null) return false

				try {
					return bitmap.compress(Bitmap.CompressFormat.JPEG, 90, targetStream)
				} finally {
					bitmap.recycle()
				}
			} ?: false
			
		} catch (e: Exception) {
			logger.error("cpssToStream", e)
			return false
		}
	}
	
	private fun convertToJpg(context: Context, attachment: Attachment<*>): File? { // 💡 fun 키워드 및 리턴 타입 뒤 ? 추가
		try {
			val tempFile = File.createTempFile("imgcdc_", ".jpg", context.cacheDir)

			FileOutputStream(tempFile).use { fos ->
				BufferedOutputStream(fos).use { out ->
					if (compressAttachmentToStream(context, attachment, out)) {
						return tempFile
					}
				}
			}
			
			if (tempFile.exists()) tempFile.delete() // If failed
			return null
			
		} catch (e: Exception) {
			logger.error("cvt2Jpg", e)
			return null
		}
	}

	private fun renameToNew(context: Context, attachment: Attachment<*>, newExt: String): File? {
		try {
			val tempFile = File.createTempFile("imgcdc_", ".$newExt", context.cacheDir)

			context.contentResolver.openInputStream(attachment.getUri()).use { isStream ->
				if (isStream == null) {
					if (tempFile.exists()) tempFile.delete()
					return null
				}
				
				FileOutputStream(tempFile).use { fos ->
					BufferedOutputStream(fos).use { out ->
						FileUtils.copy(isStream, out)
						out.flush()
						return tempFile
					}
				}
			}
		} catch (e: Exception) {
			logger.error("rn2New", e)
			return null
		}
	}

	
    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
