package com.tsq.plugins

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.view.ViewOutlineProvider
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.GridLayout
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

import com.aliucord.PluginManager

import com.discord.widgets.chat.list.adapter.WidgetChatListAdapter
import com.discord.widgets.chat.list.entries.ChatListEntry
import com.discord.widgets.media.WidgetMedia.Companion
import com.discord.utilities.images.MGImages
import com.discord.utilities.mg_recycler.MGRecyclerViewHolder
import com.discord.api.message.attachment.MessageAttachment
import com.discord.models.message.Message
import com.discord.stores.StoreStream

import com.facebook.drawee.view.SimpleDraweeView

class MosaicViewHolder : MGRecyclerViewHolder<WidgetChatListAdapter, ChatListEntry> {
    private val gridLayout: GridLayout
    private val isOpened = hashSetOf<Int>()
    private var evhandler: WidgetChatListAdapter.EventHandler? = null

    constructor(gridLayout: GridLayout, adapter: WidgetChatListAdapter) : super(gridLayout, adapter) {
        this.gridLayout = gridLayout
        initGrid()
    }

    constructor(itemView: View, gridLayout: GridLayout, adapter: WidgetChatListAdapter) : super(itemView, adapter) {
        this.gridLayout = gridLayout
        initGrid()
    }

    private fun initGrid() {
        gridLayout.layoutParams = gridLayout.layoutParams.apply { width = MosaicFork.realWidth }
        this.evhandler = (this.adapter)?.eventHandler
    }
	
	fun getGridLayout() = this.gridLayout
	
