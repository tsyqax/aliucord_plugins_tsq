package com.tsq.plugins

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat

import com.aliucord.Http
import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.CommandsAPI
import com.aliucord.api.SettingsAPI
import com.aliucord.entities.Plugin
import com.aliucord.fragments.SettingsPage
import com.aliucord.patcher.*
import com.aliucord.views.Button
import com.aliucord.views.TextInput

import com.discord.databinding.WidgetGuildContextMenuBinding
import com.discord.models.domain.ModelInvite
import com.discord.restapi.RestAPIParams
import com.discord.stores.StoreStream
import com.discord.utilities.rest.RestAPI
import com.discord.views.CheckedSetting
import com.discord.widgets.guilds.contextmenu.GuildContextMenuViewModel
import com.discord.widgets.guilds.contextmenu.WidgetGuildContextMenu
import com.discord.widgets.guilds.profile.WidgetGuildProfileSheet
import com.discord.widgets.guilds.profile.WidgetGuildProfileSheetViewModel

import com.lytefast.flexinput.R
import com.lytefast.flexinput.model.Attachment

import kotlin.concurrent.thread
import java.util.concurrent.CancellationException

import rx.Observable
import org.json.JSONArray
import org.json.JSONObject

import java.util.Properties
import java.io.StringReader


@AliucordPlugin
class FixOnboardingFork: Plugin() {
	val selectedResponses = mutableListOf<String>()
	val promptsSeen = linkedMapOf<String, Long>()
	val responsesSeen = linkedMapOf<String, Long>()
	internal var userSelection: Int = 0
	internal var userRealSelection: String? = null
	// internal var resText: String? = null
	internal var multiSelectionFlags: BooleanArray? = null

	@Volatile private var openingChannelsAndRoles = false
	
	init { 
		settingsTab = SettingsTab(PSettings::class.java, SettingsTab.Type.PAGE).withArgs(settings)
	}
	
	// ----- settings start -----
	class PSettings(private val settings: SettingsAPI): SettingsPage() {
		override fun onViewCreated(view: View, bundle: Bundle?) {
			super.onViewCreated(view, bundle)
			setActionBarTitle("FixOnboardingFork")
			setActionBarSubtitle("Settings!")

			var context = view.context
			var layout = getLinearLayout()
			
			/* var color_guide = TextView(context, null, 0, R.i.UiKit_TextView).apply {
				text = "The color must be specified as a Hex code (e.g., #ffffff). Case sensitivity does not matter, and the default value is 'auto'. If an invalid color value is provided or 'auto' is selected, the color will be synchronized with the current theme."
			} */

			val auto = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, "Auto Onboarding","Detects joining server and automatically launches the onboarding.").apply {
				setChecked(settings.getBool("auto", true))
				setOnCheckedListener({
					settings.setBool("auto", it)
				})
			}
			
			val oldPage = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, "Old Style","Turn on if you want to do onboarding with old page style.").apply {
				setChecked(settings.getBool("old", false))
				setOnCheckedListener({
					settings.setBool("old", it)
				})
			}
			
