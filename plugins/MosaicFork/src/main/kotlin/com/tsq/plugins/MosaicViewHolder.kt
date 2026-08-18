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
	
	private val shouldEnableSpoilerMethod by lazy {
        PluginManager.plugins.get("BetterSpoiler")?.javaClass?.getDeclaredMethod("shouldEnableSpoiler", Message::class.java, Long::class.javaPrimitiveType)?.apply { isAccessible = true }
    }
	
	private val swipeMediaLaunchMethod by lazy {
		PluginManager.plugins.get("SwipeMediaViewer")?.javaClass?.getDeclaredMethod("launchGroup", Context::class.java, List::class.java, MessageAttachment::class.java)?.apply { isAccessible = true }
	}
	
	private val sharedOutlineProvider = object : ViewOutlineProvider() {
		override fun getOutline(view: View, outline: Outline) {
			outline.setRoundRect(0, 0, view.width, view.height, 8f)
		}
	}

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
		
		if (PluginManager.isPluginEnabled("BetterSpoiler")) {
			shouldSpoilered = PluginManager.plugins.get("BetterSpoiler")?.let { bs ->
				shouldEnableSpoilerMethod?.invoke(bs, msg, guildId) as? Boolean
			} ?: false
		}

		for (i in 0 until total) {
			var container: FrameLayout? = null
			var imageView: SimpleDraweeView? = null
			val spanSize = getSpanSize(total, i)

			if (i < currentChildCount) {
				container = gridLayout.getChildAt(i) as FrameLayout
				imageView = container.findViewWithTag<SimpleDraweeView?>("IMAGE_VIEW")
			} else {
				container = FrameLayout(gridLayout.context)
				imageView = SimpleDraweeView(gridLayout.context).apply {
					setScaleType(ImageView.ScaleType.CENTER_CROP)
					setOutlineProvider(sharedOutlineProvider)
					setClipToOutline(true)
					tag = "IMAGE_VIEW"
				}
				
				val playButton = ImageView(gridLayout.context).apply {
					setImageResource(android.R.drawable.ic_media_play)
					setColorFilter(Color.WHITE)					
					val circleBg = GradientDrawable().apply {
						setShape(GradientDrawable.OVAL)
						setColor(Color.parseColor("#80000000"))
					}
					setBackground(circleBg)
					val padding = (8 * gridLayout.context.resources.displayMetrics.density).toInt()
					setPadding(padding, padding, padding, padding)
					visibility = View.GONE
					tag = "PLAY_BTN"
				}
				
				val btnSize = (52 * gridLayout.context.resources.displayMetrics.density).toInt()
				val btnParams = FrameLayout.LayoutParams(btnSize, btnSize)
				btnParams.gravity = Gravity.CENTER

				val spoilerOverlay = TextView(gridLayout.context).apply {
					text = "SPOILER"
					setTextColor(Color.WHITE)
					gravity = Gravity.CENTER
					typeface = Typeface.DEFAULT_BOLD
					textSize = 13f
					setBackgroundColor(Color.parseColor("#FF2F3136"))
					setOutlineProvider(sharedOutlineProvider)
					setClipToOutline(true)
					visibility = View.GONE
					tag = "SPOILER_VIEW" 
					setOnClickListener { v ->
						v.visibility = View.GONE 
						isOpened.add(i)
					}
				}
				
				container.addView(imageView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
				container.addView(playButton, btnParams)
				container.addView(spoilerOverlay, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
				
				gridLayout.addView(container)
			}

			val rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1)
			val colSpec = GridLayout.spec(GridLayout.UNDEFINED, spanSize, 1f)
			val params = GridLayout.LayoutParams(rowSpec, colSpec).apply {
				setMargins(6, 6, 6, 6)
				width = 0
				height = MosaicFork.targetHeight
			}
			container.setLayoutParams(params)

			val attachment = images.get(i)
			val fileType = attachment.e().ordinal
			var imageUrl = attachment.c() as String
			
			val playBtn = container.findViewWithTag<ImageView>("PLAY_BTN")
			val spoilerView = container.findViewWithTag<TextView>("SPOILER_VIEW")
			
			if (fileType == 0) {
				if (MosaicFork.lowImage) {
					imageUrl = attachment.c() + "format=jpeg&width=500&height=500&"
				} else {
					imageUrl = attachment.c() + "format=jpeg&"
				}
					
				MGImages.setImage(imageView, imageUrl)
				playBtn?.visibility = View.VISIBLE
				playBtn?.bringToFront()
				
			} else {
				if (imageUrl.lowercase().contains(".gif")) {
					if (MosaicFork.aniMode) imageUrl = imageUrl + "animated=true&format=webp&" 
					if (!MosaicFork.autoGif) imageUrl = imageUrl + "format=jpeg&" 
					if (MosaicFork.lowGif) imageUrl = imageUrl + "width=200&height=200&"
				} else {
					if (MosaicFork.lowImage) imageUrl = imageUrl + "width=500&height=500&"
				}
		
				MGImages.setImage(imageView, imageUrl)
				playBtn?.visibility = View.GONE
			}

			if (shouldSpoilered || attachment.h()) {
				if (!isOpened.contains(i)) {
					spoilerView?.visibility = View.VISIBLE
					spoilerView?.bringToFront()
				}
			} else {
				spoilerView?.visibility = View.GONE
			}
				
			imageView.setOnClickListener { v ->
				val smv = PluginManager.plugins.get("SwipeMediaViewer")
				
				if (smv != null && PluginManager.isPluginEnabled("SwipeMediaViewer")) {
					swipeMediaLaunchMethod?.invoke(null, v.context, images, attachment)
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
