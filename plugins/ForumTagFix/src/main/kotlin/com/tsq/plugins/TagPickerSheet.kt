package com.tsq.plugins

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.aliucord.Utils
import com.aliucord.utils.DimenUtils
import com.aliucord.views.Button
import com.aliucord.views.DangerButton
import com.aliucord.widgets.BottomSheet

import com.discord.api.channel.ForumTag
import com.lytefast.flexinput.R

import java.util.HashSet
import java.util.List
import java.util.Set

// many parameter is major constructor
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
		
		val root = LinearLayout(context)
		root.setOrientation(LinearLayout.VERTICAL)
		root.setPadding(p, p, p, p)
		root.setLayoutParams(LinearLayout.LayoutParams(-1, -1))

		val title = TextView(context, null, 0, R.i.UiKit_Settings_Text)
		title.text = "Secect Tags"
		//title.TextSize = 18f
		//title.setTextColor(Color.WHITE);
		title.setTypeface(null, Typeface.BOLD)
		title.setPadding(0, 0, 0, p)
		root.addView(title)

		// RecyclerView
		// NestedScrollView + LinearLayout
		val rv = RecyclerView(context)
		val adapter = TagAdapter(tags, selectedSet)
		val rvParams = LinearLayout.LayoutParams(-1, 0, 1.0f)
		
		rv.layoutManager  = LinearLayoutManager(context)
		rv.adapter = adapter
		
		val confirm = Button(context)
		confirm.text = "OK"
		confirm.setOnClickListener  { v ->
			selectedTagIds.clear()
			selectedTagIds.addAll(selectedSet)
			onComplete?.run()
			closePage()
		}
		
		val cancel = DangerButton(context)
		cancel.text = "Cancel"
		cancel.setOnClickListener  { v -> closePage() }

		root.addView(rv, rvParams)
		root.addView(confirm)
		root.addView(cancel)

		addView(root)
	}
	
	private class TagAdapter(private val data: MutableList<ForumTag>, private val selected: MutableSet<Long>): RecyclerView.Adapter<TagAdapter.VH>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
			val tv = TextView(parent.context, null, 0, R.i.UiKit_Settings_Text)
			val p = DimenUtils.dpToPx(16)
			tv.setPadding(p, p, p, p)
			tv.layoutParams = RecyclerView.LayoutParams(-1, -2)
			return VH(tv)
		}
		
		override fun onBindViewHolder(holder: VH, position: Int) {
			val tag = data[position]
			val id = tag.c()
			holder.tv.text = (if (tag.b() != null) tag.b() + " " else "") + tag.d()
			holder.tv.setBackgroundColor(if (selected.contains(id)) 0x405865F2.toInt() else 0)
			
			holder.tv.setOnClickListener { v ->
				if (selected.contains(id)) {
					(selected as? MutableSet<Long>)?.remove(id)
					holder.tv.setBackgroundColor(0)
				} else {
					selected.add(id)
					holder.tv.setBackgroundColor(0x405865F2.toInt())
				}
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