	override fun onConfigure(position: Int, data: ChatListEntry) {
		val mosaicEntry = data as? MosaicEntry ?: return
		
		val images = mosaicEntry.images
		val msg = mosaicEntry.msg
		val total = images.size

		gridLayout.setColumnCount(6)

		val currentChildCount = gridLayout.getChildCount()
		var gifCount = 0

		if (currentChildCount > total) gridLayout.removeViews(total, currentChildCount - total)
		
		val guildId = StoreStream.getGuildSelected().getSelectedGuildId()
		var shouldSpoilered = false
		
		PluginManager.plugins.get("BetterSpoiler")?.let { bs ->
			val method = bs.javaClass.getDeclaredMethod("shouldEnableSpoiler", Message::class.java, Long::class.javaPrimitiveType).apply { isAccessible = true }
			shouldSpoilered = method.invoke(bs, msg, guildId) as Boolean
		}

		for (i in 0 until total) {
			var container: FrameLayout? = null
			var imageView: SimpleDraweeView? = null
			val spanSize = getSpanSize(total, i)

			if (i < currentChildCount) {
				container = gridLayout.getChildAt(i) as FrameLayout
				imageView = container.getChildAt(0) as SimpleDraweeView
			} else {
				container = FrameLayout(gridLayout.getContext())
				imageView = SimpleDraweeView(gridLayout.getContext())
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP)
				imageView.setOutlineProvider(object : ViewOutlineProvider() {
					override fun getOutline(view: View, outline: Outline) {
						outline.setRoundRect(0, 0, view.width, view.height, 8f)
					}
				})
				imageView.setClipToOutline(true)
				container.addView(imageView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
				gridLayout.addView(container)
			}

			val rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1)
			val colSpec = GridLayout.spec(GridLayout.UNDEFINED, spanSize, 1f)
			val params = GridLayout.LayoutParams(rowSpec, colSpec)
			
			params.setMargins(6, 6, 6, 6)
			params.width = 0
			params.height = MosaicFork.targetHeight 
			container.setLayoutParams(params)

			val attachment = images.get(i)
			val fileType = attachment.e().ordinal
			var imageUrl = attachment.c() as String
			
			if (fileType == 0) {
				if (MosaicFork.lowImage) {
					imageUrl = attachment.c() + "format=jpeg&width=500&height=500&"
				} else {
					imageUrl = attachment.c() + "format=jpeg&"
				}
					
				MGImages.setImage(imageView, imageUrl)
		
				if (container.getChildCount() == 1) {
					val playButton = ImageView(gridLayout.getContext())
					playButton.setImageResource(android.R.drawable.ic_media_play)
					playButton.setColorFilter(Color.WHITE)
					
					val circleBg = GradientDrawable()
					circleBg.setShape(GradientDrawable.OVAL)
					circleBg.setColor(Color.parseColor("#80000000"))
					playButton.setBackground(circleBg)
					
					val padding = (8 * gridLayout.getContext().getResources().getDisplayMetrics().density).toInt()
					playButton.setPadding(padding, padding, padding, padding)
					
					val btnSize = (52 * gridLayout.getContext().getResources().getDisplayMetrics().density).toInt()
					val btnParams = FrameLayout.LayoutParams(btnSize, btnSize)
					btnParams.gravity = Gravity.CENTER

					container.addView(playButton, btnParams)
				}
			} else {
				if (container.getChildCount() > 1 && container.getChildAt(1) !is TextView) container.removeViewAt(1) 
					
				if (imageUrl.lowercase().contains(".gif")) {
					if (MosaicFork.aniMode) imageUrl = imageUrl + "animated=true&format=webp&" 
					if (!MosaicFork.autoGif) imageUrl = imageUrl + "format=jpeg&" 
					if (MosaicFork.lowGif) imageUrl = imageUrl + "width=200&height=200&"
				} else {
					if (MosaicFork.lowImage) imageUrl = imageUrl + "width=500&height=500&"
				}

				MGImages.setImage(imageView, imageUrl)
			}
				
			var hasSpoilerView = false
			
			if (container.getChildCount() > 0 && container.getChildAt(container.getChildCount() - 1) is TextView) hasSpoilerView = true
				
			if ((shouldSpoilered || attachment.h())) {
				if (!hasSpoilerView) {
					if (!isOpened.contains(i)) {							
						val spoilerOverlay = TextView(gridLayout.getContext())
						spoilerOverlay.setText("SPOILER")
						spoilerOverlay.setTextColor(Color.WHITE)
						spoilerOverlay.setGravity(Gravity.CENTER)
						spoilerOverlay.setTypeface(Typeface.DEFAULT_BOLD)
						spoilerOverlay.setTextSize(13f)
						spoilerOverlay.setBackgroundColor(Color.parseColor("#FF2F3136"))
						spoilerOverlay.setOutlineProvider(object : ViewOutlineProvider() {
							override fun getOutline(view: View, outline: Outline) {
								outline.setRoundRect(0, 0, view.width, view.height, 8f)
							}
						})
						spoilerOverlay.setClipToOutline(true)

						spoilerOverlay.setOnClickListener { v ->
							(v.parent as? FrameLayout)?.removeView(v)
							isOpened.add(i)
						}
						container.addView(spoilerOverlay, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
					}
				}
			} else {
				if (hasSpoilerView) container.removeViewAt(container.getChildCount() - 1)
			}
				
			imageView.setOnClickListener { v ->
				val smv = PluginManager.plugins.get("SwipeMediaViewer")
				
				if (smv != null) {
					val method = smv.javaClass.getDeclaredMethod("launchGroup", Context::class.java, List::class.java, MessageAttachment::class.java).apply { isAccessible = true }
					method.invoke(null, v.context, images, attachment)
				} else {
					Companion.launch(v.context, attachment) // WidgetMedia.Companion
				}
			}
		}	
		
		val handler = this.evhandler

		if (handler != null) {
			this.itemView.setOnClickListener { v -> 
				handler.onMessageClicked(msg, false)
			}
			this.itemView.setOnLongClickListener { v ->
				handler.onMessageLongClicked(msg, "", false)
				true 
			}
		}
	}

	private fun getSpanSize(total: Int, position: Int): Int {
		if (total == 1) return 6
		if (total == 2) return 3

		if (total == 3) {
			return if (position < 2) 3 else 6
		}

		if (total >= 4 && total <= 6) {
			if (total == 4) return 3
			if (total == 5) if (position < 3) return 2 else 3
			if (total == 6) return 2
		}

		if (total >= 7 && total <= 9) {
			if (total == 7) if (position < 3) return 2 else 3
			if (total == 8) if (position < 6) return 2 else 3
			if (total == 9) return 2
		}

		if (total == 10) return if (position < 9)  2 else 6

		return 6
	}
}