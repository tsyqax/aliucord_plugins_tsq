package com.tsq.plugins;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;

import com.aliucord.Http;
import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.SettingsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.patcher.*;
import com.aliucord.utils.DimenUtils;
import com.aliucord.utils.MDUtils;
import com.aliucord.views.Button;
import com.aliucord.views.DangerButton;
import com.aliucord.views.Divider;
import com.aliucord.views.TextInput;
import com.aliucord.widgets.BottomSheet;
import com.aliucord.wrappers.ChannelWrapper;

import com.discord.api.channel.Channel;
import com.discord.api.channel.ForumTag;
import com.discord.api.permission.Permission;
import com.discord.app.AppBottomSheet;
import com.discord.databinding.WidgetChatListAdapterItemThreadDraftFormBinding;
import com.discord.stores.StoreStream;
import com.discord.stores.StoreThreadDraft;
import com.discord.utilities.permissions.PermissionUtils;
import com.discord.utilities.rest.RestAPI;
import com.discord.widgets.channels.list.WidgetChannelsListItemThreadActions;
import com.discord.widgets.chat.MessageManager;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemThreadDraftForm;
import com.discord.widgets.chat.list.entries.ChatListEntry;
import com.discord.widgets.chat.list.entries.ThreadDraftFormEntry;
import com.discord.widgets.forums.ForumPostCreateManager;
import com.discord.widgets.share.WidgetIncomingShare;

import com.lytefast.flexinput.R;
import d0.t.n;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import kotlin.jvm.functions.Function2;
import kotlin.Unit;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import rx.Observable;
import rx.Subscription;

@AliucordPlugin
public class ForumTagFix extends Plugin {
	public static SettingsAPI staticSettings;
    private final Logger logger = new Logger("ForumTagFix");
    private final List<Long> selectedTagIds = new ArrayList<>();
    private boolean isReinvoked = false;
	private Object[] capturedSendArgs;
	private boolean isSendingManually = false;
	private String bonmun;
	private String name;
	private int lastTagCount = -1;

	public ForumTagFix() { 
		settingsTab = new SettingsTab(Settings.class, SettingsTab.Type.PAGE);
	}
	
	// ----- settings start -----
	public static class Settings extends SettingsPage {

		@Override
		public void onViewCreated(View view, Bundle bundle) {
			super.onViewCreated(view, bundle);
			setActionBarTitle("ForumTagFix Settings");

			var context = view.getContext();
			var layout = getLinearLayout();

			var threadInput = new TextInput(context, "Tag Change Label", "Label in context menu");
			threadInput.getEditText().setText(ForumTagFix.staticSettings.getString("change_tag", "Change Tags"));

			var saveButton = new Button(context);
			saveButton.setText("Save Settings");
			saveButton.setOnClickListener(v -> {
				String threadVal = threadInput.getEditText().getText().toString().trim();

				ForumTagFix.staticSettings.setString("change_tag", threadVal.isEmpty() ? "Change Tags" : threadVal);

				Utils.showToast("Settings saved. Will Apply when restart App.");
				
				close();
			});

			layout.addView(threadInput);
			layout.addView(saveButton);
		}
	}
	// ----- settings end -----


