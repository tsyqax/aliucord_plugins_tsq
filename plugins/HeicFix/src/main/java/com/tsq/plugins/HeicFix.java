package com.tsq.plugins;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.*;
import com.aliucord.settings.*;
import com.discord.utilities.rest.RestAPI;
import com.discord.stores.StoreStream;
import com.discord.stores.SelectedChannelAnalyticsLocation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.Fragment;
import java.util.List;

import androidx.annotation.NonNull; 
import java.nio.charset.StandardCharsets; 
import okhttp3.MediaType; 
import okhttp3.RequestBody;
import okhttp3.Request;
import okhttp3.MultipartBody;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import com.discord.widgets.channels.list.WidgetChannelsListItemChannelActions;
import com.discord.widgets.channels.list.WidgetChannelsListItemThreadActions;
import com.discord.widgets.channels.settings.WidgetTextChannelSettings;
import com.discord.widgets.channels.settings.WidgetThreadSettings;
import com.discord.widgets.share.WidgetIncomingShare;
import com.discord.widgets.user.search.WidgetGlobalSearchModel;
import com.discord.utilities.permissions.PermissionUtils;
import com.aliucord.api.SettingsAPI;

import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import java.util.HashMap;
import com.aliucord.views.Button;
import com.discord.api.channel.Channel;
import com.discord.utilities.rest.RestAPI;
import com.discord.restapi.PayloadJSON;
import com.aliucord.views.TextInput;
import com.aliucord.utils.ReflectUtils;
import com.aliucord.utils.ChannelUtils;
import com.aliucord.utils.MDUtils;

import android.widget.LinearLayout;
import com.discord.api.permission.Permission;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.fragments.FragmentProxy;
import androidx.fragment.app.DialogFragment;
import com.aliucord.widgets.BottomSheet;
import com.aliucord.fragments.ConfirmDialog;
import com.aliucord.api.CommandsAPI;
import androidx.core.widget.NestedScrollView;
import com.discord.api.commands.ApplicationCommandType;
import java.util.Arrays;
import java.util.Collections;
import java.io.File;
import java.io.FileOutputStream;

import com.discord.utilities.images.MGImages;
import android.content.ContentResolver;
import com.discord.media_picker.MediaPicker;
import com.discord.dialogs.ImageUploadDialog;
import com.lytefast.flexinput.model.Attachment;
import android.net.Uri;

@AliucordPlugin(requiresRestart = false)
@SuppressWarnings("unused")
public class HeicFix extends Plugin {
	public static SettingsAPI staticSettings;
	public static final Logger logger = new Logger("HeicFix");
	private boolean isTarget2 = false;
	
    @Override
    public void start(@NonNull Context context) throws Throwable { 
		
		/* patcher.patch(RestAPI.class.getDeclaredMethod("sendMessage", long.class, PayloadJSON.class, MultipartBody.Part[].class), new Hook(cf -> {
			logger.info(">>> sendMessage Hooked! <<<");
			try {
				File cacheDir = context.getCacheDir();
				File[] garbage = cacheDir.listFiles((dir, name) -> name.startsWith("heic_fix_"));
				
				if (garbage == null) {
					logger.info("garbage == null");
					return;
				}
				
				if (!isTarget2) return;

				int count = 0;
				for (File f : garbage) {
					if (f.getName().startsWith("heic_fix_")) {
						if (f.delete()) {
							logger.info("success: " + f.getName());
							count++;
						} else {
							logger.warn("denied: " + f.getName());
						}
					}
				}
				logger.info("All " + count + "'s file tried");
				
			} catch (Exception e) {
				logger.error("Error", e);
			}
			
			isTarget2 = false;
		})); */
		// for alpha at now
		
		
		patcher.patch(Attachment.class.getDeclaredConstructor(long.class, Uri.class, String.class, Object.class, boolean.class), 
			new PreHook(cf -> {
				try {
					
					boolean isTarget = false;	
					Uri uri = (Uri) cf.args[1];
					String fileName = (String) cf.args[2];

					StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
					
					String className =  stackTrace[9].getClassName();
					if (className.contains("compressImageAttachments")) {
						isTarget = true;
						isTarget2 = true;
					}
					
					if (!isTarget) return;
					
					//isTarget = false;
					
					if (uri == null || fileName == null || 
						!(fileName.toLowerCase().endsWith(".heic") || uri.toString().toLowerCase().endsWith(".heic"))) {
						return; 
					}

					var contentResolver = context.getContentResolver();

					try (java.io.InputStream is = contentResolver.openInputStream(uri)) {
						if (is != null) {
							android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is);

							if (bitmap != null) {
								File tempFile = File.createTempFile("heic_fix_", ".jpg", context.getCacheDir());
								try (FileOutputStream out = new FileOutputStream(tempFile)) {
									bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out);
								}
								
								cf.args[1] = Uri.fromFile(tempFile);
								cf.args[2] = fileName.substring(0, fileName.lastIndexOf('.')) + ".jpg";
								
								bitmap.recycle();
								logger.info("HEIC to JPG: " + tempFile.getName());
								
								//tempFile.delete();
							}
						}
					}
				} catch (Exception e) {
					logger.error("HEIC conversion failed: ", e);
				}
			})
		);

    }

    @Override
    public void stop(@NonNull Context context) {
        patcher.unpatchAll();
    }
}
