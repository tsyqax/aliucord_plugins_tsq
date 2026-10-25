package com.tsq.plugins

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.text.style.ForegroundColorSpan
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils

import com.google.android.material.checkbox.MaterialCheckBox

import com.aliucord.Http
import com.aliucord.Utils
import com.aliucord.views.Button
import com.aliucord.fragments.SettingsPage
import com.discord.utilities.color.ColorCompat

import org.json.JSONArray
import org.json.JSONObject

import kotlin.concurrent.thread
import com.lytefast.flexinput.R

import java.net.URL
import java.net.HttpURLConnection

class ChannelsAndRoles(
	private val fof: FixOnboardingFork, 
	private val questions: MutableList<JSONObject>,
	private val guildId: String,
	private val userId: String,
	private val channels: MutableList<JSONObject>
) : SettingsPage() {

	private val main = Handler(Looper.getMainLooper())

	private val onboardingSelectionMap = mutableMapOf<String, MutableSet<String>>()
	private val channelFlagsMap = mutableMapOf<Long, Int>()
	private val channelFollowOverride = mutableMapOf<Long, Boolean>()

	private var saving = false
	private var browserDirty = true
	private lateinit var saveButton: Button
	
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
	
	private fun getColor(ctx: Context, mode: Int): Int  {
		if (mode == 0) {
			return R.i.UiKit_Settings_Text
		} else {
			return ColorCompat.getThemedColor(ctx, R.b.colorBackgroundPrimary)
		}
	}

	private var baseCol = 0
	private var dimCol = 0
	private var accentCol = 0

	private fun chId(o: JSONObject): Long = o.optString("id").toLongOrNull() ?: 0L
	private fun parentOf(o: JSONObject): Long = o.optLong("parent_id_long", 0L)
		
	private fun adaptCustomEmoji(context: Context, urlStr: String, view: ImageView) {
		thread {
			try {
				val url = URL(urlStr)
				val connection = url.openConnection() as HttpURLConnection
				connection.doInput = true
				connection.connectTimeout = 8000
				connection.readTimeout = 8000
				connection.connect()
				
				val bitmap = connection.inputStream.use { BitmapFactory.decodeStream(it) }
				
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

    fun saveChannelSettings(guildId: Long, updatedFlags: Map<Long, Int>) {
        if (updatedFlags.isEmpty()) return
        val overrides = mutableMapOf<String, Map<String, Any>>()
        updatedFlags.forEach { (channelId, flags) ->
            overrides[channelId.toString()] = mapOf("channel_id" to channelId.toString(), "flags" to flags)
        }
        val body = mapOf(
            "guilds" to mapOf(
                guildId.toString() to mapOf("channel_overrides" to overrides)
            )
        )

        kotlin.concurrent.thread {
            try {
                val req = Http.Request.newDiscordRNRequest("/users/@me/guilds/settings", "PATCH")
                req.executeWithJson(body).assertOk()
            } catch (e: Exception) {
                fof.logger.error("saveChannelSettings", e)
                Utils.showToast("Failed to save channel settings: ${e.message}", false)
            }
        }
    }

    private inline fun JSONArray.forEachObject(action: (JSONObject) -> Unit) {
        for (i in 0 until this.length()) {
            action(this.getJSONObject(i))
        }
    }
	
	// Refered from ChannelBrowser by l6t9
	private fun onChannelFollowChanged(ids: List<Long>, enabled: Boolean) {
		val flags = if (enabled) 4096 else 0
		ids.forEach { channelFlagsMap[it] = flags }
		saveChannelSettings(guildId.toLongOrNull() ?: return, ids.associateWith { flags })
	}
	
	override fun onViewBound(view: View) {
		super.onViewBound(view)

		setActionBarTitle("Onboarding & Browse")
		setActionBarSubtitle(null)

		val parentLayout = getLinearLayout() ?: return
		parentLayout.setPadding(parentLayout.paddingLeft, 0, parentLayout.paddingRight, parentLayout.paddingBottom)
		val ctx = view.context

		val matchParent = ViewGroup.LayoutParams.MATCH_PARENT
		val wrapContent = ViewGroup.LayoutParams.WRAP_CONTENT

		baseCol = TextView(ctx).apply { setTextAppearance(getColor(ctx, 0)) }.currentTextColor
		dimCol = ColorUtils.setAlphaComponent(baseCol, 150)
		accentCol = TypedValue().also {
			ctx.theme.resolveAttribute(com.google.android.material.R.attr.colorAccent, it, true)
		}.data
		val normalColor = baseCol
		val mutedCol = dimCol
		val brand = accentCol

		val mainContent = LinearLayout(ctx).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
		}

		var selectTab: (Int) -> Unit = {}
		val tabLabels = listOf("Onboarding", "Browser")
		val tabTexts = mutableListOf<TextView>()
		val tabIndicators = mutableListOf<View>()

		val tabBar = LinearLayout(ctx).apply {
			orientation = LinearLayout.HORIZONTAL
			layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
		}
		tabLabels.forEachIndexed { idx, label ->
			val tab = LinearLayout(ctx).apply {
				orientation = LinearLayout.VERTICAL
				layoutParams = LinearLayout.LayoutParams(0, wrapContent, 1f)
				isClickable = true
				isFocusable = true
				val a = ctx.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
				background = a.getDrawable(0)
				a.recycle()
			}
			val tv = TextView(ctx).apply {
				text = label
				gravity = Gravity.CENTER
				textSize = 15f
				typeface = Typeface.DEFAULT_BOLD
				setPadding(0, 0, 0, dpToPx(ctx, 10))
				layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
			}
			val indicator = View(ctx).apply {
				layoutParams = LinearLayout.LayoutParams(matchParent, dpToPx(ctx, 3))
			}
			tab.addView(tv)
			tab.addView(indicator)
			tab.setOnClickListener { selectTab(idx) }
			tabTexts.add(tv)
			tabIndicators.add(indicator)
			tabBar.addView(tab)
		}
		mainContent.addView(tabBar)
		mainContent.addView(View(ctx).apply {
			layoutParams = LinearLayout.LayoutParams(matchParent, dpToPx(ctx, 1))
			setBackgroundColor(ColorUtils.setAlphaComponent(mutedCol, 60))
		})

		val onboardingList = LinearLayout(ctx).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
			setPadding(dpToPx(ctx, 16), dpToPx(ctx, 16), dpToPx(ctx, 16), dpToPx(ctx, 16))
		}
		val browserList = LinearLayout(ctx).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
			setPadding(dpToPx(ctx, 16), dpToPx(ctx, 8), dpToPx(ctx, 16), dpToPx(ctx, 16))
			visibility = View.GONE
		}
		
		mainContent.addView(onboardingList)
		mainContent.addView(browserList)
		parentLayout.addView(mainContent)

		saveButton = Button(ctx).apply {
			text = "Save Changes"
			setOnClickListener { submitOnboarding() }
		}

		buildOnboarding(ctx, onboardingList)
		buildBrowser(ctx, browserList)

		if (!attachFixedButton(ctx, parentLayout, saveButton)) {
			onboardingList.addView(saveButton, LinearLayout.LayoutParams(matchParent, wrapContent).apply {
				topMargin = dpToPx(ctx, 16)
			})
		}

		selectTab = { idx ->
			onboardingList.visibility = if (idx == 0) View.VISIBLE else View.GONE
			browserList.visibility = if (idx == 1) View.VISIBLE else View.GONE
			saveButton.visibility = if (idx == 0 && questions.isNotEmpty()) View.VISIBLE else View.GONE
			tabTexts.forEachIndexed { i, t -> t.setTextColor(if (i == idx) normalColor else mutedCol) }
			tabIndicators.forEachIndexed { i, v -> v.setBackgroundColor(if (i == idx) brand else Color.TRANSPARENT) }
			if (idx == 1 && browserDirty) buildBrowser(ctx, browserList)
		}
		selectTab(if (questions.isEmpty()) 1 else 0)
	}

	private fun attachFixedButton(ctx: Context, content: LinearLayout, button: View): Boolean {
		return try {
			val sv = content.parent as? ViewGroup ?: return false
			if (sv !is ScrollView && sv !is androidx.core.widget.NestedScrollView) return false
			val outer = sv.parent as? ViewGroup ?: return false
			val index = outer.indexOfChild(sv)
			val lp = sv.layoutParams
			outer.removeViewAt(index)
			val wrapper = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
			wrapper.addView(sv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
			wrapper.addView(button, LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			).apply { setMargins(dpToPx(ctx, 16), dpToPx(ctx, 8), dpToPx(ctx, 16), dpToPx(ctx, 12)) })
			outer.addView(wrapper, index, lp)
			true
		} catch (e: Exception) {
			fof.logger.error("attachFixedButton", e)
			false
		}
	}

	private fun submitOnboarding() {
		if (saving) return

		val missing = questions.firstOrNull { q ->
			q.optBoolean("required", false) && onboardingSelectionMap[q.optString("id")].isNullOrEmpty()
		}
		if (missing != null) {
			Utils.showToast("Answer required question: ${missing.optString("title")}", false)
			return
		}

		fof.selectedResponses.clear()
		fof.promptsSeen.clear()
		fof.responsesSeen.clear()

		questions.forEach { q ->
			val promptId = q.optString("id")
			val opts = q.optJSONArray("options") ?: return@forEach
			val allIds = mutableListOf<String>()
			for (i in 0 until opts.length()) allIds.add(opts.getJSONObject(i).optString("id"))

			fof.addSeenTime(promptId, allIds)
			val sel = onboardingSelectionMap[promptId]
			if (sel != null) {
				allIds.filter { it in sel }.forEach { fof.selectedResponses.add(it) }
			}
		}

		saving = true
		saveButton.isEnabled = false
		saveButton.text = "Saving..."

		fof.onboardingRequest(questions, guildId, userId) { ok ->
			saving = false
			saveButton.isEnabled = true
			if (ok) {
				saveButton.text = "Saved!"
				main.postDelayed({ if (!saving) saveButton.text = "Save Changes" }, 1500)
			} else {
				saveButton.text = "Save Changes"
			}
		}
	}

	private fun buildOnboarding(ctx: Context, container: LinearLayout) {
		container.removeAllViews()

		if (questions.isEmpty()) {
			container.addView(TextView(ctx).apply {
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
				).apply { topMargin = dpToPx(ctx, 12) }
				gravity = Gravity.CENTER
				text = "There are no onboarding :<"
				textSize = 16f
				setTextColor(dimCol)
			})
			return
		}

		questions.forEach { q ->
			try {
				container.addView(buildQuestionSection(ctx, q))
			} catch (e: Exception) {
				fof.logger.error("itemParam", e)
			}
		}
	}

	private fun buildQuestionSection(ctx: Context, q: JSONObject): LinearLayout {
		val promptId = q.getString("id")
		val title = q.optString("title")
		val required = q.optBoolean("required", false)
		val onlyOne = q.optBoolean("single_select", false)
		val options = q.getJSONArray("options")
		val selected = onboardingSelectionMap.getOrPut(promptId) { linkedSetOf() }
		val brand = accentCol

		val section = LinearLayout(ctx).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			).apply { bottomMargin = dpToPx(ctx, 24) }
		}

		val txtQuestionTitle = TextView(ctx).apply {
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			).apply { bottomMargin = dpToPx(ctx, 12) }
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
			typeface = Typeface.DEFAULT_BOLD
			setTextColor(baseCol)
		}
		if (required) {
			val redAsterisk = SpannableString(" *").apply {
				setSpan(ForegroundColorSpan(Color.RED), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
			}
			txtQuestionTitle.text = TextUtils.concat(title, redAsterisk)
		} else {
			txtQuestionTitle.text = title
		}
		section.addView(txtQuestionTitle)

		val cards = mutableListOf<Pair<String, CardViewHolder>>()
		val refreshAll = { cards.forEach { (oid, c) -> applyCardState(ctx, c, oid in selected, brand) } }

		for (i in 0 until options.length()) {
			val opt = options.getJSONObject(i)
			val optionId = opt.optString("id")
			val optionTitle = opt.optString("title")
			val optionDesc = if (opt.isNull("description")) "" else opt.optString("description")

			val card = createCardView(ctx)
			card.txtCardTitle.text = optionTitle
			if (optionDesc.isNotEmpty()) {
				card.txtCardDescription.text = optionDesc
				card.txtCardDescription.visibility = View.VISIBLE
			} else {
				card.txtCardDescription.visibility = View.GONE
			}

			bindEmoji(ctx, card, opt)
			applyCheckIcon(card)

			cards.add(optionId to card)

			card.cardRoot.setOnClickListener {
				if (onlyOne) {
					if (optionId in selected) {
						selected.remove(optionId)
					} else {
						selected.clear()
						selected.add(optionId)
					}
				} else {
					if (!selected.add(optionId)) selected.remove(optionId)
				}
				browserDirty = true
				refreshAll()
			}
			section.addView(card.cardView)
		}
		refreshAll()
		return section
	}

	private fun applyCardState(ctx: Context, card: CardViewHolder, isSelected: Boolean, brand: Int) {
		val bg = card.cardRoot.background as? GradientDrawable ?: return
		if (isSelected) {
			bg.setColor(ColorUtils.setAlphaComponent(brand, 31))
			bg.setStroke(dpToPx(ctx, 2), brand)
			card.imgCheckMark.visibility = View.VISIBLE
		} else {
			bg.setColor(Color.TRANSPARENT)
			bg.setStroke(dpToPx(ctx, 1), ColorUtils.setAlphaComponent(card.txtCardTitle.currentTextColor, 89))
			card.imgCheckMark.visibility = View.INVISIBLE
		}
	}

	private fun applyCheckIcon(card: CardViewHolder) {
		val img = card.imgCheckMark
		val res = fof.resources
		val customIconId = res?.getIdentifier("checkmark", "drawable", "com.tsq.plugins") ?: 0
		val drawable = if (res != null && customIconId != 0) ResourcesCompat.getDrawable(res, customIconId, null) else null
		if (drawable != null) {
			img.setImageDrawable(drawable)
			img.imageTintList = card.txtCardTitle.textColors
		} else {
			img.setImageResource(android.R.drawable.checkbox_on_background)
		}
		img.visibility = View.INVISIBLE
	}

	private fun bindEmoji(ctx: Context, card: CardViewHolder, opt: JSONObject) {
		val emojiObj = opt.optJSONObject("emoji")
		val emojiId = if (emojiObj == null || emojiObj.isNull("id")) "" else emojiObj.optString("id")
		val emojiName = if (emojiObj == null || emojiObj.isNull("name")) "" else emojiObj.optString("name")
		val img = card.emoji.getChildAt(0) as ImageView
		val txt = card.emoji.getChildAt(1) as TextView

		when {
			emojiId.isNotEmpty() -> {
				card.emoji.visibility = View.VISIBLE
				txt.visibility = View.GONE
				img.visibility = View.VISIBLE
				val ext = if (emojiObj!!.optBoolean("animated", false)) "gif" else "png"
				adaptCustomEmoji(ctx, "https://cdn.discordapp.com/emojis/$emojiId.$ext?size=64", img)
			}
			emojiName.isNotEmpty() -> {
				card.emoji.visibility = View.VISIBLE
				img.visibility = View.GONE
				img.setImageDrawable(null)
				txt.visibility = View.VISIBLE
				txt.text = emojiName
			}
			else -> {
				card.emoji.visibility = View.GONE
			}
		}
	}

	private fun isChannelFollowed(ch: JSONObject): Boolean {
		val id = chId(ch)
		channelFollowOverride[id]?.let { return it }
		if (ch.optBoolean("is_opted_in", false) || ch.optBoolean("is_default", false)) return true

		val idStr = id.toString()
		for (q in questions) {
			val sel = onboardingSelectionMap[q.optString("id")] ?: continue
			if (sel.isEmpty()) continue
			val opts = q.optJSONArray("options") ?: continue
			for (i in 0 until opts.length()) {
				val opt = opts.getJSONObject(i)
				if (opt.optString("id") !in sel) continue
				val cids = opt.optJSONArray("channel_ids") ?: continue
				for (j in 0 until cids.length()) {
					if (cids.optString(j) == idStr) return true
				}
			}
		}
		return false
	}

	private fun buildBrowser(ctx: Context, container: LinearLayout) {
		browserDirty = false
		container.removeAllViews()

		try {
			val categories = channels.filter { it.optInt("type") == 4 }
				.sortedWith(compareBy({ it.optInt("position_int", 0) }, { it.optString("name", "") }))
			val categoryIds = categories.map { chId(it) }.toSet()
			val normal = channels.filter { it.optInt("type") != 4 }

			fun sorted(list: List<JSONObject>) = list.sortedWith(compareBy(
				{ if (it.optInt("type") == 2 || it.optInt("type") == 13) 1 else 0 },
				{ it.optInt("position_int", 0) },
				{ it.optString("name", "") }
			))

			val uncategorized = normal.filter { val p = parentOf(it); p == 0L || p !in categoryIds }
			if (uncategorized.isNotEmpty()) {
				addBrowserSection(ctx, container, "Uncategorized", 0L, sorted(uncategorized), true)
			}

			categories.forEach { cat ->
				val cid = chId(cat)
				val kids = normal.filter { parentOf(it) == cid }
				if (kids.isEmpty()) return@forEach
				addBrowserSection(ctx, container, cat.optString("name"), cid, sorted(kids), true)
			}

			if (container.childCount == 0) {
				container.addView(TextView(ctx).apply {
					text = "No channels available to browse."
					gravity = Gravity.CENTER
					textSize = 16f
					setTextColor(dimCol)
					setPadding(0, dpToPx(ctx, 24), 0, 0)
				})
			}
		} catch (e: Exception) {
			fof.logger.error("browseRender", e)
			Utils.showToast("browseRenderError: " + e.message)
		}
	}

	private fun addBrowserSection(
		ctx: Context,
		container: LinearLayout,
		heading: String,
		categoryId: Long,
		members: List<JSONObject>,
		withFollow: Boolean
	) {
		val rows = mutableListOf<Pair<JSONObject, ChannelCardViewHolder>>()
		var syncing = false
		var categoryBox: MaterialCheckBox? = null

		val header = LinearLayout(ctx).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			).apply { topMargin = dpToPx(ctx, 16) }
		}
		header.addView(TextView(ctx).apply {
			text = heading
			typeface = Typeface.DEFAULT_BOLD
			textSize = 13f
			setTextColor(dimCol)
			maxLines = 1
			ellipsize = TextUtils.TruncateAt.END
			layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
		})

		if (withFollow) {
			categoryBox = MaterialCheckBox(ctx).apply {
				text = "Follow All"
				textSize = 12f
				setTextColor(dimCol)
				buttonTintList = ColorStateList.valueOf(accentCol)
				minWidth = 0
				minHeight = 0
				contentDescription = "Follow All $heading"
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
				)
			}
			header.addView(categoryBox)
		}
		container.addView(header)

		val group = LinearLayout(ctx).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			)
		}

		members.forEach { ch ->
			val card = createChannelCardView(ctx, ch.optInt("type"))
			card.txtChannelName.text = ch.optString("name")
			card.comCheckBox.isChecked = isChannelFollowed(ch)

			card.comCheckBox.setOnCheckedChangeListener { _, checked ->
				if (syncing) return@setOnCheckedChangeListener
				val id = chId(ch)
				channelFollowOverride[id] = checked
				onChannelFollowChanged(listOf(id), checked)
				if (withFollow) {
					syncing = true
					categoryBox?.isChecked = rows.all { it.second.comCheckBox.isChecked }
					syncing = false
				}
			}
			card.cardRoot.setOnClickListener { card.comCheckBox.toggle() }

			rows.add(ch to card)
			group.addView(card.cardView)
		}
		container.addView(group)

		categoryBox?.let { box ->
			syncing = true
			box.isChecked = rows.isNotEmpty() && rows.all { it.second.comCheckBox.isChecked }
			syncing = false

			box.setOnCheckedChangeListener { _, checked ->
				if (syncing) return@setOnCheckedChangeListener
				syncing = true
				rows.forEach { (ch, card) ->
					channelFollowOverride[chId(ch)] = checked
					card.comCheckBox.isChecked = checked
				}
				syncing = false
				val ids = rows.map { chId(it.first) }
				onChannelFollowChanged(if (categoryId != 0L) ids + categoryId else ids, checked)
			}
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
			setTextColor(baseCol)
		}

		val description = TextView(context).apply {
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 
				LinearLayout.LayoutParams.WRAP_CONTENT
			).apply {
				topMargin = dp4
			}
			setTextAppearance(R.i.UiKit_TextView_Subtext)
			setTextColor(dimCol)
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
				setStroke(dpToPx(context, 1), ColorUtils.setAlphaComponent(title.currentTextColor, 89))
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
	
	class ChannelCardViewHolder(
		val cardView: View,
		val cardRoot: LinearLayout,
		val imgChannelIcon: ImageView,
		val txtChannelName: TextView,
		val comCheckBox: MaterialCheckBox
	)

	private fun createChannelCardView(context: Context, type: Int): ChannelCardViewHolder {
		val dp4 = dpToPx(context, 4)
		val dp6 = dpToPx(context, 6)
		val dp10 = dpToPx(context, 10)
		val dp20 = dpToPx(context, 20)
		val dp44 = dpToPx(context, 44)

		val root = LinearLayout(context).apply {
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			)
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			minimumHeight = dp44
			setPadding(dp4, dp6, dp4, dp6)
			isClickable = true
			isFocusable = true
			val a = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
			background = a.getDrawable(0)
			a.recycle()
		}

		val iconName = if (type == 2 || type == 13) "ic_channel_voice" else "ic_channel_text"
		val iconRes = Utils.getResId(iconName, "drawable").takeIf { it != 0 }
			?: Utils.getResId(iconName + "_16dp", "drawable")

		val channelIcon = ImageView(context).apply {
			layoutParams = LinearLayout.LayoutParams(dp20, dp20).apply { marginEnd = dp10 }
			scaleType = ImageView.ScaleType.FIT_CENTER
			if (iconRes != 0) {
				setImageResource(iconRes)
				imageTintList = ColorStateList.valueOf(dimCol)
			} else {
				setImageResource(android.R.drawable.ic_menu_gallery)
			}
		}
		root.addView(channelIcon)

		val title = TextView(context).apply {
			layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
			textSize = 15f
			maxLines = 1
			ellipsize = TextUtils.TruncateAt.END
			setTextColor(baseCol)
		}
		root.addView(title)

		val checkBox = MaterialCheckBox(context).apply {
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			)
			text = ""
			minWidth = 0
			minHeight = 0
			buttonTintList = ColorStateList.valueOf(accentCol)
		}
		root.addView(checkBox)

		return ChannelCardViewHolder(
			cardView = root,
			cardRoot = root,
			imgChannelIcon = channelIcon,
			txtChannelName = title,
			comCheckBox = checkBox
		)
	}

}
