package com.tsq.plugins

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
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

import com.discord.databinding.WidgetGuildContextMenuBinding
import com.discord.models.domain.ModelInvite
import com.discord.restapi.RestAPIParams
import com.discord.stores.StoreStream
import com.discord.utilities.rest.RestAPI
import com.discord.views.CheckedSetting
import com.discord.widgets.guilds.contextmenu.GuildContextMenuViewModel
import com.discord.widgets.guilds.contextmenu.WidgetGuildContextMenu

import com.lytefast.flexinput.R
import com.lytefast.flexinput.model.Attachment

import kotlin.concurrent.thread

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

			val auto = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, "Auto Onboarding","").apply {
				setChecked(settings.getBool("auto", true))
				setOnCheckedListener({
					settings.setBool("auto", it)
				})
			}

			layout.addView(auto)
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
				showChainDialog(activity, questions, 0, guildId, userId, pending)
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

	internal fun showChainDialog(context: Context, questions: MutableList<JSONObject>, index: Int, guildId: String, userId: String, pending: Boolean) {
		if (index >= questions.size) { // Questions Ended
			val finalPayload = buildPayloadLast(guildId, userId)
			
			if (finalPayload == null) {
				Utils.showToast("Error, you maybe will report it on Github?")
				return
			}
			
			logger.info(finalPayload.toString())
			
			thread {
				thread {
					val postResult = runCatching {
						val req = Http.Request.newDiscordRequest("/guilds/$guildId/onboarding-responses", "POST")
						req.setHeader("Content-Type", "application/json")
						req.executeWithBody(finalPayload.toString()).text()
					}
					
					if (postResult.isSuccess) {
						Utils.showToast("Done!", false)
						return@thread
					}
					
					val e = postResult.exceptionOrNull() ?: Exception("Unknown POST Error")
					val putResult = runCatching {
						val req = Http.Request.newDiscordRequest("/guilds/$guildId/onboarding-responses", "PUT").apply {
							setHeader("Content-Type", "application/json")
							executeWithBody(finalPayload.toString()).text()
						}
					}
					
					if (putResult.isSuccess) {
						Utils.showToast("Done!", false)
						return@thread
					}
					
					val ig = putResult.exceptionOrNull() ?: Exception("Unknown PUT Error")
					logger.error("PUT_ERR", ig)
					logger.error("ERR", e)
					
					val err = e.message ?: "Unknown Error"
					
					val msg = runCatching {
						if (err.contains("\"message\": \"")) {
							val rawMsg = err.split("\"message\": \"")[1].split("\"")[0]
							java.util.Properties().apply { load(java.io.StringReader("m=$rawMsg")) }.getProperty("m")
						} else {
							err
						}
					}.getOrDefault(err)
					
					Utils.showToast(msg, false)
				}
			}
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
				val onpage = OnboardingPage(this, questions, promptId, allTitle, required, onlyOne, options, guildId, userId, allOptionIds, index, pending)
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
				
				val onpage2 = OnboardingPage_Old(this, questions, promptId, allTitle, dialogTitle, required, onlyOne, options, guildId, userId, allOptionIds, index, pending)
				Utils.openPageWithProxy(context, onpage2)
			}

		} catch (e: Exception) {
			Utils.showToast(e.message ?: "Unknown Error", false)
		}
	}
	
	override fun stop(context: Context) { patcher.unpatchAll() }
}
