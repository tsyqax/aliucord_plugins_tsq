package com.tsq.plugins

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.constraintlayout.widget.ConstraintLayout
import android.widget.GridLayout
import android.os.Bundle

import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.entities.Plugin
import com.aliucord.patcher.*
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.SettingsAPI
import com.aliucord.fragments.SettingsPage
import com.aliucord.views.TextInput
import com.aliucord.views.Button

import com.discord.models.message.Message
import com.discord.models.member.GuildMember

import com.discord.widgets.chat.list.adapter.WidgetChatListAdapter
import com.discord.widgets.chat.list.entries.ChatListEntry
import com.discord.widgets.chat.list.entries.AttachmentEntry
import com.discord.views.CheckedSetting

import com.discord.stores.StoreMessageState
import com.discord.stores.StoreStream

import com.discord.api.channel.Channel
import com.discord.api.message.attachment.MessageAttachment

import java.util.Collections


@AliucordPlugin
class MosaicFork: Plugin() {
	private val MOSAIC_VIEW_TYPE = 1234
	companion object {
        var realWidth = 0
        var screenWidth = 0
        var targetHeight = 0
        var paddingLeft = 0
        var targetHeightDP = 145
        var paddingLeftDP = 57
		var aniMode = false
		var lowGif = true
		var lowImage = true
		var autoGif = true
    }
	
	private val storeUserSettings = StoreStream.getUserSettings()
	
	init {
		settingsTab = SettingsTab(PSettings::class.java, SettingsTab.Type.PAGE).withArgs(settings)
	}
	
	// ----- settings start -----
	class PSettings(private val settings: SettingsAPI): SettingsPage() {
		
		override fun onViewCreated(view: View, bundle: Bundle?) {
			super.onViewCreated(view, bundle)
			setActionBarTitle("MosaicFork")
			setActionBarSubtitle("Settings!")
			
			val context = view.context
			val layout = getLinearLayout()

			val width_input = TextInput(context, "Width Ratio (0.0 ~ 1.0)", settings.getFloat("width", 0.83f).toString())
			val height_input = TextInput(context, "Height DP (default: 145)", settings.getInt("height", 145).toString())
			val padding_input = TextInput(context, "Padding DP (default: 57)", settings.getInt("padding", 57).toString())
			
			val auto_gif = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, "Toggle auto play Gif","").apply {
				setChecked(settings.getBool("autoGif", true))
				setOnCheckedListener({
					settings.setBool("autoGif", it)
				})
			}
			
			val ani_webp = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, "Use Animated Webp instead of Gif","").apply {
				setChecked(settings.getBool("ani_webp", false))
				setOnCheckedListener({
					settings.setBool("ani_webp", it)
				})
			}
			
			val low_gif = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, "Downgrade Gifs Preview Quality","").apply {
				setChecked(settings.getBool("lowGif", true))
				setOnCheckedListener({
					settings.setBool("lowGif", it)
				})
			}
			
			val low_image = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, "Downgrade Images Preview Quality","").apply {
				setChecked(settings.getBool("lowImage", false))
				setOnCheckedListener({
					settings.setBool("lowImage", it)
				})
			}
			
			var saveButton = Button(context)
			saveButton.setText("Save")
			saveButton.setOnClickListener({
				val widthVal = runCatching { width_input.editText.text.toString().toFloat() }.getOrDefault(0.83f)
				val heightVal = runCatching { height_input.editText.text.toString().toInt() }.getOrDefault(145)
				val paddingVal = runCatching { padding_input.editText.text.toString().toInt() }.getOrDefault(57)

				settings.setFloat("width", widthVal)
				settings.setInt("height", heightVal)
				settings.setInt("padding", paddingVal)

				Utils.promptRestart()
			})

			layout.apply {
				addView(width_input)
				addView(height_input)
				addView(padding_input)
				addView(auto_gif)
				addView(ani_webp)
				addView(low_gif)
				addView(low_image)
				addView(saveButton)
			}
		}
	}
	// ----- settings end -----

	override fun start(context: Context) {
		val dm = context.getResources().getDisplayMetrics()
		val density = dm.density
		screenWidth = dm.widthPixels 
		
		val inputWidth = settings.getFloat("width", 0.83f)
		targetHeightDP = settings.getInt("height", 145)
		paddingLeftDP = settings.getInt("padding", 57)
		
		realWidth = (screenWidth * inputWidth).toInt()
		targetHeight = (targetHeightDP * density + 0.5f).toInt()
		paddingLeft = (paddingLeftDP * density + 0.5f).toInt()
		
		val createEmbedEntriesMethod by lazy { ChatListEntry.Companion::class.java.getDeclaredMethod("createEmbedEntries", Message::class.java, StoreMessageState.State::class.java, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, Channel::class.java, GuildMember::class.java, Map::class.java, Map::class.java) }
		//  Message, StoreMessageState.State, boolean, boolean, boolean, boolean, boolean, Channel, GuildMember, Map, Map
		patcher.patch(createEmbedEntriesMethod, Hook { param ->
			@Suppress("UNCHECKED_CAST")
			val originalList = param.result as? MutableList<Any>
			val msg  = param.args[0] as Message
				
			if (!storeUserSettings.getIsAttachmentMediaInline() || originalList.isNullOrEmpty()) {
				return@Hook
			}
				
			if (originalList.size <= 1) return@Hook
				
			val images = mutableListOf<MessageAttachment>()
				
			// for (Object unKnown: originalList)
			var i = originalList.size - 1
			while (i >= 0) { //reverse
				val entry = originalList[i]
					
				if (entry is AttachmentEntry) {
					val attachment = entry.getAttachment() as MessageAttachment
					val fileType = attachment.e().ordinal
						
					if (fileType == 0 || fileType == 1) { 
						images.add(attachment)
						originalList.removeAt(i)
					}
				}
				
				i-- 
			}

			if (images.size > 1) {
				Collections.reverse(images) 
				originalList.add(0, MosaicEntry(images, msg))
			}

			param.result = originalList
		})
		
		val chatListAdapterMethod by lazy { WidgetChatListAdapter::class.java.getDeclaredMethod("onCreateViewHolder", ViewGroup::class.java, Int::class.javaPrimitiveType) }
		patcher.patch(chatListAdapterMethod, Hook { param ->
			val viewType = param.args[1] as Int
			val parent = param.args[0] as ViewGroup
			val adapter = param.thisObject as WidgetChatListAdapter
				
			if (viewType == MOSAIC_VIEW_TYPE) {
				val gridLayout = GridLayout(parent.context).apply {
					setLayoutParams(ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)) // before: MATCH_PARENT
					setPadding(paddingLeft, 0, 0, 0) 
				}
				
				val rootWrapper = ConstraintLayout(parent.context)
				rootWrapper.setLayoutParams(RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
				
				val params = ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
					horizontalBias = 0.0f
					constrainedWidth = true
					leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
					rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
				}
				
				rootWrapper.addView(gridLayout, params)
				
				settings.apply {
					aniMode = getBool("ani_webp", false)
					lowGif = getBool("lowGif", true)
					lowImage = getBool("lowImage", false)
					autoGif = getBool("autoGif", true)
				}
				
				//MosaicViewHolder mosaicViewHolder = new MosaicViewHolder(gridLayout, adapter)
				val mosaicViewHolder = MosaicViewHolder(rootWrapper, gridLayout, adapter)
    
				param.result = mosaicViewHolder
			}
		})
	}
	
	override fun stop(context: Context) {
		patcher.unpatchAll()
	}

}
