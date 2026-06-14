package com.tsq.plugins;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;

import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.Http;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;

import d0.t.n;
import kotlin.jvm.functions.Function2;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import rx.Observable;
import com.lytefast.flexinput.R;

import org.json.JSONArray;
import org.json.JSONObject;

@AliucordPlugin
public class ServerNicknameFix extends Plugin {
	private final Logger logger = new Logger("ServerNicknameFix");
	private String userInput;
	private String extractedTicket;
	private boolean reInvoked;
	private boolean isQRsent;

	@Override
	public void start(Context context) throws NoSuchMethodException {
		try {
			Method buildMethod = okhttp3.Request.a.class.getDeclaredMethod("a");

			patcher.patch(buildMethod, new Hook(cf -> {
				okhttp3.Request request = (okhttp3.Request) cf.getResult();
				if (request == null) return;
				f0.w url = request.b;
				if (url != null && url.toString().contains("members/@me")) {
					RequestBody body = request.e;
					if (body != null) {
						try {
							// find 'a'
							// This field is ByteString
							Field dataField = body.getClass().getDeclaredField("a");
							dataField.setAccessible(true);
							Object dataObj = dataField.get(body);

							if (dataObj instanceof okio.ByteString) {
								okio.ByteString byteString = (okio.ByteString) dataObj;
							
								String content = byteString.q(); 
									
								if (content != null) {
									String patched = content.replace("\"bio\":\"\"", "").replace("{,", "{").replace(",}", "}");
									
									// RequestBody.create(MediaType, String)
									RequestBody newBody = RequestBody.create(body.contentType(), patched);

									// 7. Request Creator (b, c, d, e, f)
									okhttp3.Request newRequest = new okhttp3.Request(
										url,           // b
										request.c,     // method
										request.d,     // headers
										newBody,       // e
										request.f      // tags
									);

									cf.setResult(newRequest);
									logger.info("Static Patch Success: I like FriendFix!");
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
	}
	
	@Override
	public void stop(Context context) { patcher.unpatchAll(); }
}
