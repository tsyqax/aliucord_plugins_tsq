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
import com.discord.utilities.rest.RestAPI;
import com.discord.stores.StoreStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import androidx.annotation.NonNull; 
import android.content.Context; 
import java.nio.charset.StandardCharsets; 
import okhttp3.MediaType; 
import okhttp3.RequestBody;
import okhttp3.Request;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

@AliucordPlugin(requiresRestart = false)
@SuppressWarnings("unused")
public class FriendFix extends Plugin {
    public static final Logger logger = new Logger("FriendFix");

    @Override
    public void start(@NonNull Context context) throws Throwable {
		
		/*  For Debug
		try {
			// use 'public final String d()' 
			java.lang.reflect.Method stringMethod = okhttp3.ResponseBody.class.getDeclaredMethod("d");

			patcher.patch(stringMethod, new com.aliucord.patcher.Hook(cf -> {
				String content = (String) cf.getResult();

				
				if (content != null && content.contains("\"code\"")) {
					logger.info("─── [FOUND SERVER RESPONSE] ───");
					logger.info(content);
					logger.info("───────────────────────────────");
				}
			}));
		} catch (NoSuchMethodException e) {
			logger.error("Failed to patch ResponseBody.d()", e);
		} catch (Throwable e) {
			logger.error(e);
		} */
		
		try {
			// Request.Builder.build
			java.lang.reflect.Method buildMethod = okhttp3.Request.a.class.getDeclaredMethod("a");

			patcher.patch(buildMethod, new com.aliucord.patcher.Hook(cf -> {
				okhttp3.Request request = (okhttp3.Request) cf.getResult();
				if (request == null) return;

				// use original field b(url) from JADX
				f0.w url = request.b;
				if (url != null && url.toString().contains("relationships")) {
					
					// use original field e(body) from JADX
					okhttp3.RequestBody body = request.e;
					if (body != null) {
						try {
							// find 'a'
							// This field is ByteString
							java.lang.reflect.Field dataField = body.getClass().getDeclaredField("a");
							dataField.setAccessible(true);
							Object dataObj = dataField.get(body);

							if (dataObj instanceof okio.ByteString) {
								okio.ByteString byteString = (okio.ByteString) dataObj;
								
								String content = byteString.q(); 

								if (content != null && content.contains("12345")) {
									// 12345 -> "null"
									String patched = content.replace("\"12345\"", "null").replace("12345", "null");
									
									// RequestBody.create(MediaType, String)
									okhttp3.RequestBody newBody = okhttp3.RequestBody.create(body.contentType(), patched);

									// 7. Request Creator (b, c, d, e, f)
									okhttp3.Request newRequest = new okhttp3.Request(
										url,           // b
										request.c,     // method
										request.d,     // headers
										newBody,       // e
										request.f      // tags
									);

									cf.setResult(newRequest);
									logger.info("Static Patch Success: Discriminator removed using ByteString.");
								}
							}
						} catch (Exception e) {
							logger.error("Internal Patch Error", e);
						}
					}
				}
			}));
		} catch (NoSuchMethodException e) {
			logger.error("Target not found", e);
		}


		Class<?> companionClass = null;
        // UI Patch
        try {
            companionClass = Class.forName("com.discord.widgets.friends.WidgetFriendsAddById$Companion");
            Class<?> resultClass = Class.forName("com.discord.widgets.friends.WidgetFriendsAddById$Companion$UserNameDiscriminator");
            Constructor<?> resultConstructor = resultClass.getDeclaredConstructor(String.class, Integer.class);
            resultConstructor.setAccessible(true);

            patcher.patch(companionClass.getDeclaredMethod("extractUsernameAndDiscriminator", CharSequence.class), 
                new InsteadHook(param -> {
                    String input = param.args[0].toString();
                    if (!input.contains("#")) {
                        try {
                            return resultConstructor.newInstance(input, 12345); //null or "null"... maybe?
                        } catch (Exception e) { logger.error("UI Patch failed", e); }
                    }
					
                    try {
                        Method method = (Method) param.method;
                        method.setAccessible(true);
                        return method.invoke(param.thisObject, param.args);
                    } catch (Exception e) { return null; }
                })
            );
        } catch (Exception e) { logger.error("UI Setup failed", e); }
	}
	
    @Override
    public void stop(@NonNull Context context) {
        patcher.unpatchAll();
    }
}
