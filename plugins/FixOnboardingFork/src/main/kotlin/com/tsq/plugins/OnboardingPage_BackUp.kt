package com.tsq.plugins

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView

import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.fragments.SettingsPage
import com.aliucord.patcher.*
import com.aliucord.views.Button
import com.aliucord.views.DangerButton

import org.json.JSONArray
import org.json.JSONObject

import kotlin.concurrent.thread
import com.lytefast.flexinput.R

import java.net.URL
import java.net.HttpURLConnection

class OnboardingPage_Old(
	private val fof: FixOnboardingFork, 
	private val questions: MutableList<JSONObject>,
	private val promptId: String,          // question ID
	private val allTitle: String,          // questions Title (Unicode Escape Sequence)
	private val dialogTitle: String,       // subtitle
	private val required: Boolean,         // questions required
	private val onlyOne: Boolean,          // answer is onlyOne?
	private val options: JSONArray,        // options -> id, title, description(optional)
	private val guildId: String,
	private val userId: String,
	private val allOptionIds: MutableList<String>,
	private val idx: Int,
	private val pending: Boolean
) : SettingsPage() {
	
	private fun closePage() {
		Utils.mainThread.post {
			val fragmentManager = fragmentManager
			if (fragmentManager != null) {
				try { fragmentManager.popBackStackImmediate() } catch (ignored: Exception) {}
			}
				
			val activity = activity
			if (activity != null && !activity.isFinishing()) {
				activity.finish() 
			}
		}
	}
		
	private fun dpToPx(context: Context, dp: Int): Int {
		val density = context.resources.displayMetrics.density 
		return Math.round(dp.toFloat() * density)
	}
		
	private fun adpatCustomEmoji(context: Context, emojiId: String, urlStr: String, view: TextView) {
		thread {
			try {
				val url = URL(urlStr)
				val connection = url.openConnection() as HttpURLConnection
				connection.doInput  = true
				connection.connect()
				
				val input = connection.inputStream
				val bitmap = BitmapFactory.decodeStream(input)
				
				if (bitmap != null) {
					Handler(Looper.getMainLooper()).post {
						chapCustomEmoji(context, bitmap, view)
					}
				}
			} catch (e: Exception) {}
		}
	}
		
	private fun chapCustomEmoji(context: Context, bitmap: Bitmap, view: TextView) {
		val emojiSize = dpToPx(context, 24)
		val scaled = Bitmap.createScaledBitmap(bitmap, emojiSize, emojiSize, true)
		val drawable = BitmapDrawable(context.getResources(), scaled)
		
		view.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
		view.setCompoundDrawablePadding(dpToPx(context, 8))
	}
		
	override fun onViewCreated(view: View, bundle: Bundle?) {
		super.onViewCreated(view, bundle)
		setActionBarTitle(dialogTitle)
		setActionBarSubtitle(guildId)

		var context = view.context
		var layout = getLinearLayout()
		
		val title = TextView(context, null, 0, R.i.UiKit_Settings_Text)
		title.setText(allTitle)
		title.setTypeface(null, Typeface.BOLD)
		
		val defaultTitleSize = title.textSize
		title.setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTitleSize * 1.18f)
		
		title.setPadding(0, 0, 0, dpToPx(context, 16)) 
		layout.addView(title)
		
		if (!onlyOne) fof.multiSelectionFlags = BooleanArray(options.length())
		
		var radioGroup: RadioGroup? = null
		
		if (onlyOne) {
			radioGroup = RadioGroup(context)
			radioGroup.setOrientation(LinearLayout.VERTICAL)
		}
			
		try {
			val itemParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
			itemParams.setMargins(0, 0, 0, dpToPx(context, 8))
			
			for (i in 0 until options.length()) {
				val opt = options.getJSONObject(i)
				var optionTitle = opt.optString("title")
				val id = opt.optString("id")
				val emojiObj = opt.optJSONObject("emoji")
				val emojiName = emojiObj?.optString("name") ?: ""
				var emojiUrl = ""
				val emojiId = emojiObj?.optString("id") ?: ""
				
				if (!emojiObj.isNull("name")) {
					if (emojiObj.isNull("id")) {
						optionTitle = emojiName + " " + optionTitle
					} else {
						val isAnimated = emojiObj.optBoolean("animated", false)
						val ext = if (isAnimated) "gif" else "png"
						emojiUrl = "https://cdn.discordapp.com/emojis/" + emojiId + "." + ext
					}
				}
					
				if (onlyOne) {
					val rb = RadioButton(context)
					rb.setId(i)
					rb.setText(optionTitle)
					rb.setTextAppearance(context, R.i.UiKit_Settings_Text)
					
					adpatCustomEmoji(context, emojiId, emojiUrl, rb)
					
					val defaultRbSize = rb.getTextSize()
					rb.setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultRbSize * 1.08f)
					
					rb.setPadding(dpToPx(context, 8), dpToPx(context, 10), 0, dpToPx(context, 10))
					rb.setLayoutParams(itemParams)
					radioGroup?.addView(rb)

				} else {
					val cb = CheckBox(context)
					cb.setText(optionTitle)
					cb.setTextAppearance(context, R.i.UiKit_Settings_Text)
					
					adpatCustomEmoji(context, emojiId, emojiUrl, cb)
					
					val defaultCbSize = cb.getTextSize()
					cb.setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultCbSize * 1.08f)

					cb.setPadding(dpToPx(context, 8), dpToPx(context, 10), 0, dpToPx(context, 10))
					cb.setLayoutParams(itemParams)
					cb.setOnCheckedChangeListener { _, isChecked ->
						fof.multiSelectionFlags?.set(i, isChecked)
					}

					layout.addView(cb)
				}
			}
		} catch (e: Exception) {
			fof.logger.error("itemParam", e)
		}
			
		if (onlyOne && radioGroup != null) {
			radioGroup.setOnCheckedChangeListener { _, checkedId ->
				try {
					fof.userSelection = checkedId
					fof.userRealSelection = options.getJSONObject(checkedId).getString("id")
				} catch (e: Exception) {}
			}
			layout.addView(radioGroup)
		}

		val confirm = Button(context)
		confirm.setText("Next")
		confirm.setOnClickListener({
			val chosenOptionIds = mutableListOf<String>()
			val flags = fof.multiSelectionFlags
			
			if (onlyOne) {
				val realSelection = fof.userRealSelection
				if (fof.userSelection != -1 && realSelection != null) {
					chosenOptionIds.add(realSelection)
				}
			} else {
				if (flags != null) {
					for (i in 0 until flags.size) {
						if (flags[i]) chosenOptionIds.add(allOptionIds.get(i))
					}
				}
			}

			if (required && chosenOptionIds.isEmpty()) {
				Utils.showToast("This question is required!", false)
				return@setOnClickListener
			}
			
			for (chosenId in chosenOptionIds) {
				fof.addAnswer(promptId, allOptionIds, chosenId)
			}

			fof.addSeenTime(promptId, allOptionIds)
			
			closePage()
			fof.showChainDialog(context, questions, idx + 1, guildId, userId, pending)
		})
		layout.addView(confirm)
		
		val cancel = DangerButton(context)
		cancel.setText("Cancel")
		cancel.setOnClickListener { closePage() }
		layout.addView(cancel)
	}
}
