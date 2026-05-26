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



@AliucordPlugin
public class FixOnboardingFork extends Plugin {
	private final Logger logger = new Logger("FixOnboardingFork");
	public List<String> selectedResponses = new ArrayList<>();
	public Map<String, Long> promptsSeen = new LinkedHashMap<>();
	public Map<String, Long> responsesSeen = new LinkedHashMap<>();
	private int userSelection;
	private String userRealSelection;
	//private String resText;
	private boolean[] multiSelectionFlags;

	@Override
	public void start(Context context) throws NoSuchMethodException {
		commands.registerCommand(
            "onboarding",
            "Display Onboarding",
            ctx -> {
				try {
					this.selectedResponses.clear();
					this.promptsSeen.clear();
					this.responsesSeen.clear();
					
					String guildId = String.valueOf(StoreStream.getGuildSelected().getSelectedGuildId());
					String userId = String.valueOf(ctx.getMe().getId());
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								String resText = Http.Request.newDiscordRequest(String.format("/guilds/%s/onboarding", guildId), "GET").execute().text();
								List<JSONObject> questions = parseQuestions(resText);

								 if (questions.isEmpty()) {
									Utils.showToast("There are no Onboarding.");
									return;
								}
								new Handler(Looper.getMainLooper()).post(() -> {
									showChainDialog(getSafeActivity(ctx.getContext()), questions, 0, guildId, userId);
								});
								
							} catch (Exception e) {
								logger.error("Error", e);
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
							}
						}
					}).start();
					
					//showChainDialog(context, questions, 0, guildId, userId); // start question from idx 0
					
