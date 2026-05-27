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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;

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

import com.discord.models.domain.emoji.EmojiSet;
import com.discord.models.domain.emoji.EmojiCategory;
import com.discord.models.domain.emoji.Emoji;
import org.json.JSONArray;
import org.json.JSONObject;

@AliucordPlugin
public class EmojiRank extends Plugin {
	private final Logger logger = new Logger("EmojiRank");
	private String name;
	private String jsonResponse;
	private static long cached;
	private static List<String> cachedList = new ArrayList<>();
	final Class<?> emojiSetClass = EmojiSet.class;
	//final long TREND_ID = 9999999999999L;
	private static final int MAX_TREND_COUNT = 10; // max to 18
	private List<String> trendMojiList = new ArrayList<>();

	@Override
	public void start(Context context) throws NoSuchMethodException {
		patcher.patch(emojiSetClass.getDeclaredConstructors()[0], new PreHook(param -> {			
			try {
				Object[] args = param.args;
				
				//Map<Long, List<Object>> customEmojis = (Map<Long, List<Object>>) args[1];
				//List<Object> recentEmojis = (List<Object>) args[3];
				Map<String, Object> emojiIndex = (Map<String, Object>) args[2];
				long nowId = StoreStream.getGuildSelected().getSelectedGuildId();
				
				if (nowId == 0L) {
					return;
				}
				
				new Thread(() -> {
                    try {
                        if (cached != nowId) {
                            String response = Http.Request.newDiscordRequest(
                                String.format("/guilds/%s/top-emojis", nowId), "GET"
                            ).execute().text();
                            trendMojiList = parseEmojiIds(response);
                            cachedList = new ArrayList<>(trendMojiList);
                            cached = nowId;
                            logger.info("Cached List updated: " + nowId);
                        }
                    } catch (Exception e) {
                        logger.error("ERROR02", e);
                    }
                }).start();
			
				if (trendMojiList.size() > 0) {
					cached = nowId;
					logger.info("Cached updated: " + cached + "->" + nowId);
					List<Object> trendList = new ArrayList<>();
					for (String id : trendMojiList) {
						Object originalEmoji = emojiIndex.get(id);
						if (originalEmoji != null) {
							trendList.add(originalEmoji);
						}
					}
					//customEmojis.put(TREND_ID, trendList);
					//param.args[1] = customEmojis;
					param.args[3] = trendList;
				}
			} catch (Throwable th) {
				logger.error("ERROR04", th);
			}
		}));
	}
	
	public static List<String> parseEmojiIds(String jsonResponse) {
		List<String> emojiIds = new ArrayList<>();
		
		try {
			JSONObject jsonObject = new JSONObject(jsonResponse);
			JSONArray itemsArray = jsonObject.getJSONArray("items");

			for (int i = 0; i < MAX_TREND_COUNT; i++) {
				JSONObject itemObject = itemsArray.getJSONObject(i);
				String emojiId = itemObject.getString("emoji_id");
				emojiIds.add(emojiId);
			}
		} catch (Exception e) {}
			
		return emojiIds;
	}
	
	@Override
	public void stop(Context context) { patcher.unpatchAll(); }
}
