package com.tsq.plugins;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;

import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.Http;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.*;
import com.aliucord.utils.DimenUtils;
import com.aliucord.views.Button;
import com.aliucord.views.DangerButton;
import com.aliucord.views.Divider;
import com.aliucord.widgets.BottomSheet;
import com.aliucord.wrappers.ChannelWrapper;
import com.aliucord.wrappers.GuildWrapper;
import com.aliucord.api.CommandsAPI;
import com.discord.api.commands.ApplicationCommandType;

import com.discord.api.channel.Channel;
import com.discord.api.guild.Guild;
import com.discord.api.channel.ForumTag;
import com.discord.app.AppBottomSheet;
import com.discord.stores.StoreStream;
import com.discord.stores.StoreThreadDraft;
import com.discord.utilities.rest.RestAPI;
import com.discord.widgets.chat.MessageManager;
import com.discord.widgets.forums.ForumPostCreateManager;
import com.discord.widgets.channels.list.WidgetChannelsListItemThreadActions;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.views.TextInput;
import com.discord.api.permission.Permission;
import com.discord.utilities.permissions.PermissionUtils;
import com.aliucord.api.SettingsAPI;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import com.discord.widgets.share.WidgetIncomingShare;
import android.graphics.drawable.ColorDrawable;

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
import com.lytefast.flexinput.model.Attachment;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONObject;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import com.discord.widgets.auth.WidgetRemoteAuthViewModel;
import com.discord.utilities.rest.RestAPI;
import com.discord.restapi.RestAPIParams;
import com.discord.restapi.RestAPIParams.MFALogin;


@AliucordPlugin
public class QRscanFix extends Plugin {
	private final Logger logger = new Logger("QRscanFix");
	private String userInput;
	private String extractedTicket;
	private boolean reInvoked;
	private boolean isQRsent;

	@Override
	public void start(Context context) throws NoSuchMethodException {
		try {
			Method stringMethod = okhttp3.ResponseBody.class.getDeclaredMethod("d");

			patcher.patch(stringMethod, new Hook(cf -> {
				String content = (String) cf.getResult();
				logger.info("content: " + content);
				if (isQRsent && content != null && content.contains("\"code\"") && content.contains("60003") && content.contains("ticket")) {
					try {
						isQRsent = false;
						String targetKey = "\"ticket\":\"";
						int start = content.indexOf(targetKey) + targetKey.length();
						int end = content.indexOf("\"", start);
						extractedTicket = content.substring(start, end);
						
						logger.info("ticket: " + extractedTicket);
						
						if (extractedTicket != null && !extractedTicket.isEmpty()) {
							
							//MFALogin mfaLoginBody = new MFALogin(extractedTicket, userInput);

							//RestAPI.api.postMFACode(mfaLoginBody).V(response -> {});
							Map<String, Object> requestBody = new HashMap<>();
							//requestBody.put("mfa_method", "totp");
							requestBody.put("ticket", extractedTicket);
							requestBody.put("code", userInput);
							
							new Thread(new Runnable() {
								@Override
								public void run() {
									try {
										//Http.Response response = Http.Request.newDiscordRNRequest("/users/@me/remote-auth", "POST").executeWithJson(requestBody);
										Http.Response response2 = Http.Request.newDiscordRNRequest("/auth/mfa/password", "POST").executeWithJson(requestBody);
										//logger.info("response: " + response.text());
										logger.info("response2: " + response2.text());
									} catch (Exception e) {
										String err = e.getMessage();
										String rawMsg = err.contains("\"message\": \"") ? err.split("\"message\": \"")[1].split("\"")[0] : err;
										Properties p = new Properties();
										String msg;
										try {
											p.load(new java.io.StringReader("m=" + rawMsg));
											msg = p.getProperty("m");
										} catch (Exception ignored) {
											msg = err;
										}

										Utils.showToast(msg);
										logger.error("Error", e);
									}
								}
							}).start();
							
							//Response<ModelLoginResult>
							userInput = "";
							extractedTicket = "";
							logger.info("without error V");
						}
					} catch (Exception e) {
						logger.error("MFA Auto Send Error", e);
					}
				}
			}));
		} catch (NoSuchMethodException e) {
			logger.error("Failed to patch ResponseBody.d()", e);
		} catch (Throwable e) {
			logger.error(e);
		}

		try {
			Method remoteMethod = WidgetRemoteAuthViewModel.class.getDeclaredMethod("remoteLogin");

			patcher.patch(remoteMethod, new PreHook(cf -> {
				if (reInvoked) {
					return;
				}

				cf.setResult(null);

				var appCtx = Utils.getAppActivity();
				if (appCtx != null && !appCtx.isFinishing()) {
					appCtx.finish();
				}

				var bottomSheet = new BottomSheet() {
					private void closePage() {
						Utils.mainThread.post(() -> {
							try {
								dismiss();
							} catch (Exception ignored) {}

							var activity = getActivity();
							if (activity != null && !activity.isFinishing()) {
								activity.finish();
							}
						});
					}

					@Override
					public void onViewCreated(@androidx.annotation.NonNull android.view.View view, @androidx.annotation.Nullable android.os.Bundle savedInstanceState) {
						super.onViewCreated(view, savedInstanceState);

						Context ctx = view.getContext();

						LinearLayout root = getLinearLayout();
						root.setPadding(40, 40, 40, 40);

						TextView titleView = new TextView(ctx, null, 0, R.i.UiKit_Settings_Text);
						titleView.setText("Input 2FA CODE");
						titleView.setTextSize(18);
						titleView.setTypeface(null, android.graphics.Typeface.BOLD);
						titleView.setPadding(0, 0, 0, 20);
						root.addView(titleView);

						TextView descView = new TextView(ctx, null, 0, R.i.UiKit_Settings_Text);
						descView.setText("If you don't have 2FA, then input PASSWORD");
						descView.setPadding(0, 0, 0, 30);
						root.addView(descView);

						TextInput textInput = new TextInput(ctx, "CODE");
						root.addView(textInput);

						Button confirmButton = new Button(ctx);
						confirmButton.setText("OK");
						confirmButton.setOnClickListener(v -> {
							String code = textInput.getEditText().getText().toString().trim();
							if (!code.isEmpty()) {
								userInput = code;
							}
							closePage();

							try {
								reInvoked = true;
								remoteMethod.invoke(cf.thisObject);
								reInvoked = false;
							} catch (Exception e) {
								logger.error("ERR01", e);
							}
						});
						root.addView(confirmButton);

						DangerButton cancelButton = new DangerButton(ctx);
						cancelButton.setText("Cancel");
						cancelButton.setOnClickListener(v -> {
							closePage();
						});
						root.addView(cancelButton);
					}

					@Override
					public void onDismiss(@androidx.annotation.NonNull android.content.DialogInterface dialog) {
						super.onDismiss(dialog);
					}
				};

				Utils.openPageWithProxy(appCtx, bottomSheet);
			}));

		} catch (Throwable e) {
			logger.error(e);
		}

		try {
			Method buildMethod = okhttp3.Request.a.class.getDeclaredMethod("a");

			patcher.patch(buildMethod, new Hook(cf -> {
				okhttp3.Request request = (okhttp3.Request) cf.getResult();
				if (request == null) return;

				f0.w url = request.b;
				if (url != null && url.toString().contains("remote-auth/finish")) {
					isQRsent = true;
					cf.setResult(request);
				}
			}));
		} catch (NoSuchMethodException e) {
			logger.error("Target not found", e);
		}
	}
	
	@Override
	public void stop(Context context) { patcher.unpatchAll(); }
}
