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

import com.discord.api.channel.Channel;
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

@AliucordPlugin
public class FileNameFix extends Plugin {
    private final Logger logger = new Logger("FileNameFix");
	private String name;

    @Override
    public void start(Context context) throws NoSuchMethodException {
		patcher.patch(Attachment.class.getDeclaredMethod("getDisplayName"),
			new Hook(it -> {
				String original = (String) it.getResult();
				
				int lastDotIndex = original.lastIndexOf('.');
				String nameWithoutExt = (lastDotIndex == -1) ? original : original.substring(0, lastDotIndex);
				String ext = (lastDotIndex == -1) ? "" : original.substring(lastDotIndex + 1);
				
				String cleanName = nameWithoutExt.replaceAll("[^\\x00-\\x7F]", "");
				cleanName = cleanName.trim();
				
				if (cleanName.isEmpty()) {
					cleanName = UUID.randomUUID().toString().substring(0, 8);
				}
				
				it.setResult(cleanName + (ext.isEmpty() ? "" : "." + ext));
			})
		);
	}
    @Override
    public void stop(Context context) { patcher.unpatchAll(); }
}
