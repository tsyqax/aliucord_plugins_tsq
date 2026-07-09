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
import com.aliucord.settings.*;
import com.discord.utilities.rest.RestAPI;
import com.discord.stores.StoreStream;
import com.discord.stores.SelectedChannelAnalyticsLocation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.Fragment;
import java.util.List;

import androidx.annotation.NonNull; 
import java.nio.charset.StandardCharsets; 
import okhttp3.MediaType; 
import okhttp3.RequestBody;
import okhttp3.Request;
import okhttp3.MultipartBody;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import com.discord.widgets.channels.list.WidgetChannelsListItemChannelActions;
import com.discord.widgets.channels.list.WidgetChannelsListItemThreadActions;
import com.discord.widgets.channels.settings.WidgetTextChannelSettings;
import com.discord.widgets.channels.settings.WidgetThreadSettings;
import com.discord.widgets.share.WidgetIncomingShare;
import com.discord.widgets.user.search.WidgetGlobalSearchModel;
import com.discord.utilities.permissions.PermissionUtils;
import com.aliucord.api.SettingsAPI;
import com.discord.views.CheckedSetting;

import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import java.util.HashMap;
import com.aliucord.views.Button;
import com.discord.api.channel.Channel;
import com.discord.utilities.rest.RestAPI;
import com.aliucord.views.TextInput;
import com.aliucord.utils.ReflectUtils;
import com.aliucord.utils.ChannelUtils;
import com.aliucord.utils.MDUtils;

import android.widget.LinearLayout;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.discord.api.permission.Permission;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.fragments.FragmentProxy;
import androidx.fragment.app.DialogFragment;
import com.aliucord.widgets.BottomSheet;
import com.aliucord.fragments.ConfirmDialog;
import com.aliucord.api.CommandsAPI;
import androidx.core.widget.NestedScrollView;
import com.discord.api.commands.ApplicationCommandType;

import java.util.Arrays;
import java.util.Collections;

import android.view.ViewGroup;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage;
import com.discord.widgets.chat.list.entries.ChatListEntry;
import com.discord.widgets.chat.list.WidgetChatList;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapter;
import com.aliucord.utils.DimenUtils;
import com.aliucord.views.Divider;
import com.discord.widgets.chat.list.entries.MessageEntry;
import java.util.function.Supplier;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lytefast.flexinput.R;


@AliucordPlugin(requiresRestart = true)
@SuppressWarnings("UItweaks")
public class UItweaks extends Plugin {
	public static SettingsAPI staticSettings;
	public static final Logger logger = new Logger("UItweaks");
	final int redColor = 0xFFED4245;
	
	public UItweaks() { 
		settingsTab = new SettingsTab(PSettings.class, SettingsTab.Type.PAGE).withArgs(settings);
	}
	
	// ----- settings start -----
	public static class PSettings extends SettingsPage {
		private final SettingsAPI settings;
		
		public PSettings(SettingsAPI settings) {
			this.settings = settings;
		}
		
		@Override
		public void onViewCreated(View view, Bundle bundle) {
			super.onViewCreated(view, bundle);
			setActionBarTitle("UItweaks");
			setActionBarSubtitle("Settings!");

			var context = view.getContext();
			var layout = getLinearLayout();
			
			CheckedSetting threadDEL  = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, "Add button to delete thread/channel on list menu","");
			threadDEL.setChecked(settings.getBool("threadDEL", true));
			threadDEL.setOnCheckedListener(Boolean -> {
				settings.setBool("threadDEL", Boolean);
				Utils.promptRestart();
			});
			
			CheckedSetting forumLine = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, "Add line to first forum message","");
			forumLine.setChecked(settings.getBool("forumLine", true));
			forumLine.setOnCheckedListener(Boolean -> {
				settings.setBool("forumLine", Boolean);
				Utils.promptRestart();
			});

