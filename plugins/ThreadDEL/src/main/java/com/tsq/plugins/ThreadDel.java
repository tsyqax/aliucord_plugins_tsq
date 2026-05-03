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

@AliucordPlugin(requiresRestart = true)
@SuppressWarnings("unused")
public class ThreadDel extends Plugin {
	public static SettingsAPI staticSettings;
	public static final Logger logger = new Logger("ThreadDEL");
	final int redColor = 0xFFED4245;
	
	public ThreadDel() { 
		settingsTab = new SettingsTab(Settings.class, SettingsTab.Type.PAGE);
	}
	
	// ----- settings start -----
	public static class Settings extends SettingsPage {

		@Override
		public void onViewCreated(View view, Bundle bundle) {
			super.onViewCreated(view, bundle);
			setActionBarTitle("ThreadDEL Settings");

			var context = view.getContext();
			var layout = getLinearLayout();

			var threadInput = new TextInput(context, "Thread Delete Label", "Label in context menu");
			threadInput.getEditText().setText(ThreadDel.staticSettings.getString("thread_label", "Delete Thread"));
			
			var channelInput = new TextInput(context, "Channel Delete Label", "Label for normal channels");
			channelInput.getEditText().setText(ThreadDel.staticSettings.getString("channel_label", "Delete Channel"));

			var saveButton = new Button(context);
			saveButton.setText("Save Settings");
			saveButton.setOnClickListener(v -> {
				String threadVal = threadInput.getEditText().getText().toString().trim();
				String channelVal = channelInput.getEditText().getText().toString().trim();

				ThreadDel.staticSettings.setString("thread_label", threadVal.isEmpty() ? "Delete Thread" : threadVal);
				ThreadDel.staticSettings.setString("channel_label", channelVal.isEmpty() ? "Delete Channel" : channelVal);

				Utils.showToast("Settings saved. Will Apply when restart App.");
				
				close();
			});

			layout.addView(threadInput);
			layout.addView(channelInput);
			layout.addView(saveButton);
		}
	}
	// ----- settings end -----
	


    @Override
    public void start(@NonNull Context context) throws Throwable { //h
		staticSettings = settings;
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

							FragmentManager fm = Utils.getAppActivity().getSupportFragmentManager();
							Fragment oldFrag = fm.findFragmentByTag("confirm_tag2");

							if (oldFrag != null) {
								fm.beginTransaction().remove(oldFrag).commitAllowingStateLoss();
								fm.executePendingTransactions();
							}

							ConfirmDialog dialog = new ConfirmDialog();
							
							dialog.setTitle(context.getString(titleRes));
							dialog.setDescription(MDUtils.render(context.getString(bodyRes).replace("!!{channelName}!!", channelName)));

							dialog.setIsDangerous(true);
							
							dialog.setOnOkListener(w -> {
								dialog.dismiss();
								actions.dismiss();
								RestAPI.api.deleteChannel(willDeleteThread.k()).V(channel -> {});
								
							});
							
							
							dialog.show(Utils.getAppActivity().getSupportFragmentManager(), "confirm_tag2");

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

							FragmentManager fm = Utils.getAppActivity().getSupportFragmentManager();
							Fragment oldFrag = fm.findFragmentByTag("confirm_tag1");

							if (oldFrag != null) {
								fm.beginTransaction().remove(oldFrag).commitAllowingStateLoss();
								fm.executePendingTransactions();
							}

							ConfirmDialog dialog = new ConfirmDialog();
							
							dialog.setTitle(context.getString(titleRes));
							dialog.setDescription(MDUtils.render(context.getString(bodyRes).replace("!!{channelName}!!", channelName)));

							dialog.setIsDangerous(true);
							
							dialog.setOnOkListener(w -> {
								RestAPI.api.deleteChannel(willDeleteThread.k()).V(channel -> {});
								dialog.dismiss();
								actions.dismiss();
							});
							
							dialog.show(Utils.getAppActivity().getSupportFragmentManager(), "confirm_tag1");
							
							
						} catch (Exception e) {
							logger.error("Error in re-implemented confirmDelete", e);
							Utils.showToast("Error: " + e.getMessage());
						}
                    });
                }
            })
		);
    }

    @Override
    public void stop(@NonNull Context context) {
        patcher.unpatchAll();
    }
}
