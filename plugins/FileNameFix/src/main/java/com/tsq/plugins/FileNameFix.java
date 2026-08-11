package com.tsq.plugins;

import android.content.Context;
import android.util.Base64;

import com.aliucord.Logger;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.*;


import com.discord.utilities.rest.SendUtils.FileUpload;
import com.discord.utilities.io.NetworkUtils;
import com.lytefast.flexinput.model.Attachment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import okhttp3.Headers;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import rx.Observable;


@AliucordPlugin
public class FileNameFix extends Plugin {
    private final Logger logger = new Logger("FileNameFix");
	private String name;

	@Override
	public void start(Context context) throws Throwable {
		
		Method dnMethod = Attachment.class.getDeclaredMethod("getDisplayName");
		Constructor<?> fuConstructor = FileUpload.class.getDeclaredConstructor(String.class, long.class, MultipartBody.Part.class, String.class, Observable.class);
		
		patcher.patch(dnMethod, new Hook(param -> {
				String original = (String) param.getResult();
				
				int lastDotIndex = original.lastIndexOf('.');
				String nameWithoutExt = (lastDotIndex == -1) ? original : original.substring(0, lastDotIndex);
				String ext = (lastDotIndex == -1) ? "" : original.substring(lastDotIndex + 1);
				
				name = original;
				//String cleanName = UUID.randomUUID().toString().substring(0, 8);
				//param.setResult(cleanName + (ext.isEmpty() ? "" : "." + ext));
		}));
		
		patcher.patch(fuConstructor, new PreHook(param -> {
			try {
				String nameField = (String) param.args[0];
				long contentLengthField = (long) param.args[1];
				MultipartBody.Part partField = (MultipartBody.Part) param.args[2];
				String mimeTypeField = (String) param.args[3];
				
				if (nameField.startsWith("SPOILER_")) {
					param.args[0] = "SPOILER_" + name;
				} else {
					param.args[0] = name;
				}
				
				logger.info("before: " + nameField + " || after: " + param.args[0]);
				
			} catch (Exception e) {
				logger.error(">>> FileUpload Hook Runtime Error", e);
			}
		}));
	}
	
    @Override
    public void stop(Context context) { patcher.unpatchAll(); }
}
