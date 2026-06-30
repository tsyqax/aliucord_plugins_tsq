package com.tsq.plugins;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;

import android.os.Build;
import android.os.Bundle;
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
public class HeicFix extends Plugin {
	public static SettingsAPI staticSettings;
	public static final Logger logger = new Logger("HeicFix");
	private Field attachmentField;

    @Override
    public void start(@NonNull Context context) throws Throwable {
		attachmentField = AttachmentRequestBody.class.getDeclaredField("attachment");
        attachmentField.setAccessible(true);
		
		try {
			File cacheDir = context.getCacheDir();
			File[] garbage = cacheDir.listFiles((dir, name) -> name.startsWith("heicFix_"));
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
			if (attachment == null || !(attachment.getDisplayName().toLowerCase().endsWith(".heic") || attachment.getDisplayName().toLowerCase().endsWith(".heif"))) return;
				
			String newDisplayName = getJpgName(attachment); 
				
			File tempJpg = convertHeicToJpgFile(context, attachment); 
			if (tempJpg != null) {
				param.args[0] = new Attachment<>(attachment.getId(), Uri.fromFile(tempJpg), newDisplayName, attachment.getData(), attachment.getSpoiler());
				tempJpg.deleteOnExit();
			}

            param.args[2] = newDisplayName;
		}));
    }
	
	private Attachment<?> getAttachment(AttachmentRequestBody body) throws IllegalAccessException {
        return (Attachment<?>) attachmentField.get(body);
    }
	
	private String getJpgName(Attachment<?> attachment) {
		if (attachment == null || attachment.getDisplayName() == null) {
			return "image.jpg";
		}
		
		String displayName = attachment.getDisplayName();
		String baseName = displayName.replaceAll("(?i)\\.(heic|heif)$", "");
		
		if (baseName.isEmpty()) {
			baseName = "image";
		}
		
		return baseName + ".jpg";
	}
	
	private boolean compressAttachmentToStream(Context context, Attachment<?> attachment, OutputStream targetStream) {
		if (attachment == null || attachment.getUri() == null || targetStream == null) return false;

		try {
			var contentResolver = context.getContentResolver();
			try (InputStream is = contentResolver.openInputStream(attachment.getUri())) {
				if (is == null) return false;
				
				Bitmap bitmap = BitmapFactory.decodeStream(is);
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
	
	private File convertHeicToJpgFile(Context context, Attachment<?> attachment) {
		try {
			File tempFile = File.createTempFile("heicFix_", ".jpg", context.getCacheDir());
			
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
	
    @Override
    public void stop(@NonNull Context context) {
        patcher.unpatchAll();
    }
}
