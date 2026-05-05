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
import com.discord.app.AppBottomSheet;
import com.discord.stores.StoreStream;
import com.discord.stores.StoreThreadDraft;
import com.discord.utilities.rest.RestAPI;
import com.discord.widgets.chat.MessageManager;
import com.discord.widgets.forums.ForumPostCreateManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import d0.t.n;
import kotlin.jvm.functions.Function2;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import rx.Observable;

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
		/* try {
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
		} */
		
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
			int p = DimenUtils.dpToPx(16);
			
			Set<Long> selectedSet = new java.util.HashSet<>(selectedTagIds);

			LinearLayout root = new LinearLayout(context);
			root.setOrientation(LinearLayout.VERTICAL);
			root.setPadding(p, p, p, p);
			root.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));

			TextView title = new TextView(context);
			title.setText("Select Tags");
			title.setTextSize(18f);
			title.setTypeface(null, android.graphics.Typeface.BOLD);
			title.setTextColor(Color.WHITE);
			title.setPadding(0, 0, 0, p);
			root.addView(title);

			// RecyclerView
			// NestedScrollView + LinearLayout
			RecyclerView rv = new RecyclerView(context);
			rv.setLayoutManager(new LinearLayoutManager(context));
			
			TagAdapter adapter = new TagAdapter(tags, selectedSet);
			rv.setAdapter(adapter);
			
			LinearLayout.LayoutParams rvParams = new LinearLayout.LayoutParams(-1, 0, 1.0f);
			root.addView(rv, rvParams);

			Button confirm = new Button(context);
			confirm.setText("OK");
			confirm.setOnClickListener(v -> {
				selectedTagIds.clear();
				selectedTagIds.addAll(selectedSet);
				if (onComplete != null) onComplete.run();
				closePage(); });
			root.addView(confirm);
			
			DangerButton cancel = new DangerButton(context);
			cancel.setText("Cancel");
			cancel.setOnClickListener(v -> closePage());
			root.addView(cancel);

			addView(root);
		}
		
		private static class TagAdapter extends RecyclerView.Adapter<TagAdapter.VH> {
			private final List<ForumTag> data;
			private final Set<Long> selected;

			TagAdapter(List<ForumTag> data, Set<Long> selected) {
				this.data = data;
				this.selected = selected;
			}

			@Override
			public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
				TextView tv = new TextView(parent.getContext());
				int p = DimenUtils.dpToPx(16);
				tv.setPadding(p, p, p, p);
				tv.setTextColor(Color.WHITE);
				tv.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
				return new VH(tv);
			}

			@Override
			public void onBindViewHolder(VH holder, int position) {
				ForumTag tag = data.get(position);
				long id = tag.c();
				holder.tv.setText((tag.b() != null ? tag.b() + " " : "") + tag.d());
				
				holder.tv.setBackgroundColor(selected.contains(id) ? 0x405865F2 : 0);

				holder.tv.setOnClickListener(v -> {
					if (selected.contains(id)) {
						selected.remove(id);
						holder.tv.setBackgroundColor(0);
					} else {
						selected.add(id);
						holder.tv.setBackgroundColor(0x405865F2);
					}
				});
			}

			@Override
			public int getItemCount() { return data.size(); }

			static class VH extends RecyclerView.ViewHolder {
				TextView tv;
				VH(View v) { super(v); tv = (TextView) v; }
			}
		}

	}

    @Override
    public void stop(Context context) { patcher.unpatchAll(); }
}
