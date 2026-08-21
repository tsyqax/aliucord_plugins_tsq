package com.tsq.plugins

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView

import com.aliucord.Http
import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.SettingsAPI
import com.aliucord.entities.Plugin
import com.aliucord.fragments.SettingsPage
import com.aliucord.patcher.*
import com.aliucord.utils.MDUtils
import com.aliucord.views.Button
import com.aliucord.views.TextInput
import com.aliucord.wrappers.ChannelWrapper

import com.discord.api.permission.Permission
import com.discord.databinding.WidgetChatListAdapterItemThreadDraftFormBinding
import com.discord.stores.StoreStream
import com.discord.stores.StoreThreadDraft
import com.discord.utilities.permissions.PermissionUtils
import com.discord.utilities.rest.RestAPI
import com.discord.widgets.channels.list.WidgetChannelsListItemThreadActions
import com.discord.widgets.chat.MessageManager
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemThreadDraftForm
import com.discord.widgets.chat.list.entries.ChatListEntry
import com.discord.widgets.chat.list.entries.ThreadDraftFormEntry
import com.discord.widgets.forums.ForumPostCreateManager
import com.discord.widgets.share.WidgetIncomingShare

import java.util.ArrayList
import java.util.HashMap
import java.util.Properties
import java.io.StringReader

import kotlin.collections.List
import kotlin.Function2
import com.lytefast.flexinput.R
import d0.t.n

import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody

import rx.Observable
import rx.Subscription

@AliucordPlugin
class ForumTagFix: Plugin() {
	//private val selectedTagIds: List<Long> = ArrayList<Long>() as List<Long>
	private val selectedTagIds: MutableList<Long> = mutableListOf()
	private var isReinvoked: Boolean = false
	private var capturedSendArgs: Array<Any?>? = null
	private var isSendingManually: Boolean = false
	private var sBonmun: String? = null
	private var sName: String? = null
	private var lastTagCount = -1
	
	
	init {
		settingsTab = SettingsTab(PSettings::class.java, SettingsTab.Type.PAGE)
	}
	
	// ----- settings start -----
	class PSettings(private val settings: SettingsAPI) : SettingsPage() {
		override fun onViewCreated(view: View, bundle: Bundle?) {
			super.onViewCreated(view, bundle);
			setActionBarTitle("ForumTagFix")
			setActionBarSubtitle("Settings!")

			var ctx = view.context
			var layout = linearLayout
			
			var threadInput = TextInput(ctx, "Tag Change Label", "Label in context menu")
			threadInput.getEditText().setText(settings.getString("change_tag", "Change Tags"))

			var saveButton = Button(ctx)
			saveButton.text = "Save Settings"
			
			saveButton.setOnClickListener{v ->
				val threadVal = threadInput.getEditText().getText().toString().trim();

				settings.setString("change_tag", if (threadVal.isEmpty()) "Change Tags" else threadVal)

				Utils.promptRestart()
				
				close()
			}

			layout.addView(threadInput)
			layout.addView(saveButton)
		}
	}
	// ----- settings end -----
	