			val chChList = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, "Channels & Roles in ChannelList","Turn on when to view button for Channels & Roles in Channels List").apply {
				setChecked(settings.getBool("chlist", false))
				setOnCheckedListener({
					settings.setBool("chlist", it)
				})
			}

			layout.addView(auto)
			layout.addView(oldPage)
			//layout.addView(chChList)
		}
	}
	// ----- settings end -----

	override fun start(context: Context) {
		val viewId1 = View.generateViewId()
		val mailIcon = ContextCompat.getDrawable(Utils.appActivity, R.e.ic_mail_24dp)?.mutate()
        Utils.tintToTheme(mailIcon)
		
		// Refered from BetterSpolier by Ushie
		val getServerBindingMethod by lazy { WidgetGuildContextMenu::class.java.getDeclaredMethod("getBinding").apply { isAccessible = true } }
		val gcmvm_cfg = WidgetGuildContextMenu::class.java.getDeclaredMethod("configureUI", GuildContextMenuViewModel.ViewState::class.java)

		patcher.patch(gcmvm_cfg, PreHook { param ->
			try {
				val validState = param.args[0] as GuildContextMenuViewModel.ViewState.Valid
				val binding = getServerBindingMethod.invoke(param.thisObject) as WidgetGuildContextMenuBinding
				val lay = binding.e.getParent() as LinearLayout

				lay.removeView(lay.findViewById(viewId1))
				if (lay.findViewById<View>(viewId1) == null) {
					val tw = TextView(lay.context, null, 0, R.i.ContextMenuTextOption).apply {
						id = viewId1
						text = "Onboarding"
						setCompoundDrawablesRelativeWithIntrinsicBounds(mailIcon, null, null, null)
					}
		
					lay.addView(tw)
					
					tw.setOnClickListener { v ->
						val ctx = v.context as Context
						lay.visibility = View.GONE
						
						this.selectedResponses.clear()
						this.promptsSeen.clear()
						this.responsesSeen.clear()
							
						val guildId = validState.getGuild().getId().toString()
						val userId = StoreStream.getUsers().getMe().getId().toString()
							
						thread {
							startOnboarding(getSafeActivity(ctx), guildId, userId)
						}
					}
				}
			} catch (e: ClassCastException) {
			} catch (e: Exception) {
				logger.error("gcmvm_cfg", e)
			}
        })

		try {
			patcher.patch(
				WidgetGuildProfileSheet::class.java,
				"configureUI",
				arrayOf(WidgetGuildProfileSheetViewModel.ViewState.Loaded::class.java),
				Hook { param ->
					try {
						val sheet = param.thisObject as WidgetGuildProfileSheet
						val state = param.args[0] as WidgetGuildProfileSheetViewModel.ViewState.Loaded
						ensureChannelsAndRolesButton(sheet, state.guildId)
					} catch (e: Exception) {
						logger.error("profileSheetButton", e)
					}
				}
			)
		} catch (e: Exception) {
			logger.error("profileSheetPatch", e)
		}
		
		commands.registerCommand(
            "onboarding",
            "Display Onboarding",
            { ctx ->
				try {
					this.selectedResponses.clear()
					this.promptsSeen.clear()
					this.responsesSeen.clear()
					
					val guildId = StoreStream.getGuildSelected().getSelectedGuildId().toString()
					val userId = ctx.getMe().getId().toString()
					thread {
						startOnboarding(getSafeActivity(ctx.context), guildId, userId)
					}
					
					//showChainDialog(context, questions, 0, guildId, userId) // start question from idx 0
					
					CommandsAPI.CommandResult("Fetching onboarding details...", null, false)
                } catch (t: Throwable) {
					logger.error("CMD", t)
                    CommandsAPI.CommandResult("Error: '" + t.message , null, false)
                }
            }
        )
		
		commands.registerCommand(
            "onboardingBrowser",
            "Display Onboarding & Browse screen",
            { ctx ->
				try {
					this.selectedResponses.clear()
					this.promptsSeen.clear()
					this.responsesSeen.clear()
					
					val guildId = StoreStream.getGuildSelected().getSelectedGuildId().toString()
					val userId = ctx.getMe().getId().toString()
					thread {
						startChannelsAndRoles(getSafeActivity(ctx.context), guildId, userId)
					}
					
					//showChainDialog(context, questions, 0, guildId, userId) // start question from idx 0
					
					CommandsAPI.CommandResult("Displayed!", null, false)
                } catch (t: Throwable) {
					logger.error("CMD", t)
                    CommandsAPI.CommandResult("Error: '" + t.message , null, false)
                }
            }
        )
	
		val autoMode = settings.getBool("auto", true)
		
		if (autoMode) {
			try {
				val realJoinMethod = RestAPI::class.java.getDeclaredMethod("postInviteCode", ModelInvite::class.java, String::class.java, RestAPIParams.InviteCode::class.java)
				patcher.patch(realJoinMethod, Hook { param ->
					val modelinvite = param.args[0] as ModelInvite
					
					this.selectedResponses.clear()
					this.promptsSeen.clear()
					this.responsesSeen.clear()
					
					@Suppress("UNCHECKED_CAST")
					val responseObservable = param.result as  Observable<ModelInvite>
					
					param.result = responseObservable.u { freshInvite ->
						if (freshInvite != null && freshInvite.guild != null) {
							val realGuildId = freshInvite.guild.r().toString()
							val userId = StoreStream.getUsers().getMe().getId().toString()

							startOnboarding(Utils.appActivity, realGuildId, userId)
						}
					}
				})
			} catch (e: Exception) {
				logger.error("authoMode", e)
			}
		}
	}

	private fun ensureChannelsAndRolesButton(sheet: WidgetGuildProfileSheet, guildId: Long) {
		val root = sheet.view ?: return
		val cardId = Utils.getResId("guild_profile_sheet_secondary_actions", "id")
		val card = if (cardId != 0) root.findViewById<CardView>(cardId) else null
		val container = card?.getChildAt(0) as? LinearLayout ?: return

		val row = container.findViewWithTag<TextView>("fof_car_button") ?: run {
			val ctx = container.context
			var styleId = Utils.getResId("GuildProfileSheet_Actions_Title", "style")
			if (styleId == 0) styleId = Utils.getResId("GuildProfileSheet.Actions.Title", "style")
			val fontSourceId = Utils.getResId("change_identity_header", "id")
			val fontSource = if (fontSourceId != 0) root.findViewById<TextView>(fontSourceId) else null
			val pad = (16 * ctx.resources.displayMetrics.density + 0.5f).toInt()

			val tv = TextView(ctx, null, 0, styleId).apply {
				tag = "fof_car_button"
				text = "Onboarding & Browse"
				if (styleId == 0) textSize = 16f
				fontSource?.let { source ->
					typeface = source.typeface
					setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, source.textSize)
					setTextColor(source.currentTextColor)
					letterSpacing = source.letterSpacing
				}
				setPadding(pad, pad, pad, pad)
				isClickable = true
				isFocusable = true
			}
			container.addView(tv, 0, LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			))
			tv
		}
		row.visibility = View.VISIBLE

		row.setOnClickListener {
			val activity = sheet.activity ?: return@setOnClickListener
			sheet.dismiss()
			val userId = StoreStream.getUsers().getMe().getId().toString()
			thread {
				startChannelsAndRoles(activity, guildId.toString(), userId)
			}
		}
	}
	
	fun startOnboarding(activity: Activity, guildId: String, userId: String) {
		try {
			val resText = Http.Request.newDiscordRequest("/guilds/$guildId/onboarding", "GET").execute().text()
			val questions = parseQuestions(resText) // List<JSONObject>
			//var pending = parsePending(resText)
			var pending = false

			if (questions.isEmpty()) {
				Utils.showToast("There are no Onboarding.", false)
				return
			}
			
			Handler(Looper.getMainLooper()).post {
				showChainDialog(activity, questions, 0, guildId, userId)
			}
			
		} catch (e: Exception) {
			logger.error("Error", e)
			val err = e.message ?: "Unknown Error"
			val msg = runCatching {
				if (err.contains("\"message\": \"")) {
					val rawMsg = err.split("\"message\": \"")[1].split("\"")[0]
					Properties().apply { load(StringReader("m=$rawMsg")) }.getProperty("m")
				} else err
			}.getOrDefault(err)
			
			Utils.showToast(msg, false)
		}
	}
	
	fun startChannelsAndRoles(activity: Activity, guildId: String, userId: String) {
		if (openingChannelsAndRoles) return
		openingChannelsAndRoles = true
        try {
			var questions: MutableList<JSONObject> = mutableListOf()
			val defaultChannelIds = mutableSetOf<String>()
			runCatching {
				val resText = Http.Request.newDiscordRequest("/guilds/$guildId/onboarding", "GET").execute().text()
				questions = parseQuestions(resText)
				val defaults = JSONObject(resText).optJSONArray("default_channel_ids")
				if (defaults != null) {
					for (i in 0 until defaults.length()) defaultChannelIds.add(defaults.optString(i))
				}
			}.onFailure { logger.error("onboardingFetch", it) }

            val gId = guildId.toLongOrNull() ?: 0L
            val channelsResponseText = Http.Request.newDiscordRequest("/guilds/$guildId/channels", "GET").execute().text()
            val responseArray = JSONArray(channelsResponseText)
            
            val channels = mutableListOf<JSONObject>()
            val localPermissions = runCatching { StoreStream.getPermissions().permissionsByChannel }.getOrNull()
			val guildSettings = runCatching { StoreStream.getUserGuildSettings().guildSettings[gId] }.getOrNull()

            for (i in 0 until responseArray.length()) {
                val channel = responseArray.getJSONObject(i)
                val id = channel.optString("id").toLongOrNull() ?: continue
                val type = channel.optInt("type", -1)

                if (type !in setOf(0, 2, 5, 13, 15, 16) && type != 4) continue

				val localPerm = localPermissions?.get(id)
				if (localPerm != null && (localPerm and (1L shl 10)) == 0L) continue

				val flags = runCatching { guildSettings?.getChannelOverride(id)?.flags }.getOrNull() ?: 0

                channel.put("parent_id_long", channel.optString("parent_id").toLongOrNull() ?: 0L)
                channel.put("position_int", channel.optInt("position", 0))
				channel.put("is_opted_in", (flags and 4096) != 0)
				channel.put("is_default", id.toString() in defaultChannelIds)
                
                channels.add(channel)
            }

            channels.sortWith(compareBy(
                { it.optInt("position_int", 0) },
                { it.optString("name", "") }
            ))

            Handler(Looper.getMainLooper()).post {
                showChannelsAndRoles(activity, questions, guildId, userId, channels)
            }
            
        } catch (e: Exception) {
            logger.error("Error", e)
            Utils.showToast(parseErrorMessage(e), false)
        } finally {
			openingChannelsAndRoles = false
		}
    }

	internal fun showChannelsAndRoles(context: Context, questions: MutableList<JSONObject>, guildId: String, userId: String, channels: MutableList<JSONObject>) {
		try {
			Utils.openPageWithProxy(context, ChannelsAndRoles(this, questions, guildId, userId, channels))
		} catch (e: Exception) {
			logger.error("showChannelsAndRoles", e)
			Utils.showToast(e.message ?: "Unknown Error", false)
		}
	}

	private fun parseErrorMessage(e: Throwable): String {
		val err = e.message ?: "Unknown Error"
		return runCatching {
			if (err.contains("\"message\": \"")) {
				val rawMsg = err.split("\"message\": \"")[1].split("\"")[0]
				Properties().apply { load(StringReader("m=$rawMsg")) }.getProperty("m")
			} else err
		}.getOrDefault(err)
	}

	//val themeColor = ContextCompat.getColor(context, R.i.UiKit_Settings_Text)
	
	// refered from FixOnboarding by @scourage_main
	private fun getSafeActivity(context: Context): Activity {
		var currentCtx: Context? = context

		while (currentCtx is ContextWrapper) {
			if (currentCtx is Activity && !currentCtx.isFinishing && !currentCtx.isDestroyed) {
				return currentCtx
			}
			currentCtx = currentCtx.baseContext
		}

		return Utils.appActivity
	}
	
	fun parseQuestions(jsonString: String): MutableList<JSONObject> {
		val questionList = mutableListOf<JSONObject>()
		try {
			val root = JSONObject(jsonString)
			val prompts = root.optJSONArray("prompts") as JSONArray

			for (i in 0 until prompts.length()) {
				questionList.add(prompts.getJSONObject(i))
			}
		} catch (e: Exception) {}
		return questionList
	}
	
	fun parsePending(jsonString: String): Boolean {
		try {
			val root = JSONObject(jsonString)
			val prompts = root.optJSONArray("responses") as JSONArray
			logger.info(jsonString)
			logger.info(prompts.toString())
			
			if (prompts.length() == 0) {
				return true
			}
			return false
		} catch (e: Exception) {
			return false
		}
	}
	
	fun addAnswer(promptId: String, allOptionIdsOfPrompt: MutableList<String>, chosenOptionId: String) {
		selectedResponses.removeAll(allOptionIdsOfPrompt) // for back
		selectedResponses.add(chosenOptionId) //answer
    }
	
	fun addSeenTime(promptId: String, allOptionIdsOfPrompt: MutableList<String>) {
        val currentTime = System.currentTimeMillis() //seenTime
        promptsSeen.put(promptId, currentTime) 

        for (optionId in allOptionIdsOfPrompt) {
            responsesSeen[optionId] = currentTime // all seenTime for correct
        }
    }
	
	fun buildPayloadLast(guildId: String, userId: String): JSONObject? {
		val payload = JSONObject()
		try {
			//payload.put("guild_id", guildId)
			//payload.put("user_id", userId)

			val responsesArray = JSONArray().apply {
				selectedResponses.forEach { id -> put(id) }
			}
			payload.put("onboarding_responses", responsesArray)

			val promptsSeenJson = JSONObject().apply {
				promptsSeen.forEach { (key, value) -> put(key, value) }
			}
			payload.put("onboarding_prompts_seen", promptsSeenJson)

			val responsesSeenJson = JSONObject().apply {
				responsesSeen.forEach { (key, value) -> put(key, value) }
			}
			payload.put("onboarding_responses_seen", responsesSeenJson)
			
		} catch (e: Exception) {
			logger.error("buildPLL", e)
			return null
		}
		return payload
	}

	private fun sendOnboardingResponses(guildId: String, method: String, body: String) {
		val req = Http.Request.newDiscordRequest("/guilds/$guildId/onboarding-responses", method)
		req.setHeader("Content-Type", "application/json")
		val res = req.executeWithBody(body)
		res.assertOk()
	}
	
	internal fun onboardingRequest(questions: MutableList<JSONObject>, guildId: String, userId: String, onResult: ((Boolean) -> Unit)? = null) {
		val finalPayload = buildPayloadLast(guildId, userId)
			
		if (finalPayload == null) {
			Utils.showToast("Error, you maybe will report it on Github?")
			Utils.mainThread.post { onResult?.invoke(false) }
			return
		}
			
		logger.info(finalPayload.toString())
		val body = finalPayload.toString()
			
		thread {
			var firstError: Throwable? = null
			var success = false

			for (method in listOf("PUT", "POST")) {
				try {
					sendOnboardingResponses(guildId, method, body)
					success = true
					break
				} catch (e: Throwable) {
					logger.error("${method}_ERR", e)
					if (firstError == null) firstError = e
				}
			}

			if (success) {
				Utils.showToast("Done!", false)
			} else {
				Utils.showToast(parseErrorMessage(firstError ?: Exception("Unknown Error")), false)
			}
			Utils.mainThread.post { onResult?.invoke(success) }
		}
	}

	internal fun showChainDialog(context: Context, questions: MutableList<JSONObject>, index: Int, guildId: String, userId: String) {
		if (index >= questions.size) { // Questions Ended
			onboardingRequest(questions, guildId, userId)
			return
		}

		try {
			val currentPrompt = questions.get(index)
			val promptId = currentPrompt.getString("id") //question ID
			var allTitle = currentPrompt.getString("title") // questions Title (Unicode Escape Sequence)
			val required = currentPrompt.optBoolean("required", false) // questions required
			val onlyOne = currentPrompt.optBoolean("single_select", false) // answer is onlyOne?
			val options = currentPrompt.getJSONArray("options") //options -> id, title, description(optional)
			
			val optionTitles = Array<String?>(options.length()) { null }
			val allOptionIds = mutableListOf<String>()

			for (i in 0 until options.length()) {
				val opt = options.getJSONObject(i)
				optionTitles[i] = opt.getString("title")
				allOptionIds.add(opt.getString("id"))
			}
			
			userSelection = -1 // initialize
			// userRealSelection = allOptionIds[-1] // initialize
			
			try {
				val oldMode = settings.getBool("old", true)
				if (oldMode) {
					throw CancellationException("oldMode")
				}
				
				val onpage = OnboardingPage(this, questions, promptId, allTitle, required, onlyOne, options, guildId, userId, allOptionIds, index)
				Utils.openPageWithProxy(context, onpage)
			} catch (e: Exception) {
				var dialogTitle = "[${index + 1}/${questions.size}]"

				if (required) {
					dialogTitle += " · Required"
					allTitle += " *"
				}
				if (!onlyOne) {
					dialogTitle += " · Multiable"
				}
				
				val onpage2 = OnboardingPage_Old(this, questions, promptId, allTitle, dialogTitle, required, onlyOne, options, guildId, userId, allOptionIds, index)
				Utils.openPageWithProxy(context, onpage2)
			}

		} catch (e: Exception) {
			Utils.showToast(e.message ?: "Unknown Error", false)
		}
	}

	override fun stop(context: Context) { patcher.unpatchAll() }
}
