package com.tsq.plugins;

import android.content.Context;

import com.aliucord.Logger;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.discord.stores.StoreStream;

import okhttp3.RequestBody;

@AliucordPlugin
public class ServerNicknameFix extends Plugin {
	private final Logger logger = new Logger("ServerNicknameFix");

	@Override
	public void start(Context context) throws NoSuchMethodException {
		try {
			
			Method buildMethod = okhttp3.Request.a.class.getDeclaredMethod("a");
			
			patcher.patch(buildMethod, new Hook(cf -> {
				boolean isCanChange = StoreStream.getUsers().getMe().getPremiumTier().ordinal() == 4;
				if (isCanChange) {
					return;
				}				
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