	override fun start(context: Context) {
		val viewId1 = View.generateViewId()
		
		val bindingReflection by lazy { WidgetIncomingShare::class.java.getDeclaredMethod("getBinding").apply {isAccessible = true } }
		
		val changeIcon = ContextCompat.getDrawable(Utils.appActivity, R.e.ic_edit_24dp)!!.mutate()
		Utils.tintToTheme(changeIcon)

		val changeTagText = settings.getString("change_tag", "Change Tags")
		
		// [1] UI Trigger
		val createForumMethod by lazy { ForumPostCreateManager::class.java.getDeclaredMethod("createForumPostWithMessage", Context::class.java, MessageManager::class.java, Long::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java, StoreThreadDraft.ThreadDraftState::class.java, MessageManager.AttachmentsRequest::class.java, Function2::class.java, Function2::class.java) }
		patcher.patch(createForumMethod, PreHook { param -> 
			if (isReinvoked) return@PreHook
			
			val channelId = param.args[2] as Long
			val wrapper = ChannelWrapper(StoreStream.getChannels().getChannel(channelId))
		
			val availableTags = wrapper.availableTags
			if (availableTags.isNullOrEmpty()) return@PreHook
			
			val originalFlow = param.result as? Observable<*>
			logger.info("ORINIGLA: " + originalFlow)
			
			val sheet = TagPickerSheet(availableTags.toMutableList(), selectedTagIds, Runnable {
				try {
					isReinvoked = true
					createForumMethod.isAccessible = true
					createForumMethod.invoke(param.thisObject, *param.args)
				} catch (e: Exception) { 
					logger.error("SheetLoad", e)
				} finally {
					isReinvoked = false
				}
			})

			Utils.openPageWithProxy(param.args[0] as Context, sheet)
			param.result = null 
		})		
		
		// [2] get value
		val createThreadMethod by lazy { RestAPI::class.java.getDeclaredMethod("createThreadWithMessage", Long::class.javaPrimitiveType, String::class.java, String::class.java, List::class.java, List::class.java, Int::class.javaPrimitiveType, Int::class.javaObjectType, Array<MultipartBody.Part>::class.java) }
		patcher.patch(createThreadMethod, PreHook { param ->
			if (!selectedTagIds.isNullOrEmpty()) {
				//cf.args[3] = selectedTagIds;
				sName = param.args[1] as? String
				sBonmun = param.args[2] as? String
			}
		})
		
		
		val cMethod = okhttp3.MultipartBody.a::class.java.getDeclaredMethod("b")
		patcher.patch(cMethod, PreHook { param ->
			if (selectedTagIds.isNullOrEmpty()) return@PreHook
			
			try {
				val safeName = sName?.replace("\\", "\\\\")?.replace("\"", "\\\"") ?: ""
				val safeBonmun = sBonmun?.replace("\\", "\\\\")?.replace("\"", "\\\"")?.replace("\n", "\\n") ?: ""
				val jsonContent = String.format("{\"name\":\"%s\",\"content\":\"%s\",\"applied_tags\":%s}", safeName, safeBonmun, selectedTagIds.toString().replace(" ", ""))
			
				val mediaType = MediaType.b("application/json; charset=utf-8")
				val headers = Headers.j.c("Content-Disposition", "form-data; name=\"payload_json\"")
				//val requestBody = RequestBody.Companion.a(jsonContent, mediaType)
				val requestBody = okhttp3.RequestBody.create(mediaType, jsonContent)
				
				val tagPart = MultipartBody.Part.a(headers, requestBody)
				
				val builder = param.thisObject as okhttp3.MultipartBody.a
				builder.a(tagPart)
				
				selectedTagIds.clear()
				
				logger.info(jsonContent) // for debug
				
			} catch (e: Exception) {
				logger.error("Multipart", e)
			}
		});
		
		//thread actions (for tags)
		val chListMethod = WidgetChannelsListItemThreadActions::class.java.getDeclaredMethod("configureUI", WidgetChannelsListItemThreadActions.Model::class.java)
		patcher.patch(chListMethod, PreHook { param ->
			val actions = param.thisObject as WidgetChannelsListItemThreadActions
			val scrollView = actions.view as NestedScrollView
			val lay = scrollView.getChildAt(0) as LinearLayout
			
			val channelId = StoreStream.getChannelsSelected().id
			val permissions = StoreStream.getPermissions().permissionsByChannel.get(channelId)
				
			val model = param.args[0] as WidgetChannelsListItemThreadActions.Model
			val willDelete = model.channel

			val wrapper = ChannelWrapper(willDelete)
			val wrapper2 = ChannelWrapper(StoreStream.getChannels().getChannel(channelId))

			val appliedTags = wrapper.appliedTags			
			val availableTags = wrapper2.availableTags
			val chType = wrapper2.type
			
			if (chType != 15 && chType != 16) return@PreHook
			
			if (!PermissionUtils.can(Permission.MANAGE_THREADS, permissions)) return@PreHook
			
			lay.findViewById<View>(viewId1) ?: let {
				val tv = TextView(lay.context, null, 0, R.i.UiKit_Settings_Item_Icon)
				tv.id = viewId1
				tv.text = changeTagText
				tv.setCompoundDrawablesRelativeWithIntrinsicBounds(changeIcon, null, null, null)
				
				val childrenCount = lay.childCount
				var foundIndex = false
				
				for (i in 0 until childrenCount) {
					val view = lay.getChildAt(i) as View
					if (view.id == Utils.getResId("channels_list_item_thread_actions_leave", "id")) {
						foundIndex = true
						lay.addView(tv, i - 2)
						break
					}
				}
					
				if (!foundIndex) lay.addView(tv, 7)
				
				val sheet = TagPickerSheet(availableTags.toMutableList(), selectedTagIds, appliedTags.toMutableList(), Runnable {
					try {
						logger.info("available: " + availableTags)
						logger.info("applied: " + appliedTags)
						logger.info("Selected: " + selectedTagIds)

						val requestBody = hashMapOf<String, Any?>("applied_tags" to selectedTagIds)
						
						kotlin.concurrent.thread(start = true) {
							try {
								// refered from EditWebooks by c10udburst-discord
								val response = Http.Request.newDiscordRequest("/channels/${willDelete.k()}", "PATCH").executeWithJson(requestBody)
								//logger.info("res: " + response.text())

							} catch (e: Exception) {
								var err = e.message ?: ""
								val rawMsg = if (err.contains("\"message\": \"")) err.split("\"message\": \"")[1].split("\"")[0] else err
									
								val p = Properties()
								var msg: String? = null
								try {
									p.load(StringReader("m=" + rawMsg))
									msg = p.getProperty("m")
								} catch (ignored: Exception) {
									msg = err
								}

								Utils.showToast(msg)
								logger.error("ChangeThread", e)
							}
						}

					} catch (e: Exception) { 
						logger.error("Change", e)
					}
				})
					
				tv.setOnClickListener { v ->
					Utils.openPageWithProxy(lay.context, sheet)
				}
			}
		})

		// indicator
		val chatThreadMethod = WidgetChatListAdapterItemThreadDraftForm::class.java.getDeclaredMethod("onConfigure", Int::class.javaPrimitiveType, ChatListEntry::class.java)
		patcher.patch(chatThreadMethod, Hook { param ->
			val form = param.thisObject as WidgetChatListAdapterItemThreadDraftForm

			try {
				val dataEntry = param.args[1] as ChatListEntry
				if (dataEntry !is ThreadDraftFormEntry) return@Hook
				
				val formEntry = dataEntry
				val rawChannel = formEntry.parentChannel
				
				if (rawChannel == null) return@Hook
				
				val wrapper = ChannelWrapper(rawChannel)
				
				val chType = wrapper.type
				if (chType != 15 && chType != 16) return@Hook
				
				var bindingField = form::class.java.getDeclaredField("binding")
				bindingField.isAccessible = true
				var binding = bindingField.get(form) as WidgetChatListAdapterItemThreadDraftFormBinding

				val itemView = form.itemView
				
				if (itemView is ViewGroup) {
					val root = itemView
					
					val viewTag = "forumTagFix_plugin_indicator"
					var indicatorView = root.findViewWithTag(viewTag) as? TextView
					
					var availableTags = wrapper.availableTags
					
					val tagCount = availableTags.size
					
					var guideText = "\n**" + tagCount + "** tags found!";
						
					if (tagCount > 0) {
						guideText += "\nTag selector will display after click send button";
					}
						
					if (indicatorView != null && tagCount == lastTagCount) {
						return@Hook
					}
					
					if (indicatorView == null) {
						indicatorView = TextView(itemView.context, null, 0, R.i.UiKit_TextView_Subtext)
						indicatorView.tag = viewTag
						indicatorView.text = MDUtils.render(guideText)
						
						root.addView(indicatorView)
					} else {
						indicatorView.text = MDUtils.render(guideText)
					}
					
					lastTagCount = tagCount
					
				}
			} catch (e: Exception) {
				logger.error("Indicator", e)
			}
		})
	}
	
	override fun stop(context: Context) { patcher.unpatchAll() }
}