    @Override
    public void start(Context context) throws NoSuchMethodException {
		
		staticSettings = settings;
		var viewId1 = View.generateViewId();
		
		Method bindingReflection = WidgetIncomingShare.class.getDeclaredMethod("getBinding");
		bindingReflection.setAccessible(true);
		
		Drawable changeIcon = ContextCompat.getDrawable(Utils.appActivity, R.e.ic_edit_24dp).mutate();
		Utils.tintToTheme(changeIcon);
		
		Method cMethod = okhttp3.MultipartBody.a.class.getDeclaredMethod("b");
		String changeTagText = settings.getString("change_tag", "Change Tags");
		
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

				selectedTagIds.clear();
				
				if (availableTags == null || availableTags.isEmpty()) return;

				TagPickerSheet sheet = new TagPickerSheet(availableTags, () -> {
					try {
						isReinvoked = true;
						((Method) cf.method).setAccessible(true); 
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
					
					selectedTagIds.clear();
					
					logger.info(jsonContent); // for debug
				} catch (Exception e) {
					logger.error(">>> Append Failed", e);
				}
			}
		}));
		
		//thread actions (for tags)
		patcher.patch(WidgetChannelsListItemThreadActions.class.getDeclaredMethod("configureUI", WidgetChannelsListItemThreadActions.Model.class),
            new PreHook(cf -> {

                var actions = (WidgetChannelsListItemThreadActions) cf.thisObject;
                var scrollView = (NestedScrollView) actions.getView();
                var lay = (LinearLayout) scrollView.getChildAt(0);
				
				long channelId = StoreStream.getChannelsSelected().getId();
				long permissions = StoreStream.getPermissions().getPermissionsByChannel().get(channelId);
				
				WidgetChannelsListItemThreadActions.Model model = (WidgetChannelsListItemThreadActions.Model) cf.args[0];
				Channel willDelete = model.getChannel();

				ChannelWrapper wrapper2 = new ChannelWrapper(StoreStream.getChannels().getChannel(channelId));
				ChannelWrapper wrapper1 = new ChannelWrapper(willDelete);
				List<Long> appliedTags = wrapper1.getAppliedTags();
				List<ForumTag> availableTags = wrapper2.getAvailableTags();
				Integer chType = wrapper2.getType();
				
				logger.info("CHtype: " + chType);
				if (chType != 15 && chType != 16) return; // 15 = forum, 16 = media
				
				if (!PermissionUtils.can(Permission.MANAGE_THREADS, permissions)) return;
				
				selectedTagIds.clear();
				
				lay.removeView(lay.findViewById(viewId1));
                if (lay.findViewById(viewId1) == null) {
                    TextView tw = new TextView(lay.getContext(), null, 0, R.i.UiKit_Settings_Item_Icon);
                    tw.setId(viewId1);
                    tw.setText(changeTagText);
					
                    tw.setCompoundDrawablesRelativeWithIntrinsicBounds(changeIcon, null, null, null);
					
                    int childrenCount = lay.getChildCount();
                    boolean foundIndex = false;
					
                    for (int i = 0; i < childrenCount; i++) {
                        View view = lay.getChildAt(i);
                        if (view.getId() == Utils.getResId("channels_list_item_thread_actions_leave", "id")) {
                            foundIndex = true;
                            lay.addView(tw, i - 2);
                            break;
                        }
                    }
					
                    if (!foundIndex) lay.addView(tw, 7);
					
					TagPickerSheet sheet = new TagPickerSheet(availableTags, appliedTags, () -> {
						try {
							logger.info("applied: " + appliedTags);
							//logger.info("available: " + availableTags);
							logger.info("Selected: " + selectedTagIds);
							//String jsonStr = String.format("{\"applied_tags\":%s}", selectedTagIds.toString().replace(" ", ""));
							Map<String, Object> requestBody = new HashMap<>();
							requestBody.put("applied_tags", selectedTagIds);
							
							new Thread(new Runnable() {
								@Override
								public void run() {
									try {
										// refered from EditWebooks by c10udburst-discord
										Http.Response response = Http.Request.newDiscordRequest(String.format("/channels/%s", String.valueOf(willDelete.k())), "PATCH").executeWithJson(requestBody);
										//logger.info("res: " + response.text());

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

						} catch (Exception e) { 
							logger.error(e); 
						} finally {
							isReinvoked = false;
						}
					});
					
                    tw.setOnClickListener((v) -> {
						Utils.openPageWithProxy(lay.getContext(), sheet);
					});
                }
            })
		);

		patcher.patch(
			WidgetChatListAdapterItemThreadDraftForm.class.getDeclaredMethod("onConfigure", int.class, ChatListEntry.class), 
			new Hook(param -> {
				WidgetChatListAdapterItemThreadDraftForm thiz = (WidgetChatListAdapterItemThreadDraftForm) param.thisObject;
				
				try {
					ChatListEntry dataEntry = (ChatListEntry) param.args[1];
					if (!(dataEntry instanceof ThreadDraftFormEntry)) return;
					
					ThreadDraftFormEntry formEntry = (ThreadDraftFormEntry) dataEntry;
					var rawChannel = formEntry.getParentChannel(); 
					
					if (rawChannel == null) return;
					
					ChannelWrapper wrapper3 = new ChannelWrapper(rawChannel);
					
					Integer chType = wrapper3.getType();
					if (chType == null || (chType != 15 && chType != 16)) return;
					
					var bindingField = thiz.getClass().getDeclaredField("binding");
					bindingField.setAccessible(true);
					WidgetChatListAdapterItemThreadDraftFormBinding binding = (WidgetChatListAdapterItemThreadDraftFormBinding) bindingField.get(thiz);
					
					View itemView = thiz.itemView;
					if (itemView instanceof ViewGroup) {
						ViewGroup rootLayout = (ViewGroup) itemView;

						String viewTag = "forumTagFix_plugin_indicator";
						TextView indicatorView = (TextView) rootLayout.findViewWithTag(viewTag);
						
						var availableTags = wrapper3.getAvailableTags();
						int tagCount = (availableTags != null) ? availableTags.size() : 0;
						String guideText = "\n**" + tagCount + "** tags found!";
						
						if (tagCount > 0) {
							guideText += "\nTag selector will display after click send button";
						}
						
						if (indicatorView != null && tagCount == lastTagCount) {
							return;
						}
							
						if (indicatorView == null) {
							indicatorView = new TextView(itemView.getContext());
							indicatorView.setTag(viewTag);
							indicatorView.setText(MDUtils.render(guideText));
							
							rootLayout.addView(indicatorView);
						} else {
							indicatorView.setText(MDUtils.render(guideText));
						}
					}
				} catch (Exception e) {
					logger.error("Error new Noti", e);
				}
			})
		);

		
	}


	public class TagPickerSheet extends BottomSheet {
		private final List<ForumTag> tags;
		private List<Long> apl_tags;
		private final Runnable onComplete;

		public TagPickerSheet(List<ForumTag> tags, Runnable onComplete) {
			this.tags = tags;
			this.onComplete = onComplete;
		}
		
		public TagPickerSheet(List<ForumTag> tags, List<Long> apl_tags, Runnable onComplete) {
			this.tags = tags;
			this.apl_tags = apl_tags;
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
			Set<Long> selectedSet;
			
			
			if (apl_tags == null) {
				selectedSet = new java.util.HashSet<>(selectedTagIds);
			} else {
				selectedSet = new java.util.HashSet<>(apl_tags);
			}

			LinearLayout root = new LinearLayout(context);
			root.setOrientation(LinearLayout.VERTICAL);
			root.setPadding(p, p, p, p);
			root.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));

			TextView title = new TextView(context, null, 0, R.i.UiKit_Settings_Text);
			title.setText("Select Tags");
			title.setTextSize(18f);
			title.setTypeface(null, android.graphics.Typeface.BOLD);
			//title.setTextColor(Color.WHITE);
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
				TextView tv = new TextView(parent.getContext(), null, 0, R.i.UiKit_Settings_Text);
				int p = DimenUtils.dpToPx(16);
				tv.setPadding(p, p, p, p);
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
			public int getItemCount() {
				try {
					return data.size();
				} catch (Exception e) {
					return 0;
				}
			}
			
			static class VH extends RecyclerView.ViewHolder {
				TextView tv;
				VH(View v) { super(v); tv = (TextView) v; }
			}
		}

	}

    @Override
    public void stop(Context context) { patcher.unpatchAll(); }
}
