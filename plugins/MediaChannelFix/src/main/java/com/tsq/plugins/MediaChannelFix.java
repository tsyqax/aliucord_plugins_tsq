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
import com.aliucord.utils.DimenUtils;
import com.aliucord.views.Button;
import com.aliucord.views.DangerButton;
import com.discord.api.channel.Channel;
import com.aliucord.widgets.BottomSheet;
import com.aliucord.wrappers.ChannelWrapper;
import com.discord.api.channel.ForumTag;
import com.discord.utilities.rest.RestAPI;
import com.discord.stores.StoreStream;
import com.discord.widgets.forums.ForumPostCreateManager;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import com.discord.widgets.chat.MessageManager;
import com.discord.stores.StoreThreadDraft;
import kotlin.jvm.functions.Function2;
import android.content.Context;
import okhttp3.MultipartBody;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Headers;
import java.util.Iterator;
import androidx.fragment.app.FragmentActivity;
import rx.Observable;
import d0.t.n;
import android.view.Gravity;
import android.graphics.Typeface;
import android.widget.LinearLayout;
import com.discord.app.AppBottomSheet;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.LayoutInflater;

@AliucordPlugin
public class MediaChannelFix extends Plugin {
	private final Logger logger = new Logger("MediaChannelFix");
	private final List<Long> selectedTagIds = new ArrayList<>();
	private boolean isReinvoked = false;
	private Object[] capturedSendArgs;
	private boolean isSendingManually = false;
	private String bonmun;
	private String name;

	@Override
	public void start(Context context) throws NoSuchMethodException {
		patcher.patch(Channel.class, "D", new Class<?>[]{}, new Hook(param -> {
			if ((int) param.getResult() == 16) {
				param.setResult(15);
			}
		}));
	}


	@Override
	public void stop(Context context) { patcher.unpatchAll(); }
}
