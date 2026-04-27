package com.tsq.forumtagfix;

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
import com.aliucord.views.Divider;
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
public class ForumTagFix extends Plugin {
    private final Logger logger = new Logger("ForumTagFix");
    private final List<Long> selectedTagIds = new ArrayList<>();
    private boolean isReinvoked = false;
	private Object[] capturedSendArgs;
	private boolean isSendingManually = false;
	private String bonmun;
	private String name;

    @Override
    public void start(Context context) throws NoSuchMethodException {
		try {
			// use 'public final String d()' 
			java.lang.reflect.Method stringMethod = okhttp3.ResponseBody.class.getDeclaredMethod("d");

			patcher.patch(stringMethod, new com.aliucord.patcher.Hook(cf -> {
				String content = (String) cf.getResult();

				
				if (content != null ) {
					logger.info("─── [FOUND SERVER RESPONSE] ───");
					logger.info(content);
					logger.info("───────────────────────────────");
				}
			}));
		} catch (NoSuchMethodException e) {
			logger.error("Failed to patch ResponseBody.d()", e);
		} catch (Throwable e) {
			logger.error(e);
		}
		
		Method cMethod = okhttp3.MultipartBody.a.class.getDeclaredMethod("b");
		
		// [1] UI Trigger
        patcher.patch(ForumPostCreateManager.class.getDeclaredMethod("createForumPostWithMessage", 
			Context.class, MessageManager.class, long.class, int.class, String.class, 
			StoreThreadDraft.ThreadDraftState.class, MessageManager.AttachmentsRequest.class, 
			Function2.class, Function2.class), 
			new PreHook(cf -> {
				if (isReinvoked) return;

				long channelId = (long) cf.args[2];
				ChannelWrapper wrapper = new ChannelWrapper(StoreStream.getChannels().getChannel(channelId));
				
				final List<ForumTag> availableTags = wrapper.getAvailableTags();

				if (availableTags == null || availableTags.isEmpty()) return;

				selectedTagIds.clear();
				
				TagPickerSheet sheet = new TagPickerSheet(availableTags, () -> {
					try {
						isReinvoked = true;
						((Method) cf.method).invoke(cf.thisObject, cf.args);
					} catch (Exception e) { 
						logger.error(e); 
					} finally {
						isReinvoked = false;
					}
				});
				
				//sheet.show(Utils.getAppActivity().getSupportFragmentManager(), "confirm_tag2");
				
				Utils.openPageWithProxy((Context) cf.args[0], sheet);
				
				cf.setResult(null); // inject			
			}));

			
        // [2] get value
		//long channelId, String name, String content, List<Long> appliedTags, List<Long> stickerIds, int type, Integer autoArchiveDuration, MultipartBody.Part[] partArr
		patcher.patch(RestAPI.class.getDeclaredMethod("createThreadWithMessage", long.class, String.class, String.class, List.class, List.class, int.class, Integer.class, MultipartBody.Part[].class), new PreHook(cf -> {
			if (selectedTagIds != null && !selectedTagIds.isEmpty()) {
				//cf.args[3] = selectedTagIds;
				name = (String) cf.args[1];
				bonmun = (String) cf.args[2];
			}
		}));
		
		patcher.patch(cMethod, new PreHook(cf -> {
			if (selectedTagIds != null && !selectedTagIds.isEmpty()) {
				try {
					// 1. JSON String (even if they are in part, we must include name and content)
					//String jsonContent = "{\"applied_tags\":" + selectedTagIds.toString().replace(" ", "") +"}";
					String safeName = name.replace("\\", "\\\\").replace("\"", "\\\"");
					String safeBonmun = bonmun.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
					String jsonContent = String.format(
						"{\"name\":\"%s\",\"content\":\"%s\",\"applied_tags\":%s}",
						safeName, 
						safeBonmun, 
						selectedTagIds.toString().replace(" ", "")
					);
					// 2. MediaType Generate (Origin: MediaType.parse)
					// b(String) Method
					MediaType mediaType = MediaType.b("application/json; charset=utf-8");

					// 3. RequestBody Generate (Origin: RequestBody.create)
					// a(String, MediaType)  in RequestBody.Companion
					RequestBody requestBody = RequestBody.Companion.a(jsonContent, mediaType);

					// 4. Headers Generate (Origin: Headers.of)
					// c(String...) in Headers
					Headers headers = Headers.j.c("Content-Disposition", "form-data; name=\"payload_json\"");

					// 5. Part Object Generate (Origin: MultipartBody.Part.create)
					// a(Headers, RequestBody) in MultipartBody.Part
					MultipartBody.Part myPart = MultipartBody.Part.a(headers, requestBody);

					// 6. Inject tags to MultipartBody.a (Origin: Builder.addPart)
					// we inject to instance
					((MultipartBody.a) cf.thisObject).a(myPart);
					
					logger.info(jsonContent); // for debug
				} catch (Exception e) {
					logger.error(">>> Append Failed", e);
				}
			}
		}));
		
	}


	public class TagPickerSheet extends BottomSheet {
		private final List<ForumTag> tags;
		private final Runnable onComplete;

		public TagPickerSheet(List<ForumTag> tags, Runnable onComplete) {
			this.tags = tags;
			this.onComplete = onComplete;
		}
		
		private void closePage() {
			Utils.mainThread.post(() -> {
				try { dismiss(); } catch (Exception ignored) {}

				var activity = getActivity();
				if (activity != null && !activity.isFinishing()) {
					activity.finish(); 
				}
			});
		}

		@Override
		public void onViewCreated(View view, Bundle bundle) {
			super.onViewCreated(view, bundle);
			Context context = view.getContext();

			LinearLayout layout = new LinearLayout(context);
			int padding = DimenUtils.dpToPx(16);
			layout.setPadding(padding, padding, padding, padding);
			layout.setOrientation(LinearLayout.VERTICAL);

			TextView title = new TextView(context);
			title.setText("Select Tags");
			title.setTextSize(18f);
			title.setTypeface(null, android.graphics.Typeface.BOLD);

			title.setTextColor(Color.WHITE);
			title.setPadding(0, 0, 0, padding);
			layout.addView(title);

			for (ForumTag tag : tags) {
				TextView item = new TextView(context);
				String label = (tag.b() != null ? tag.b() + " " : "") + tag.d();
				item.setText(label);
				item.setTextColor(Color.WHITE);
				item.setPadding(padding, padding, padding, padding);
				
				long id = tag.c();

				if (selectedTagIds.contains(id)) {
					item.setBackgroundColor(0x405865F2);
				}

				item.setOnClickListener(v -> {
					if (selectedTagIds.contains(id)) {
						selectedTagIds.remove(id);
						item.setBackgroundColor(Color.TRANSPARENT);
					} else {
						selectedTagIds.add(id);
						item.setBackgroundColor(0x405865F2);
					}
				});
				layout.addView(item);
			}

			Button confirm = new Button(context);
			confirm.setText("OK");
			confirm.setOnClickListener(v -> {
				if (onComplete != null) onComplete.run();
				closePage();
			});
			layout.addView(confirm);

			DangerButton cancel = new DangerButton(context);
			cancel.setText("Cancel");
			cancel.setOnClickListener(v -> closePage());
			layout.addView(cancel);

			addView(layout);
			
		}
	}

    @Override
    public void stop(Context context) { patcher.unpatchAll(); }
}
