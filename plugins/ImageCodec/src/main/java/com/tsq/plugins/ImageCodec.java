package com.tsq.plugins;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;

import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.net.Uri;
import android.view.View;

import androidx.annotation.NonNull;

import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.patcher.*;

import com.aliucord.api.SettingsAPI;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;

import com.discord.utilities.rest.AttachmentRequestBody;
import com.discord.utilities.rest.SendUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okio.BufferedSink;
import com.lytefast.flexinput.model.Attachment;
import rx.Observable;
import rx.functions.Action1;


@AliucordPlugin(requiresRestart = false)
@SuppressWarnings("unused")
public class ImageCodec extends Plugin {
	public static SettingsAPI staticSettings;
	public static final Logger logger = new Logger("ImageCodec");
	private Field attachmentField;

    @Override
    public void start(@NonNull Context context) throws Throwable {
		attachmentField = AttachmentRequestBody.class.getDeclaredField("attachment");
        attachmentField.setAccessible(true);
		
		try {
			File cacheDir = context.getCacheDir();
			File[] garbage = cacheDir.listFiles((dir, name) -> name.startsWith("imgcdc_"));
			int count = 0;
			
			for (File f : garbage) {
				if (f.delete()) { count++; }
			}
			logger.info("Removed " + count + " cache images");
			
		} catch (Exception e) {
			logger.error("Error", e);
		}
			
		Method getPart = SendUtils.class.getDeclaredMethod("getPart", Attachment.class, ContentResolver.class, String.class);
		patcher.patch(getPart, new PreHook(param -> {
			Attachment<?> attachment = (Attachment<?>) param.args[0];
			if (attachment == null) return;
			
			String checking = attachment.getDisplayName().toLowerCase();
			
			String newDsName = null; 
			File tempFile = null;

			if (checking.endsWith(".heic") || checking.endsWith(".heif") || checking.endsWith(".hif") || checking.endsWith(".bmp") || checking.endsWith(".avif") || checking.endsWith(".dib")) {
				newDsName = getNewName(attachment, "jpg");
				tempFile = convertToJpg(context, attachment); 
			}
			
			if (checking.endsWith(".jfif") || checking.endsWith(".jfi") || checking.endsWith(".jpe") || checking.endsWith(".pjpeg") || checking.endsWith(".pjpg")) {
				newDsName = getNewName(attachment, "jpg");
				tempFile = renameToNew(context, attachment, "jpg"); 
			}
			
			if (checking.endsWith(".apng")) {
				newDsName = getNewName(attachment, "png");
				tempFile = renameToNew(context, attachment, "png"); 
			}
			
			if (tempFile == null) return;
			
			Attachment<?> newAttachment = new Attachment<>(attachment.getId(), Uri.fromFile(tempFile), newDsName, attachment.getData(), attachment.getSpoiler());
			tempFile.deleteOnExit();

			param.args[0] = newAttachment;
            param.args[2] = newDsName;
		}));
    }
	
	private Attachment<?> getAttachment(AttachmentRequestBody body) throws IllegalAccessException {
        return (Attachment<?>) attachmentField.get(body);
    }
	
	private String getNewName(Attachment<?> attachment, String newExt) {
		if (attachment == null || attachment.getDisplayName() == null) {
			return "image." + newExt;
		}
		
		String displayName = attachment.getDisplayName();
		String baseName = displayName.replaceAll("(?i)\\.[^.]+$", "");
		
		if (baseName.isEmpty()) {
			baseName = "image";
		}
		
		return baseName + "." + newExt;
	}
	
	private boolean compressAttachmentToStream(Context context, Attachment<?> attachment, OutputStream targetStream) {
		if (attachment == null || attachment.getUri() == null || targetStream == null) return false;

		try {
			var contentResolver = context.getContentResolver();
			try (InputStream is = contentResolver.openInputStream(attachment.getUri())) {
				if (is == null) return false;
				
				Bitmap bitmap = null;
				
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
					bitmap = BitmapFactory.decodeStream(is);
				} else {
					// Referenced mantikafasi's HeicImageConvertor
					ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), attachment.getUri());
					bitmap = ImageDecoder.decodeBitmap(source, (decoder, info, src) -> decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE));
				}
				if (bitmap == null) return false;

				try {
					return bitmap.compress(Bitmap.CompressFormat.JPEG, 90, targetStream);
				} finally {
					bitmap.recycle();
				}
			}
		} catch (Exception e) {
			logger.error("ERR03", e);
			return false;
		}
	}
	
	private File convertToJpg(Context context, Attachment<?> attachment) {
		try {
			File tempFile = File.createTempFile("imgcdc_", ".jpg", context.getCacheDir());
			
			try (var out = new BufferedOutputStream(new FileOutputStream(tempFile))) {
				if (compressAttachmentToStream(context, attachment, out)) {
					return tempFile;
				}
			}
			
			if (tempFile.exists()) tempFile.delete(); // If failed
			return null;
			
		} catch (Exception e) {
			logger.error("ERR04", e);
			return null;
		}
	}
	
	private File renameToNew(Context context, Attachment<?> attachment, String newExt) {
		try {
			File tempFile = File.createTempFile("imgcdc_", "." + newExt, context.getCacheDir());

			try (InputStream is = context.getContentResolver().openInputStream(attachment.getUri());
				 var out = new BufferedOutputStream(new FileOutputStream(tempFile))) {
				
				if (is == null) {
					if (tempFile.exists()) tempFile.delete();
					return null;
				}
				
				FileUtils.copy(is, out);
				out.flush();
				return tempFile;
			}
		} catch (Exception e) {
			logger.error("ERR05", e);
			return null;
		}
	}
	
    @Override
    public void stop(@NonNull Context context) {
        patcher.unpatchAll();
    }
}
