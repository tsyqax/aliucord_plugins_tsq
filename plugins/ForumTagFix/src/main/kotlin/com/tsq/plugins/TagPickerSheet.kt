package com.tsq.plugins

import android.content.Context
import android.util.TypedValue
import android.graphics.Typeface
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator

import com.aliucord.Utils
import com.aliucord.utils.DimenUtils
import com.aliucord.views.Button
import com.aliucord.views.DangerButton
import com.aliucord.widgets.BottomSheet

import com.discord.api.channel.ForumTag
import com.lytefast.flexinput.R

// Thanks to 'Loomis' for the better UI
class TagPickerSheet(private val tags: MutableList<ForumTag>, private val selectedTagIds: MutableList<Long>, private var apl_tags: MutableList<Long>?, private val onComplete: Runnable) : BottomSheet() {
	
	// minor
	constructor(tags: MutableList<ForumTag>, selectedTagIds: MutableList<Long>, onComplete: Runnable) : this(tags, selectedTagIds, null, onComplete)
	private fun closePage() {
		Utils.mainThread.post {
			try {
				dismiss()
			} catch (ignore: Exception) {}
			
			// null = skip
			activity?.run {
				if (!isFinishing) {
					finish()
				}
			}
		}
	}
	
	override fun onViewCreated(view: View, bundle: Bundle?) {
		super.onViewCreated(view, bundle)
		val context = view.context
		val p = DimenUtils.dpToPx(16)
		var selectedSet: MutableSet<Long>
		
		if (apl_tags == null) {
			selectedSet = selectedTagIds.toMutableSet()
		} else {
			selectedSet = apl_tags!!.toMutableSet()
		}
		
		val root = LinearLayout(context).apply {
			setOrientation(LinearLayout.VERTICAL)
			setPadding(p, p, p, p)
			setLayoutParams(LinearLayout.LayoutParams(-1, -1))
		}

		val title = TextView(context, null, 0, R.i.UiKit_Settings_Text).apply {
			text = "Select Tags"
			setTextSize(TypedValue.COMPLEX_UNIT_PX, 1.2f * textSize)
			setTypeface(null, Typeface.BOLD)
			setPadding(0, 0, 0, p)
		}
		root.addView(title)

		// RecyclerView
		// NestedScrollView + LinearLayout
		val rv = RecyclerView(context)
		val adapter = TagAdapter(tags, selectedSet)
		val rvParams = LinearLayout.LayoutParams(-1, 0, 1.0f)
		
		rv.layoutManager  = LinearLayoutManager(context)
		rv.adapter = adapter
		(rv.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
		
		val buttonRow = LinearLayout(context).apply {
			orientation = LinearLayout.HORIZONTAL
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
		}
		
		val btnParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f).apply {
			rightMargin = DimenUtils.dpToPx(4)
			leftMargin = DimenUtils.dpToPx(4)
		}
		
		val cancel = DangerButton(context).apply {
			text = "Cancel"
			layoutParams = btnParams
			setOnClickListener { closePage() }
		}

		val confirm = Button(context).apply {
			text = "OK"
			layoutParams = btnParams
			setOnClickListener {
				selectedTagIds.clear()
				selectedTagIds.addAll(selectedSet)
				onComplete.run()
				closePage()
			}
		}
		
		buttonRow.addView(cancel)
		buttonRow.addView(confirm)
		
		root.addView(rv, rvParams)
		root.addView(buttonRow)

		addView(root)
	}
	
	private class TagAdapter(private val data: MutableList<ForumTag>, private val selected: MutableSet<Long>): RecyclerView.Adapter<TagAdapter.VH>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
			val context = parent.context
			val ph = DimenUtils.dpToPx(16) 
			val pv = DimenUtils.dpToPx(10) 

			val tv = TextView(context, null, 0, R.i.UiKit_Settings_Text).apply {
				setPadding(ph, pv, ph, pv)

				layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
					bottomMargin = DimenUtils.dpToPx(8)
				}
				
				val baseTextColor = currentTextColor
				val defaultBorderColor = ColorUtils.setAlphaComponent(baseTextColor, 95)
				
				background = GradientDrawable().apply {
					shape = GradientDrawable.RECTANGLE
					setStroke(DimenUtils.dpToPx(1), defaultBorderColor)
					setColor(Color.TRANSPARENT)
					cornerRadius = DimenUtils.dpToPx(14).toFloat() 
				}
			}
			return VH(tv)
		}
		
		override fun onBindViewHolder(holder: VH, position: Int) {
			val tag = data[position]
			val id = tag.c()
			holder.tv.text = (if (tag.b() != null) tag.b() + " " else "") + tag.d()
			
			val strokeDrawable = holder.tv.background as GradientDrawable
			
			if (selected.contains(id)) {
				strokeDrawable.setColor(0x405865F2)
			} else {
				strokeDrawable.setColor(Color.TRANSPARENT)
			}
			
			holder.tv.setOnClickListener { v ->
				if (selected.contains(id)) {
					selected.remove(id)
					strokeDrawable.setColor(Color.TRANSPARENT)
				} else {
					selected.add(id)
				}
				notifyItemChanged(position)
			}
		}

		override fun getItemCount(): Int{
			try {
				return data.size
			} catch (e: Exception) {
				return 0
			}
		}
		
		class VH(v: View): RecyclerView.ViewHolder(v) {
			val tv: TextView = v as TextView
		}
	}
}
