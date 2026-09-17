package com.tsq.plugins

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView

import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment

import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.patcher.*
import com.aliucord.views.Button
import com.aliucord.api.SettingsAPI
import com.aliucord.fragments.SettingsPage
import com.discord.utilities.color.ColorCompat

import org.json.JSONArray
import org.json.JSONObject

import kotlin.concurrent.thread
import com.lytefast.flexinput.R

import java.net.URL
import java.net.HttpURLConnection

// Thanks to 'Loomis' for the better UI
class OnboardingPage(
	private val fof: FixOnboardingFork, 
	private val questions: MutableList<JSONObject>,
	private val promptId: String,          // question ID
	private val allTitle: String,          // questions Title (Unicode Escape Sequence)
	private val required: Boolean,         // questions required
	private val onlyOne: Boolean,          // answer is onlyOne?
	private val options: JSONArray,        // options -> id, title, description(optional)
	private val guildId: String,
	private val userId: String,
	private val allOptionIds: MutableList<String>,
	private val idx: Int
) : SettingsPage() {
	
	private lateinit var btnClose: ImageButton
	private lateinit var txtProgress: TextView
	private lateinit var txtQuestionTitle: TextView
	private lateinit var btnBack: Button
	private lateinit var btnNext: Button
	private lateinit var optionsContainer: LinearLayout
	
	//val hexRegex = """^#([A-Fa-f0-9]{6})$""".toRegex()
	
	private fun getColor(ctx: Context, mode: Int): Int  {
		if (mode == 0) {
			return R.i.UiKit_Settings_Text
		} else {
			return ColorCompat.getThemedColor(ctx, R.b.colorBackgroundPrimary)
		}
	}

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
		
	private fun adaptCustomEmoji(context: Context, urlStr: String, view: ImageView) {
		thread {
			try {
				val url = URL(urlStr)
				val connection = url.openConnection() as HttpURLConnection
				connection.doInput = true
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
		
	private fun chapCustomEmoji(context: Context, bitmap: Bitmap, view: ImageView) {
		val emojiSize = dpToPx(context, 24)
		val scaled = Bitmap.createScaledBitmap(bitmap, emojiSize, emojiSize, true)
		view.setImageBitmap(scaled)
	}
	
	override fun onViewCreated(view: View, bundle: Bundle?) {
		super.onViewCreated(view, bundle)

		val parentLayout = getLinearLayout()
		val context = view.context
		parentLayout.setBackgroundColor(getColor(context, 1))

		val ID_LAYOUT_HEADER = 1001
		val ID_LAYOUT_BOTTOM_BAR = 1002
		val ID_TXT_QUESTION_TITLE = 1003

		val dp16 = dpToPx(context, 16)
		val dp12 = dpToPx(context, 12)
		val dp8 = dpToPx(context, 8)
		
		val getThemeAttr = { attrId: Int ->
			val typedValue = TypedValue()
			context.theme.resolveAttribute(attrId, typedValue, true)
			typedValue.resourceId
		}

		val rootLayout = RelativeLayout(context).apply {
			layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
			setPadding(dp8, 0, dp8, dp8)
		}
		
		val layoutHeader = LinearLayout(context).apply {
			id = ID_LAYOUT_HEADER
			orientation = LinearLayout.VERTICAL
			setPadding(0, 0, 0, dp8)
			
			layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
				addRule(RelativeLayout.ALIGN_PARENT_TOP)
			}
		}

		txtQuestionTitle = TextView(context).apply {
			layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
			setTextAppearance(getColor(context, 0))
			setTypeface(null, Typeface.BOLD)
			id = ID_TXT_QUESTION_TITLE
		}
		
		layoutHeader.addView(txtQuestionTitle)
		rootLayout.addView(layoutHeader)

		val layoutBottomBar = RelativeLayout(context).apply {
			id = ID_LAYOUT_BOTTOM_BAR
			setPadding(0, dp12, 0, 0)
			
			layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
				addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
			}
		}

		btnBack = Button(context).apply {
			text = "Back"
			layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
				addRule(RelativeLayout.ALIGN_PARENT_START)
				addRule(RelativeLayout.CENTER_VERTICAL)
			}
		}
		layoutBottomBar.addView(btnBack)

		btnNext = Button(context).apply {
			text = "Next"
			layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
				addRule(RelativeLayout.ALIGN_PARENT_END)
				addRule(RelativeLayout.CENTER_VERTICAL)
			}
		}
		layoutBottomBar.addView(btnNext)
		rootLayout.addView(layoutBottomBar)

		val scrollView = ScrollView(context).apply {
			isFillViewport = true
			isVerticalScrollBarEnabled = true

			layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT).apply {
				addRule(RelativeLayout.BELOW, ID_LAYOUT_HEADER)
				addRule(RelativeLayout.ABOVE, ID_LAYOUT_BOTTOM_BAR)
			}
		}

		optionsContainer = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(0, dp8, 0, dp8)
			layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
		}
		scrollView.addView(optionsContainer)
		rootLayout.addView(scrollView)

		parentLayout.addView(rootLayout)
		
		// ----------------------------------------
		
		//btnClose.setImageResource(android.R.drawable.ic_menu_revert) 

		setActionBarTitle("Step ${idx + 1} of ${questions.size}") //before: dIalogTitle, R.i.UiKit_Settings_Text
		setActionBarSubtitle(null)
		txtQuestionTitle.text = allTitle
		
		if (required) { 
			val redAsterisk = SpannableString(" *").apply { setSpan(ForegroundColorSpan(Color.RED), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) }
			txtQuestionTitle.text = TextUtils.concat(allTitle, redAsterisk)
		} else {
			txtQuestionTitle.text = allTitle
		}
		
		val defaultTitleSize = txtQuestionTitle.textSize
		txtQuestionTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTitleSize * 1.12f)
		
		btnBack.setOnClickListener {
			closePage()
			fof.showChainDialog(context, questions, idx - 1, guildId, userId)
		}
		
		btnNext.setOnClickListener {
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
			fof.showChainDialog(context, questions, idx + 1, guildId, userId)
		}

		if (idx == 0) {
			btnBack.visibility = View.INVISIBLE
		} else {
			btnBack.visibility = View.VISIBLE
		}
		
		if (idx + 1 == questions.size) btnNext.text = "DONE"
		
		if (!onlyOne) fof.multiSelectionFlags = BooleanArray(options.length())

		try {
			for (i in 0 until options.length()) {
				val card = createCardView(context)
				val cardView = card.cardView as LinearLayout
				val cardRoot = card.cardRoot
				val emojiFrame = card.emoji
				val imgEmoji = emojiFrame.getChildAt(0) as ImageView
				val txtEmojiText = emojiFrame.getChildAt(1) as TextView
				val txtCardTitle = card.txtCardTitle
				val txtCardDescription = card.txtCardDescription
				val imgCheckMark = card.imgCheckMark
				
				val opt = options.getJSONObject(i)
				var optionTitle = opt.optString("title")
				var optionDesc = opt.optString("description") ?: ""
				val id = opt.optString("id")
				val emojiObj = opt.optJSONObject("emoji")
				val emojiName = emojiObj?.optString("name") ?: ""
				var emojiUrl = ""
				val emojiId = emojiObj?.optString("id") ?: ""
				
				txtCardTitle.text = optionTitle
				//txtCardTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, txtCardTitle.textSize)

				if (optionDesc.isNotEmpty()) {
					txtCardDescription.text = optionDesc
					txtCardDescription.visibility = View.VISIBLE
					//txtCardDescription.setTextSize(TypedValue.COMPLEX_UNIT_PX, txtCardDescription.textSize * 0.94f)
				} else {
					txtCardDescription.visibility = View.GONE
				}
				if (emojiObj != null && !emojiObj.isNull("name")) {
					if (emojiObj.isNull("id")) {
						imgEmoji.visibility = View.GONE
						imgEmoji.setImageDrawable(null)
						
						txtEmojiText.visibility = View.VISIBLE
						txtEmojiText.text = emojiName
						txtCardTitle.text = optionTitle
					} else {
						txtEmojiText.visibility = View.GONE
						txtEmojiText.text = ""
						
						val isAnimated = emojiObj.optBoolean("animated", false)
						val ext = if (isAnimated) "gif" else "png"

						emojiUrl = "https://cdn.discordapp.com/emojis/" + emojiId + "." + ext
						
						imgEmoji.visibility = View.VISIBLE
						adaptCustomEmoji(context, emojiUrl, imgEmoji)
						txtCardTitle.text = optionTitle
					}
				} else {
					imgEmoji.visibility = View.GONE
					imgEmoji.setImageDrawable(null)
					txtEmojiText.visibility = View.GONE
					txtEmojiText.text = ""
					txtCardTitle.text = optionTitle
				}
				
				val fofResources = fof.resources
				val customIconId = fofResources?.getIdentifier("checkmark", "drawable", "com.tsq.plugins")
				if (fofResources != null && customIconId != null && customIconId != 0) {
					val vectorDrawable = ResourcesCompat.getDrawable(fofResources, customIconId, null)
					if (vectorDrawable != null) {
						imgCheckMark.setImageDrawable(vectorDrawable)
						imgCheckMark.imageTintList = txtCardTitle.textColors
					} else {
						imgCheckMark.setImageResource(android.R.drawable.checkbox_on_background)
						fof.logger.info("novec")
					}
				} else {
					imgCheckMark.setImageResource(android.R.drawable.checkbox_on_background)
					fof.logger.info("noID")
				}
				
				imgCheckMark.visibility = View.INVISIBLE
				
				val typedValue = TypedValue()
				context.theme.resolveAttribute(R.b.colorAccent, typedValue, true)
				val accentColor = typedValue.data
				val selectedBgColor = ColorUtils.setAlphaComponent(accentColor, 31)
				
				cardRoot.setOnClickListener {
					val baseTextColor = txtCardTitle.currentTextColor
					val defaultBorderColor = ColorUtils.setAlphaComponent(baseTextColor, 89)
					val activeBorderColor = selectedBgColor
					
					if (onlyOne) {
						val isAlreadySelected = (fof.userSelection == i)
						if (isAlreadySelected) {
							fof.userSelection = -1
							fof.userRealSelection = null
						} else {
							fof.userSelection = i
							fof.userRealSelection = id
						}

						for (childIdx in 0 until optionsContainer.childCount) {
							val childCard = optionsContainer.getChildAt(childIdx) as LinearLayout
							val childCheck = childCard.getChildAt(2) as ImageView
							val strokeDrawable = childCard.background as GradientDrawable

							if (childIdx == fof.userSelection) { 
								strokeDrawable.setColor(selectedBgColor)
								strokeDrawable.setStroke(dpToPx(context, 3), activeBorderColor)
								childCheck.visibility = View.VISIBLE
							} else {
								strokeDrawable.setColor(Color.TRANSPARENT)
								strokeDrawable.setStroke(dpToPx(context, 1), defaultBorderColor)
								childCheck.visibility = View.INVISIBLE
							}
						}
						} else {
						val flags = fof.multiSelectionFlags
						if (flags != null) {
							val strokeDrawable = cardRoot.background as GradientDrawable
							val nextState = !flags[i]
							flags[i] = nextState

							if (nextState) {
								strokeDrawable.setColor(selectedBgColor)
								strokeDrawable.setStroke(dpToPx(context, 3), activeBorderColor)
								imgCheckMark.visibility = View.VISIBLE
							} else {
								strokeDrawable.setColor(Color.TRANSPARENT)
								strokeDrawable.setStroke(dpToPx(context, 1), defaultBorderColor)
								imgCheckMark.visibility = View.INVISIBLE
							}
						}
					}
				}
				optionsContainer.addView(cardView)
			}
		} catch (e: Exception) {
			fof.logger.error("itemParam", e)
		}
	}
	
	class CardViewHolder(
		val cardView: View,
		val cardRoot: LinearLayout,
		val emoji: FrameLayout,
		val txtCardTitle: TextView,
		val txtCardDescription: TextView,
		val imgCheckMark: ImageView
	)

	private fun createCardView(context: Context): CardViewHolder {
		val dp16 = dpToPx(context, 16)
		val dp12 = dpToPx(context, 12)
		val dp8 = dpToPx(context, 8)
		val dp4 = dpToPx(context, 4)
		val dp40 = dpToPx(context, 40)
		val dp24 = dpToPx(context, 24)

		val title = TextView(context).apply {
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 
				LinearLayout.LayoutParams.WRAP_CONTENT
			)
			setTextAppearance(getColor(context, 0))
		}

		val description = TextView(context).apply {
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 
				LinearLayout.LayoutParams.WRAP_CONTENT
			).apply {
				topMargin = dp4
			}
			setTextAppearance(R.i.UiKit_TextView_Subtext) // ?
			//setTextAppearance(ColorUtils.setAlphaComponent(getColor(context, 0), 179))
			visibility = View.GONE
		}

		val root = LinearLayout(context).apply {
			layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
				bottomMargin = dp8
			}
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			setPadding(dp16, dp16, dp16, dp16)
			isClickable = true
			isFocusable = true

			background = GradientDrawable().apply {
				shape = GradientDrawable.RECTANGLE
				cornerRadius = dpToPx(context, 8).toFloat()

				setColor(Color.TRANSPARENT) 

				val baseTextColor = title.currentTextColor
				val borderColor = ColorUtils.setAlphaComponent(baseTextColor, 89)
				setStroke(dpToPx(context, 1), borderColor)
			}
		}

		val emoji = FrameLayout(context).apply {
			layoutParams = LinearLayout.LayoutParams(dp40, dp40)
			
			addView(ImageView(context).apply {
				layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
				scaleType = ImageView.ScaleType.FIT_CENTER
				visibility = View.INVISIBLE
			})
			
			addView(TextView(context).apply {
				layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
				gravity = Gravity.CENTER
				textSize = 24f
				visibility = View.GONE
			})
		}
		root.addView(emoji)

		val textContainer = LinearLayout(context).apply {
			layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
				weight = 1f
				setMarginStart(dp12)
				setMarginEnd(dp12)
			}
			orientation = LinearLayout.VERTICAL
		}

		textContainer.addView(title)
		textContainer.addView(description)
		root.addView(textContainer)

		val checkMark = ImageView(context).apply {
			layoutParams = LinearLayout.LayoutParams(dp24, dp24)
			scaleType = ImageView.ScaleType.CENTER_INSIDE
		}
		root.addView(checkMark)

		return CardViewHolder(
			cardView = root,
			cardRoot = root,
			emoji = emoji,
			txtCardTitle = title,
			txtCardDescription = description,
			imgCheckMark = checkMark
		)
	}
}