			var threadInput = new TextInput(context, "Thread Delete Label", String.valueOf(settings.getString("thread_label", "Delete Thread")));
			var channelInput = new TextInput(context, "Channel Delete Label", String.valueOf(settings.getString("channel_label", "Delete Channel")));
			var saveButton = new Button(context);
			saveButton.setText("Save Settings");
			
			saveButton.setOnClickListener(v -> {
				var threadVal = ((Supplier<String>) () -> { try { return String.valueOf(threadInput.getEditText().getText().toString()); } catch (Exception e) { return "Delete Thread"; }}).get();
				var channelVal = ((Supplier<String>) () -> { try { return String.valueOf(channelInput.getEditText().getText().toString()); } catch (Exception e) { return "Delete Channel"; }}).get();

				settings.setString("thread_label", threadVal);
				settings.setString("channel_label", channelVal);
				
				Utils.promptRestart();
			});
			
			layout.addView(threadDEL);
			layout.addView(forumLine);
			
			if (settings.getBool("threadDEL", true)) {
				layout.addView(threadInput);
				layout.addView(channelInput);
				layout.addView(saveButton);
			}
		}
	}
	// ----- settings end -----
	


    @Override
    public void start(@NonNull Context context) throws Throwable { //h
		Boolean theradDEL = settings.getBool("threadDEL", true);
		Boolean forumLine = settings.getBool("forumLine", true);
		
		if (theradDEL) {
			String ThreadDelText = settings.getString("thread_label", "Delete Thread");
			String ChannelDelText = settings.getString("channel_label", "Delete Channel");
			
			// refered from ForwardMessage on https://github.com/reisxd/AliucordPlugins
			var viewId1 = View.generateViewId();
			var viewId2 = View.generateViewId();
			
			Drawable deleteIcon = ContextCompat.getDrawable(Utils.appActivity, com.lytefast.flexinput.R.e.ic_delete_24dp).mutate();
			Utils.tintToTheme(deleteIcon);
			deleteIcon.setTint(redColor); 
			
			Method bindingReflection = WidgetIncomingShare.class.getDeclaredMethod("getBinding");
			bindingReflection.setAccessible(true);
			Field modelCommentField = WidgetIncomingShare.Model.class.getDeclaredField("comment");
			modelCommentField.setAccessible(true);
			
			// thread delete
			patcher.patch(WidgetChannelsListItemThreadActions.class.getDeclaredMethod("configureUI", WidgetChannelsListItemThreadActions.Model.class),
				new PreHook(param -> {

					var actions = (WidgetChannelsListItemThreadActions) param.thisObject;
					var scrollView = (NestedScrollView) actions.getView();
					var lay = (LinearLayout) scrollView.getChildAt(0);
					
					long channelId = StoreStream.getChannelsSelected().getId();
					long permissions = StoreStream.getPermissions().getPermissionsByChannel().get(channelId);
					
					if (!PermissionUtils.can(Permission.MANAGE_THREADS, permissions)) return;
					
					lay.removeView(lay.findViewById(viewId1));
					if (lay.findViewById(viewId1) == null) {
						TextView tw = new TextView(lay.getContext(), null, 0,
								com.lytefast.flexinput.R.i.UiKit_Settings_Item_Icon);
						tw.setId(viewId1);
						tw.setText(ThreadDelText);
						tw.setTextColor(redColor); 
						
						tw.setCompoundDrawablesRelativeWithIntrinsicBounds(deleteIcon, null, null, null);
						
						int childrenCount = lay.getChildCount();
						boolean foundIndex = false;
						
						for (int i = 0; i < childrenCount; i++) {
							View view = lay.getChildAt(i);
							if (view.getId() == Utils.getResId("channels_list_item_thread_actions_leave", "id")) {
								foundIndex = true;
								lay.addView(tw, i + 1);
								break;
							}
						}
						
						if (!foundIndex) lay.addView(tw, 5);
						tw.setOnClickListener((v) -> {
							WidgetChannelsListItemThreadActions.Model model = (WidgetChannelsListItemThreadActions.Model) param.args[0];
							Channel willDeleteThread = model.getChannel();

							try {
								int titleRes = Utils.getResId("delete_thread", "string");
								int bodyRes = Utils.getResId("delete_channel_body", "string");
								//int themeId = Utils.getResId("Base_Theme_AppCompat_Dialog_Alert", "style");
								
								String channelName = ChannelUtils.getDisplayName(willDeleteThread);
								FragmentManager fm = actions.getParentFragmentManager();
								ConfirmDialog dialog = new ConfirmDialog();

								dialog.setTitle(context.getString(titleRes));
								dialog.setDescription(MDUtils.render(context.getString(bodyRes).replace("!!{channelName}!!", channelName)));

								dialog.setIsDangerous(true);
								
								dialog.setOnOkListener(w -> {
									dialog.dismiss();
									RestAPI.api.deleteChannel(willDeleteThread.k()).V(channel -> {});
									StoreStream.getMessagesLoader().jumpToMessage(1L, 1L);
									actions.dismiss();
									Utils.showToast("If not done, wait a second!");
								});
								
								dialog.show(fm, "confirm_tag");

							} catch (Exception e) {
								logger.error("Error in re-implemented confirmDelete", e);
								Utils.showToast("Error: " + e.getMessage());
							}
						});
					}
				})
			);
			
			// channel delete
			patcher.patch(WidgetChannelsListItemChannelActions.class.getDeclaredMethod("configureUI", WidgetChannelsListItemChannelActions.Model.class),
				new PreHook(param -> {
					var actions = (WidgetChannelsListItemChannelActions) param.thisObject;
					var scrollView = (NestedScrollView) actions.getView();
					var lay = (LinearLayout) scrollView.getChildAt(0);
					
					
					long channelId = StoreStream.getChannelsSelected().getId();
					long permissions = StoreStream.getPermissions().getPermissionsByChannel().get(channelId);
					
					if (!PermissionUtils.can(Permission.MANAGE_CHANNELS, permissions)) return;

					lay.removeView(lay.findViewById(viewId2));
					if (lay.findViewById(viewId2) == null) {
						TextView tw = new TextView(lay.getContext(), null, 0,
								com.lytefast.flexinput.R.i.UiKit_Settings_Item_Icon);
								
						tw.setId(viewId2);
						tw.setText(ChannelDelText);
						tw.setTextColor(redColor); 
						tw.setCompoundDrawablesRelativeWithIntrinsicBounds(deleteIcon, null, null, null);
						
						int childrenCount = lay.getChildCount();
						boolean foundIndex = false;
						
						for (int i = 0; i < childrenCount; i++) {
							View view = lay.getChildAt(i);
							if (view.getId() == Utils.getResId("action_invite", "id")) {
								foundIndex = true;
								lay.addView(tw, i + 1);
								break;
							}
						}
						
						if (!foundIndex) lay.addView(tw, 5);
						tw.setOnClickListener((v) -> {
							//Method confirmDelete = WidgetThreadSettings.class.getDeclaredMethod("confirmDelete", Channel.class);
							//confirmDelete.setAccessible(true);
							WidgetChannelsListItemChannelActions.Model model = (WidgetChannelsListItemChannelActions.Model) param.args[0];
							Channel willDeleteThread = model.getChannel();

							try {
								
								int titleRes = Utils.getResId("delete_channel", "string");
								int bodyRes = Utils.getResId("delete_channel_body", "string");
								//int themeId = Utils.getResId("Base_Theme_AppCompat_Dialog_Alert", "style");
								
								String channelName = ChannelUtils.getDisplayName(willDeleteThread);
								//FragmentManager fm = Utils.appActivity.getSupportFragmentManager();
								FragmentManager fm = actions.getParentFragmentManager();
								ConfirmDialog dialog = new ConfirmDialog();
								
								dialog.setTitle(context.getString(titleRes));
								dialog.setDescription(MDUtils.render(context.getString(bodyRes).replace("!!{channelName}!!", channelName)));

								dialog.setIsDangerous(true);

								dialog.setOnOkListener(w -> {
									dialog.dismiss();
									RestAPI.api.deleteChannel(willDeleteThread.k()).V(channel -> {});
									StoreStream.getMessagesLoader().jumpToMessage(1L, 1L);
									actions.dismiss();
									Utils.showToast("If not done, wait a second!");
								});
								
								dialog.show(fm, "confirm_tag");
								
							} catch (Exception e) {
								logger.error("Error in re-implemented confirmDelete", e);
								Utils.showToast("Error: " + e.getMessage());
							}
						});
					}
				})
			);
		}
		
		if (forumLine) {
			try {
				Method lineMethod = WidgetChatList.class.getDeclaredMethod("onViewBoundOrOnResume");

				patcher.patch(lineMethod, new Hook(param -> {
					try {
						Object thiz = param.thisObject;

						Method getBindingMethod = thiz.getClass().getDeclaredMethod("getBinding"); //private
						getBindingMethod.setAccessible(true);
						Object bindingObj = getBindingMethod.invoke(thiz);
						
						if (bindingObj == null) return;
						
						Field recyclerField = bindingObj.getClass().getDeclaredField("b");
						recyclerField.setAccessible(true);
						RecyclerView recyclerView = (RecyclerView) recyclerField.get(bindingObj);

						Field adapterField = thiz.getClass().getDeclaredField("adapter");
						adapterField.setAccessible(true);
						WidgetChatListAdapter chatAdapter = (WidgetChatListAdapter) adapterField.get(thiz);

						if (recyclerView != null && chatAdapter != null) {
							int tagKey = 0x7FF232F;

							if (recyclerView.getTag(tagKey) == null) {
								
								recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
									
									private final Paint paint = new Paint();
									{
										paint.setColor(Color.parseColor("#4E5058"));
										paint.setStrokeWidth(DimenUtils.dpToPx(5));
									}

									@Override
									public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
										int position = parent.getChildAdapterPosition(view);
										if (position == RecyclerView.NO_POSITION) return;

										try {
											List<ChatListEntry> entries = (List<ChatListEntry>) chatAdapter.getData().getList();

											if (entries != null && position < entries.size()) {
												Object entry = entries.get(position);
												
												if (entry instanceof MessageEntry && ((MessageEntry) entry).isGuildForumPostFirstMessage()) {
													outRect.bottom = DimenUtils.dpToPx(16);
												}
											}
										} catch (Exception e) {
											logger.error("Error in forumline deeply", e);
										}
									}

									@Override
									public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
										try {
											List<ChatListEntry> entries = (List<ChatListEntry>) chatAdapter.getData().getList();
											
											if (entries == null) return;

											for (int i = 0; i < parent.getChildCount(); i++) {
												View child = parent.getChildAt(i);
												int position = parent.getChildAdapterPosition(child);
												if (position == RecyclerView.NO_POSITION) continue;

												if (position < entries.size()) {
													Object entry = entries.get(position);

													if (entry instanceof MessageEntry && ((MessageEntry) entry).isGuildForumPostFirstMessage()) {

														float startX = parent.getPaddingLeft();
														float endX = parent.getWidth() - parent.getPaddingRight();
														
														float bottom = child.getBottom() + DimenUtils.dpToPx(8);
														float top = child.getTop();
														
														c.drawLine(startX, bottom, endX, bottom, paint);
													}
												}
											}
										} catch (Exception e) {
											logger.error("Error in forumline deeply2", e);
										}
									}
								});

								recyclerView.setTag(tagKey, true);
							}
						}
					} catch (Exception e) {
						logger.error("Error inside onViewBoundOrOnResume hook", e);
					}
				}));
			} catch (Exception e) {
				logger.error("Failed to patch WidgetChatList", e);
			}

		}

    }

    @Override
    public void stop(@NonNull Context context) {
        patcher.unpatchAll();
    }
}
