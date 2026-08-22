package com.tsq.plugins

import android.content.Context
import android.os.Bundle
import android.view.View

import com.discord.views.CheckedSetting

import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.SettingsAPI
import com.aliucord.entities.Plugin
import com.aliucord.fragments.SettingsPage
import com.aliucord.patcher.*;
import com.aliucord.settings.*;
import com.aliucord.api.PatcherAPI
import com.aliucord.views.Button
import com.aliucord.views.TextInput

@AliucordPlugin(requiresRestart = true)
class UItweaks: Plugin() {
	
	init {
        settingsTab = SettingsTab(PSettings::class.java, SettingsTab.Type.PAGE).withArgs(settings)
    }
	
	// ----- settings start -----
	class PSettings(private val settings: SettingsAPI) : SettingsPage() {
		
		override fun onViewCreated(view: View, bundle: Bundle?) {
			super.onViewCreated(view, bundle)
			setActionBarTitle("UItweaks")
			setActionBarSubtitle("Settings!")
			
			var context = view.context
			var layout = getLinearLayout()
			
			val th_del = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, "Add button to delete thread/channel on list menu","")
			th_del.setChecked(settings.getBool("ThreadDEL", true))
			th_del.setOnCheckedListener({
				settings.setBool("threadDEL", it)
				Utils.promptRestart()
			})
			
			val forum_line = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, "Add line to first forum message","")
			forum_line.setChecked(settings.getBool("ForumLine", true))
			forum_line.setOnCheckedListener({
				settings.setBool("forumLine", it)
				Utils.promptRestart()
			})
			
			val rule_ch_icon = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, "Make rules channel icon","")
			rule_ch_icon.setChecked(settings.getBool("RuleChIcon", true))
			rule_ch_icon.setOnCheckedListener({
				settings.setBool("ruleChIcon", it)
				Utils.promptRestart()
			})
			
			val plural_fix = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, "Fix plurals when no distinct","")
			plural_fix.setChecked(settings.getBool("PluralFix", true))
			plural_fix.setOnCheckedListener({
				settings.setBool("PluralFix", it)
				Utils.promptRestart()
			})
			
			val profile_deco = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, "Add deco on profile (beta)","")
			plural_fix.setChecked(settings.getBool("ProfileDeco", true))
			plural_fix.setOnCheckedListener({
				settings.setBool("ProfileDeco", it)
				Utils.promptRestart()
			})
		
			val threadInput = TextInput(context, "Thread Delete Label", settings.getString("thread_label", "Delete Thread"))
			var channelInput = TextInput(context, "Channel Delete Label", settings.getString("channel_label", "Delete Channel"))
			var saveButton = Button(context)
			saveButton.text = "Save Settings"
			
			saveButton.setOnClickListener({
				val threadVal = runCatching { threadInput.editText.text?.toString() }.getOrDefault("Delete Thread")
				val channelVal = runCatching { channelInput.editText.text?.toString() }.getOrDefault("Delete Channel")
				
				settings.setString("thread_label", threadVal)
				settings.setString("channel_label", channelVal)
				
				Utils.promptRestart()
			})
			
			layout.addView(th_del)
			layout.addView(forum_line)
			layout.addView(rule_ch_icon)
			layout.addView(plural_fix)
            layout.addView(profile_deco)
			
			if (settings.getBool("threadDEL", true)) {
				layout.addView(threadInput)
				layout.addView(channelInput)
				layout.addView(saveButton)
			}
		}
	}
	// ----- settings end -----
	
	override fun start(context: Context) {
		val th_del_bool = settings.getBool("ThreadDEL", true)
		val forum_line_bool = settings.getBool("ForumLine", true)
		val rule_ch_icon_bool = settings.getBool("RuleChIcon", true)
		val plural_fix_bool = settings.getBool("PluralFix", true)
		val profile_deco = settings.getBool("ProfileDeco", true)
		
		if (th_del_bool) ThreadDEL.init(context, patcher, logger, settings)
		if (forum_line_bool) ForumLine.init(context, patcher, logger)
		if (rule_ch_icon_bool) RuleChIcon.init(context, patcher, logger, resources)
		if (plural_fix_bool) PluralFix.init(context, patcher, logger)
		if (profile_deco) ProfileDeco.init(context, patcher, logger)
	}
	
    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
