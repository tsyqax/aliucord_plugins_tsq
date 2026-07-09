package com.tsq.plugins;

import android.content.Context;
import android.view.View;
import androidx.annotation.NonNull;

import com.aliucord.Http;
import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.*;
import com.aliucord.annotations.AliucordPlugin;

import com.discord.widgets.user.profile.UserProfileHeaderView;
import com.discord.widgets.user.profile.UserProfileHeaderViewModel;
import org.json.JSONObject;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@AliucordPlugin
public class AvatarDeco extends Plugin {
    public static final Logger logger = new Logger("AvatarDeco");
    public static final Map<String, String> decoMap = new HashMap<>();
    public static final Map<String, String> UrlMap = new HashMap<>();

    @Override
    public void start(@NonNull Context context) throws Throwable {
		Method targetMethod = UserProfileHeaderView.class.getDeclaredMethod("updateViewState", UserProfileHeaderViewModel.ViewState.Loaded.class);
		patcher.patch(targetMethod, new Hook(cf -> {
			UserProfileHeaderViewModel.ViewState.Loaded state = (UserProfileHeaderViewModel.ViewState.Loaded) cf.args[0];
			if (state == null || state.getUser() == null) return;
			
			View headerView = (View) cf.thisObject;
			if (headerView == null || !headerView.isAttachedToWindow()) return;

			try {
				long rawUserId = state.getUser().getId();
				final String userId = String.valueOf(rawUserId);

				if (decoMap.containsKey(userId)) {
					logger.info("Cached: " + userId + " -> " + UrlMap.get(userId));
					String cachedUrl = UrlMap.get(userId);
					if (cachedUrl != null && !cachedUrl.isEmpty()) {
						applyDecorationView(headerView, cachedUrl);
					}
					return;
				}

				new Thread(() -> {
					try {
						String targetUrl = String.format("/users/%s/profile", userId);
						Http.Response response = Http.Request.newDiscordRequest(targetUrl, "GET").execute();
						String content = response.text(); 
							
						String sku_id = "";
						if (content != null && !content.isEmpty()) {
							JSONObject json = new JSONObject(content);
							
							if (json.has("user")) {
								JSONObject userObj = json.getJSONObject("user");

								if (userObj.has("user_profile") && !userObj.isNull("user_profile")) {
									JSONObject userProfileObj = userObj.getJSONObject("user_profile");

									if (userProfileObj.has("profile_effect") && !userProfileObj.isNull("profile_effect")) {
										JSONObject profileEffectObj = userProfileObj.getJSONObject("profile_effect");

										if (profileEffectObj.has("sku_id")) {
											sku_id = profileEffectObj.getString("sku_id");
											logger.info("sku_id: " + sku_id);
										}
									}
								}
							}
						}

						if (sku_id == null || sku_id.isEmpty()) return;

						String storeUrl = String.format("/collectibles-products/%s", sku_id);
						Http.Response storeResponse = Http.Request.newDiscordRequest(storeUrl, "GET").execute();
						String storeContent = storeResponse.text(); 
							
						if (storeContent != null && !storeContent.isEmpty()) {
							JSONObject json = new JSONObject(storeContent);
							if (json.has("items")) {
								org.json.JSONArray itemsArray = json.getJSONArray("items");
								if (itemsArray.length() > 0) {
									JSONObject itemObj = itemsArray.getJSONObject(0);
									if (itemObj.has("staticFrameSrc") && !itemObj.isNull("staticFrameSrc")) {
										String decoUrl = itemObj.getString("staticFrameSrc");
										decoMap.put(userId, decoUrl);

										applyDecorationView(headerView, decoUrl);
									}
								}
							}
						}
					} catch (Exception e) {
						logger.error("ERR", e);
					}
				}).start();

			} catch (Exception e) {
				logger.error("ERRR", e);
			}
		}));
	}

	private void applyDecorationView(View headerView, String decoUrl) {
		android.app.Activity activity = (android.app.Activity) headerView.getContext();
		if (activity == null) return;

		activity.runOnUiThread(() -> {
			try {
				java.lang.reflect.Field bindingField = UserProfileHeaderView.class.getDeclaredField("binding");
				bindingField.setAccessible(true);
				Object bindingObj = bindingField.get(headerView);
				
				java.lang.reflect.Field avatarField = bindingObj.getClass().getDeclaredField("f");
				avatarField.setAccessible(true);
				android.view.View avatarView = (android.view.View) avatarField.get(bindingObj);

				android.view.ViewGroup avatarParent = (android.view.ViewGroup) avatarView.getParent();

				int decoViewTagKey = 0x7FFFFFFF;
				if (avatarParent.findViewById(decoViewTagKey) == null) {
					com.facebook.drawee.view.SimpleDraweeView decoImageView = new com.facebook.drawee.view.SimpleDraweeView(headerView.getContext());
					decoImageView.setId(decoViewTagKey);

					android.view.ViewGroup.LayoutParams avatarParams = avatarView.getLayoutParams();
					android.view.ViewGroup.LayoutParams decoParams = new android.view.ViewGroup.LayoutParams(avatarParams.width, avatarParams.height);
					decoImageView.setLayoutParams(decoParams);

					decoImageView.setImageURI(android.net.Uri.parse(decoUrl));
					avatarParent.addView(decoImageView);
					
					logger.info("POWER!!!!!!");
				} else {
					com.facebook.drawee.view.SimpleDraweeView existingDecoView = avatarParent.findViewById(decoViewTagKey);
					existingDecoView.setImageURI(android.net.Uri.parse(decoUrl));
				}
			} catch (Exception e) {
				logger.error("ERRRRR", e);
			}
		});
	}



    @Override
    public void stop(@NonNull Context context) {
        patcher.unpatchAll();
        decoMap.clear();
    }
}