					return new CommandsAPI.CommandResult("Fetching onboarding details...", null, false);
                } catch (Throwable t) {
					logger.error("CMD Thowable", t);
                    return new CommandsAPI.CommandResult("Error: '" + t.getMessage() , null, false);
					
                }
            }
        );

	}
	
	public static void showToast(Context context, String message) {
		try {
			Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			Utils.showToast(e.getMessage());
		}
	}
	
	//int themeColor = ContextCompat.getColor(context, R.i.UiKit_Settings_Text);
	
	private Activity getSafeActivity(Context context) { // refered from FixOnboarding by @scourage_main
		Context currentCtx = context;

		while (currentCtx instanceof ContextWrapper) {
			if (currentCtx instanceof Activity) {
				Activity act = (Activity) currentCtx;
				if (!act.isFinishing() && !act.isDestroyed()) {
					return act;
				}
			}
			currentCtx = ((ContextWrapper) currentCtx).getBaseContext();
		}
		
		Activity appActivity = Utils.getAppActivity();
		if (appActivity != null && !appActivity.isFinishing() && !appActivity.isDestroyed()) {
			return appActivity;
		}
		
		return null;
	}
	
	public List<JSONObject> parseQuestions(String jsonString) {
		List<JSONObject> questionList = new ArrayList<>();
		try {
			JSONObject root = new JSONObject(jsonString);
			JSONArray prompts = root.optJSONArray("prompts");
			
			if (prompts != null) {
				for (int i = 0; i < prompts.length(); i++) {
					questionList.add(prompts.getJSONObject(i));
				}
			}
		} catch (Exception e) {}
		return questionList;
	}
	
	
	public void addAnswer(String promptId, List<String> allOptionIdsOfPrompt, String chosenOptionId) {
        long currentTime = System.currentTimeMillis(); //seenTime
        this.selectedResponses.add(chosenOptionId); //answer
        this.promptsSeen.put(promptId, currentTime); 

        for (String optionId : allOptionIdsOfPrompt) {
            this.responsesSeen.put(optionId, currentTime); // all seenTime for correct
        }
    }
	
	public JSONObject buildPayloadLast(String guildId, String userId) {
		JSONObject payload = new JSONObject();
		try {
			//payload.put("guild_id", guildId);
			//payload.put("user_id", userId);

			JSONArray responsesArray = new JSONArray();
			for (String respId : this.selectedResponses) {
				responsesArray.put(respId);
			}
			payload.put("onboarding_responses", responsesArray);

			JSONObject promptsSeenJson = new JSONObject(new java.util.LinkedHashMap<>());
			for (Map.Entry<String, Long> entry : this.promptsSeen.entrySet()) {
				promptsSeenJson.put(entry.getKey(), entry.getValue());
			}
			payload.put("onboarding_prompts_seen", promptsSeenJson);

			JSONObject responsesSeenJson = new JSONObject(new java.util.LinkedHashMap<>());
			for (Map.Entry<String, Long> entry : this.responsesSeen.entrySet()) {
				responsesSeenJson.put(entry.getKey(), entry.getValue());
			}
			payload.put("onboarding_responses_seen", responsesSeenJson);
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return payload;
	}

	private void showChainDialog(Context context, List<JSONObject> questions, int index, String guildId, String userId) {

		if (index >= questions.size()) { // Questions Ended
			JSONObject finalPayload = buildPayloadLast(guildId, userId);
			logger.info(finalPayload.toString());
			
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Http.Request req = Http.Request.newDiscordRequest(String.format("/guilds/%s/onboarding-responses", guildId), "POST");
						req.setHeader("Content-Type", "application/json");
						String resText = req.executeWithBody(finalPayload.toString()).text();
						Utils.showToast("Done!");
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
						logger.error("ERR", e);
					}
				}
			}).start();
			return;
		}

		try {
			JSONObject currentPrompt = questions.get(index);
			String promptId = currentPrompt.getString("id"); //question ID
			String allTitle = currentPrompt.getString("title"); // questions Title (Unicode Escape Sequence)
			boolean required = currentPrompt.optBoolean("required", false); // questions required
			boolean onlyOne = currentPrompt.optBoolean("single_select", false); // answer is onlyOne?
			JSONArray options = currentPrompt.getJSONArray("options"); //options -> id, title, description(optional)
			
			String[] optionTitles = new String[options.length()];
			List<String> allOptionIds = new ArrayList<>(); 


			for (int i = 0; i < options.length(); i++) {
				JSONObject opt = options.getJSONObject(i);
				optionTitles[i] = opt.getString("title");
				allOptionIds.add(opt.getString("id"));
			}
			
			
			userSelection = 0; //initialize
			userRealSelection = allOptionIds.get(0); //initialize
			String dialogTitle = " [" + (index + 1) + "/" + questions.size() + "]";

			if (required) {
				dialogTitle += " · Required";
			}
			if (!onlyOne) {
				dialogTitle += " · Multiable";
			}
			
			allTitle += dialogTitle;
			//dialogTitle += "DEBUG:" + Arrays.toString(optionTitles);

			AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(allTitle); // do not use setMessage(dialogTitle);
			
			if (onlyOne) {
				builder.setSingleChoiceItems(optionTitles, userSelection, (dialog, which) -> {
					this.userSelection = which; 
					this.userRealSelection = allOptionIds.get(which); 
				});
			} else {
				this.multiSelectionFlags = new boolean[options.length()];
				builder.setMultiChoiceItems(optionTitles, this.multiSelectionFlags, (dialog, which, isChecked) -> {
					this.multiSelectionFlags[which] = isChecked;
				});
			}

			builder.setNegativeButton("Cancel", null);
			
			builder.setPositiveButton("Next", (dialog, which) -> {
				List<String> chosenOptionIds = new ArrayList<>();
				
				if (onlyOne) {
					chosenOptionIds.add(this.userRealSelection);
					
				} else {
					for (int i = 0; i < this.multiSelectionFlags.length; i++) {
						if (this.multiSelectionFlags[i]) {
							chosenOptionIds.add(allOptionIds.get(i));
						}
					}
				}
				
				if (required && chosenOptionIds.isEmpty()) {
					showToast(context, "This question is required!");
					return;
				}
				
				for (String chosenId : chosenOptionIds) {
					this.addAnswer(promptId, allOptionIds, chosenId);
				}
				showChainDialog(context, questions, index + 1, guildId, userId);
				}
			);

			AlertDialog dialog = builder.create();
			dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
			dialog.show();

		} catch (Exception e) {
			Utils.showToast(e.getMessage());
		}
	}
	
	
	@Override
	public void stop(Context context) { patcher.unpatchAll(); }
}
