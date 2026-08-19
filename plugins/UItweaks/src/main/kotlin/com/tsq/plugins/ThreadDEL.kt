package com.tsq.plugins

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.FragmentManager

import com.discord.app.AppBottomSheet
import com.discord.api.channel.Channel
import com.discord.api.permission.Permission
import com.discord.stores.StoreStream
import com.discord.utilities.permissions.PermissionUtils
import com.discord.utilities.rest.RestAPI
import com.discord.widgets.channels.list.WidgetChannelsListItemChannelActions
import com.discord.widgets.channels.list.WidgetChannelsListItemThreadActions
import com.discord.widgets.share.WidgetIncomingShare
import com.lytefast.flexinput.R

import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.api.SettingsAPI
import com.aliucord.api.PatcherAPI
import com.aliucord.fragments.ConfirmDialog
import com.aliucord.patcher.*;
import com.aliucord.settings.*;
import com.aliucord.utils.ChannelUtils
import com.aliucord.utils.MDUtils

object ThreadDEL {
	fun init(context: Context, patcher: PatcherAPI, logger: Logger, settings: SettingsAPI) {
		val redColor = 0xFFED4245.toInt()
		val threadDelText = settings.getString("thread_label", "Delete Thread")
		val channelDelText = settings.getString("channel_label", "Delete Channel")
		
		// refered from ForwardMessage on https://github.com/reisxd/AliucordPlugins
		val viewId1 = View.generateViewId()
		val viewId2 = View.generateViewId()
		
		val deleteIcon = ContextCompat.getDrawable(Utils.appActivity, R.e.ic_delete_24dp)?.mutate() as Drawable
		Utils.tintToTheme(deleteIcon)
		deleteIcon.setTint(redColor)
		
		val bindingReflection = WidgetIncomingShare::class.java.getDeclaredMethod("getBinding")
		val modelCommentField = WidgetIncomingShare.Model::class.java.getDeclaredField("comment")
		bindingReflection.isAccessible = true
		modelCommentField.isAccessible = true
		
		val thListMethod by lazy { WidgetChannelsListItemThreadActions::class.java.getDeclaredMethod("configureUI", WidgetChannelsListItemThreadActions.Model::class.java) }
		val chListMethod by lazy { WidgetChannelsListItemChannelActions::class.java.getDeclaredMethod("configureUI", WidgetChannelsListItemChannelActions.Model::class.java) }
		
		// power delete
		val createDeleteHook = { viewId: Int, buttonText: String, foundResId: Int, titleRes: Int ->
			PreHook { param ->
				val actions = (param.thisObject as? WidgetChannelsListItemThreadActions) ?: (param.thisObject as? WidgetChannelsListItemChannelActions) as AppBottomSheet
				val scrollView = actions.getView() as NestedScrollView
				val lay = scrollView.getChildAt(0) as LinearLayout
				
				val channelId = StoreStream.getChannelsSelected().getId()
				val permissions = StoreStream.getPermissions().getPermissionsByChannel().get(channelId)
				
				if (!PermissionUtils.can(Permission.MANAGE_THREADS, permissions)) return@PreHook
				
				lay.removeView(lay.findViewById(viewId))
				
				if (lay.findViewById<View>(viewId) == null) {
					val tw = TextView(lay.getContext(), null, 0, R.i.UiKit_Settings_Item_Icon)
					tw.id = viewId
					tw.setText(buttonText)
					tw.setTextColor(redColor)
					tw.setCompoundDrawablesRelativeWithIntrinsicBounds(deleteIcon, null, null, null)
					
					val childrenCount = lay.getChildCount()
					var foundIndex = false
					
					for (i in 0 until childrenCount) {
						val view = lay.getChildAt(i) as View
						
						if (view.id == foundResId) {
							foundIndex = true
							lay.addView(tw, i + 1)
							break
						}
					}
						
					if (!foundIndex) lay.addView(tw, 5)
					tw.setOnClickListener {
						val model = param.args[0] as? WidgetChannelsListItemThreadActions.Model ?: param.args[0] as? WidgetChannelsListItemChannelActions.Model
						val willDeleteThread = (param.args[0] as? WidgetChannelsListItemThreadActions.Model)?.getChannel() ?: (param.args[0] as? WidgetChannelsListItemChannelActions.Model)?.getChannel() ?: return@setOnClickListener

						try {
							val bodyRes = Utils.getResId("delete_channel_body", "string")
							
							val channelName = ChannelUtils.getDisplayName(willDeleteThread)
							val fm = actions.getParentFragmentManager()
							val dialog = ConfirmDialog()

							dialog.setTitle(context.getString(titleRes))
							dialog.setDescription(MDUtils.render(context.getString(bodyRes).replace("!!{channelName}!!", channelName ?: "")))

							dialog.setIsDangerous(true)
							
							dialog.setOnOkListener {
								dialog.dismiss()
								RestAPI.api.deleteChannel(willDeleteThread.k()).V { channel -> } 
								StoreStream.getMessagesLoader().jumpToMessage(1L, 1L)
								actions.dismiss()
								Utils.showToast("If not done, wait a second!")
							}
							
							dialog.show(fm, "confirm_tag")

						} catch (e: Exception) {
							logger.error("threadDEL: 1", e)
							Utils.showToast("Error: " + e.message)
						}
					}
				}
			}
		}
		// viewId: Int, foundResId: Int, titleRes: Int
		patcher.patch(thListMethod, createDeleteHook(viewId1,threadDelText,  Utils.getResId("channels_list_item_thread_actions_leave", "id"), Utils.getResId("delete_thread", "string")))
		patcher.patch(chListMethod, createDeleteHook(viewId2, channelDelText, Utils.getResId("action_invite", "id"), Utils.getResId("delete_channel", "string")))
	}
}
