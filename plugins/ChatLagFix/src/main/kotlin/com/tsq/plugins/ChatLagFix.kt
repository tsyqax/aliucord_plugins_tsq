package com.tsq.plugins

import android.content.Context
import android.os.Looper
import android.os.Handler
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView

import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.*
import com.aliucord.Logger
import com.aliucord.api.SettingsAPI
import com.aliucord.fragments.SettingsPage
import com.aliucord.views.TextInput
import com.aliucord.views.Button

import com.discord.views.CheckedSetting
import com.discord.widgets.chat.input.WidgetChatInputEditText

import com.lytefast.flexinput.widget.FlexEditText

@AliucordPlugin
public class ChatLagFix: Plugin() {

	init { 
		settingsTab = SettingsTab(PSettings::class.java, SettingsTab.Type.PAGE).withArgs(settings)
	}
	
	// ----- settings start -----
	class PSettings(private val settings: SettingsAPI) : SettingsPage() {

		override fun onViewCreated(view: View, bundle: Bundle?) {
			super.onViewCreated(view, bundle)
			setActionBarTitle("ChatLagFix")
			setActionBarSubtitle("Settings!")
			
			var context = view.context
			var layout = getLinearLayout()

			var chat = TextInput(context, "Chat Length to working", settings.getInt("chat", 1000).toString())
			var delay = TextInput(context, "Listener Delay (default: 500)", settings.getInt("delay", 500).toString())

			val power = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, "Only Fast mode","").apply {
				setChecked(settings.getBool("power", false))
				setOnCheckedListener({
					settings.setBool("power", it)
					if (it) {
						Utils.showToast("WARN: Mention and others may not be working!")
						Utils.showToast("WARN: Some text can be diminished!")
					}
					Utils.promptRestart()
				})
			}
			
			val saveButton = Button(context).apply {
				setText("Save")
				setOnClickListener({
					val chatVal = runCatching { chat.editText.text.toString().toInt() }.getOrDefault(1000)
					val delayVal = runCatching { delay.editText.text.toString().toInt() }.getOrDefault(500)

					settings.setInt("delay", delayVal)
					settings.setInt("chat", chatVal)

					Utils.promptRestart()
				})
			}

			layout.apply {
				addView(delay)
				addView(chat)
				addView(power)
				addView(saveButton)
			}
		}
	}
	// ----- settings end -----
	
	
	// Thanks for someone(Anonymous) to help me
	override fun start(context: Context) {
		val delay = settings.getInt("delay", 500)
		val chat = settings.getInt("chat", 500)
		val power = settings.getBool("power", false)
		
		try {
			val addListenerMethod by lazy { TextView::class.java.getDeclaredMethod("addTextChangedListener", TextWatcher::class.java) }

			patcher.patch(addListenerMethod, PreHook { param ->
				if (param.thisObject is FlexEditText) {

					if (param.args[0] is TextWatcher) {
						val original = param.args[0] as TextWatcher

						// new watcher
						param.args[0] = object : TextWatcher {
							val handler = Handler(Looper.getMainLooper())
							var pendingRunnable: Runnable? = null
							var stoped = false
							
							override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
								if (s == null || s.length < chat) {
									original.beforeTextChanged(s, start, count, after)
									return
								}

								if (!stoped) original.beforeTextChanged(s, start, count, after)
							}

							override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
								if (s == null || s.length < chat) {
									original.onTextChanged(s, start, before, count)
									return
								}
								
								if (!stoped) original.onTextChanged(s, start, before, count)
							}

							override fun afterTextChanged(s: Editable?) {
								if (s == null) return
								val textLength = s.length
								
								if (textLength == 0 && stoped) {
									stoped = false
									if (pendingRunnable != null) {
										handler.removeCallbacks(pendingRunnable!!)
										pendingRunnable = null
									}
									original.afterTextChanged(s)
									return
								}

								if (textLength < chat) {
									original.afterTextChanged(s)
									return
								}

								stoped = true
								if (pendingRunnable != null) handler.removeCallbacks(pendingRunnable!!)

								pendingRunnable = object : Runnable {
									override fun run() {
										stoped = false
										original.beforeTextChanged(s, 0, textLength, textLength)
										original.onTextChanged(s, 0, 0, textLength)
										original.afterTextChanged(s)
									}
								}
								
								if (!power) 	handler.postDelayed(pendingRunnable!!, delay.toLong())
							}
						}
					}
				}
			})
		} catch (e: NoSuchMethodException) {
			logger.error("addTextChangedListener", e)
		}

		try {
			val getTextMethod by lazy { WidgetChatInputEditText::class.java.getDeclaredMethod("getText") }

			patcher.patch(getTextMethod, PreHook { param ->
				val widgetInstance = param.thisObject as WidgetChatInputEditText
				
				try {
					val editTextField = WidgetChatInputEditText::class.java.getDeclaredField("editText")
					editTextField.isAccessible = true
					val flexEditText = editTextField.get(widgetInstance) as? FlexEditText

					if (flexEditText != null && flexEditText.text != null) {
						val originalText = flexEditText.text as CharSequence
						
						if (originalText.length >= chat) {
							val finalRawText = originalText.toString()
							param.result = finalRawText
							return@PreHook
						}
					}
				} catch (e: Exception) {
					logger.error("gt-patch", e)
				}
			})
		} catch (e: Throwable) {
			logger.error("gt-outer", e)
		}

	}

	override fun stop(context: Context) {
		patcher.unpatchAll()
	}
}
