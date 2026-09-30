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
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator

import com.aliucord.Utils
import com.aliucord.utils.DimenUtils
import com.aliucord.views.Button
import com.aliucord.widgets.BottomSheet

import com.google.android.flexbox.FlexboxLayout

import com.discord.utilities.view.text.SimpleDraweeSpanTextView
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

		val scrollView = object : NestedScrollView(context) {
			override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
				val maxPx = DimenUtils.dpToPx(320)
				val newHeightSpec = View.MeasureSpec.makeMeasureSpec(maxPx, View.MeasureSpec.AT_MOST)
				super.onMeasure(widthMeasureSpec, newHeightSpec)
			}
		}.apply {
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
				topMargin = DimenUtils.dpToPx(8)
				weight = 1.0f 
			}
		}

		val flexContainer = FlexboxLayout(context, null).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            setFlexDirection(0)
            setFlexWrap(1)
            
            val margin10 = DimenUtils.dpToPx(10)
            setPadding(margin10, 0, margin10, 0)
        }

        val adapter = TagAdapter(tags, selectedSet)
        val count = adapter.itemCount
        
        for (i in 0 until count) {
            val holder = adapter.onCreateViewHolder(flexContainer, adapter.getItemViewType(i))
            adapter.onBindViewHolder(holder, i)
            flexContainer.addView(holder.container)
        }

        scrollView.addView(flexContainer)
		
		val confirm = Button(context).apply {
			text = "OK"
			layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
				topMargin = DimenUtils.dpToPx(8)
			}
			setOnClickListener {
				selectedTagIds.clear()
				selectedTagIds.addAll(selectedSet)
				onComplete.run()
				closePage()
			}
		}
		
		root.addView(scrollView)
		root.addView(confirm)

		addView(root)
	}
	
	private class TagAdapter(private val data: MutableList<ForumTag>, private val selected: MutableSet<Long>): RecyclerView.Adapter<TagAdapter.VH>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
			val context = parent.context
			val ph8 = DimenUtils.dpToPx(8)
			val pv2 = DimenUtils.dpToPx(2)

			val itemLayout = LinearLayout(context).apply {
				orientation = LinearLayout.HORIZONTAL
				setPadding(ph8, pv2, ph8, pv2)

				layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
					rightMargin = DimenUtils.dpToPx(8)
					bottomMargin = DimenUtils.dpToPx(8)
				}
				
				background = GradientDrawable().apply {
					shape = GradientDrawable.RECTANGLE
					cornerRadius = DimenUtils.dpToPx(14).toFloat() 
				}
			}

			val emojiView = SimpleDraweeSpanTextView(context).apply {
				id = View.generateViewId()
				layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
				visibility = View.GONE
			}

			val nameView = TextView(context, null, 0, R.i.UiKit_Settings_Text).apply {
				id = View.generateViewId()
				layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
				setPadding(DimenUtils.dpToPx(4), 0, 0, 0)
				maxLines = 1
			}

			itemLayout.addView(emojiView)
			itemLayout.addView(nameView)

			return VH(itemLayout, emojiView, nameView)
		}
		
		override fun onBindViewHolder(holder: VH, position: Int) {
			val tag = data[position]
			val id = tag.c()
			
			if (tag.b() != null) {
				holder.emojiTv.text = tag.b().toString()
				holder.emojiTv.visibility = View.VISIBLE
			} else {
				holder.emojiTv.visibility = View.GONE
			}

			holder.nameTv.text = tag.d()

			val strokeDrawable = holder.container.background as GradientDrawable
			val baseTextColor = holder.nameTv.currentTextColor
			
			strokeDrawable.setColor(Color.TRANSPARENT)

			if (selected.contains(id)) {
				val thickness1_7dp = (DimenUtils.dpToPx(17) / 10f).toInt().coerceAtLeast(1)
				strokeDrawable.setStroke(thickness1_7dp, 0xFF5865F2.toInt())
			} else {
				val thickness1dp = DimenUtils.dpToPx(1)
				val defaultBorderColor = ColorUtils.setAlphaComponent(baseTextColor, 95)
				strokeDrawable.setStroke(thickness1dp, defaultBorderColor)
			}
			
			holder.container.setOnClickListener {
				if (selected.contains(id)) {
					selected.remove(id)
				} else {
					selected.add(id)
				}
				onBindViewHolder(holder, position)
			}
		}

		override fun getItemCount(): Int {
			try {
				return data.size
			} catch (e: Exception) {
				return 0
			}
		}
		
		class VH(val container: LinearLayout, val emojiTv: SimpleDraweeSpanTextView, val nameTv: TextView): RecyclerView.ViewHolder(container)
	}
}
