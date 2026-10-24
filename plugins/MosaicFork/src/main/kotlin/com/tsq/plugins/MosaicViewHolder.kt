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
    //private val isOpened = hashSetOf<Int>()
    private var evhandler: WidgetChatListAdapter.EventHandler? = null
	private var realWidth: Int = 0;
	private var playButtonSizePx: Int = 0
    private var playButtonPaddingPx: Int = 0

    constructor(gridLayout: GridLayout, adapter: WidgetChatListAdapter) : super(gridLayout, adapter) {
        this.gridLayout = gridLayout
        initGrid()
    }

    constructor(itemView: View, gridLayout: GridLayout, adapter: WidgetChatListAdapter) : super(itemView, adapter) {
        this.gridLayout = gridLayout
        initGrid()
    }
	
	private fun initGrid() {
        val dm = gridLayout.context.resources.displayMetrics
        playButtonSizePx = (52 * dm.density).toInt()
        playButtonPaddingPx = (8 * dm.density).toInt()
        updateGridSize()
        evhandler = this.adapter?.eventHandler
    }

	fun getGridLayout() = this.gridLayout
	
	private val shouldEnableSpoilerMethod by lazy {
		runCatching {
			PluginManager.plugins.get("BetterSpoiler")?.javaClass
				?.getDeclaredMethod("shouldEnableSpoiler", Message::class.java, Long::class.javaPrimitiveType)
				?.apply { isAccessible = true }
		}.getOrNull()
	}

	private val swipeMediaLaunchMethod by lazy {
		runCatching {
			PluginManager.plugins.get("SwipeMediaViewer")?.javaClass
				?.getDeclaredMethod("launchGroup", Context::class.java, List::class.java, MessageAttachment::class.java)
				?.apply { isAccessible = true }
		}.getOrNull()
	}
	
	private val sharedOutlineProvider = object : ViewOutlineProvider() {
		override fun getOutline(view: View, outline: Outline) {
			outline.setRoundRect(0, 0, view.width, view.height, 8f)
		}
	}

    private fun updateGridSize() {
        val screenWidth = gridLayout.context.resources.displayMetrics.widthPixels
        val targetWidth = (screenWidth * MosaicFork.inputWidth).toInt().coerceAtLeast(1)
        realWidth = targetWidth
		
        gridLayout.layoutParams?.let { params ->
            if (params.width != targetWidth) {
				params.width = targetWidth
                gridLayout.layoutParams = params
            }
        }
    }
	
    private fun getTileHeights(total: Int): IntArray {
		fun squareHeight(span: Int): Int = ((realWidth.toLong() * span / 6) - 12L).toInt().coerceAtLeast(1)

		val tileHeights = IntArray(total) { i -> squareHeight(getSpanSize(total, i)) }

		if (total > 1 && getSpanSize(total, 0) == 6) {
			val halfWidthHeight = squareHeight(3) 
			tileHeights[0] = minOf(tileHeights[0], halfWidthHeight)
		}

		return tileHeights
	}
	
	private fun createTileLayoutParams(spanSize: Int, tileHeight: Int): GridLayout.LayoutParams {
        val rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1)
        val colSpec = GridLayout.spec(GridLayout.UNDEFINED, spanSize, spanSize.toFloat())

        return GridLayout.LayoutParams(rowSpec, colSpec).apply {
            setMargins(6, 6, 6, 6)
            width = 0
            height = tileHeight
        }
    }

	override fun onConfigure(position: Int, data: ChatListEntry) {
		val mosaicEntry = data as? MosaicEntry ?: return
		
		val images = mosaicEntry.images
		val msg = mosaicEntry.msg
		val total = images.size
		
		updateGridSize()
		gridLayout.setColumnCount(6)

		val currentChildCount = gridLayout.getChildCount()
		var gifCount = 0

		if (currentChildCount > total) gridLayout.removeViews(total, currentChildCount - total)
		
		val guildId = StoreStream.getGuildSelected().getSelectedGuildId()
		val shouldSpoilered = if (PluginManager.isPluginEnabled("BetterSpoiler")) {
			PluginManager.plugins.get("BetterSpoiler")?.let { bs ->
				runCatching { shouldEnableSpoilerMethod?.invoke(bs, msg, guildId) as? Boolean }.getOrNull()
			} ?: false
		} else false
		
		val tileHeights = getTileHeights(total)

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
					background = GradientDrawable().apply {
						setShape(GradientDrawable.OVAL)
						setColor(Color.parseColor("#80000000"))
					}
					setPadding(playButtonPaddingPx, playButtonPaddingPx, playButtonPaddingPx, playButtonPaddingPx)
					visibility = View.GONE
					tag = "PLAY_BTN"
				}
				
				val btnParams = FrameLayout.LayoutParams(playButtonSizePx, playButtonSizePx).apply {
					gravity = Gravity.CENTER
				}

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
				}

				container.addView(imageView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
				container.addView(playButton, btnParams)
				container.addView(spoilerOverlay, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
				
				//gridLayout.addView(container)
			}

            val tileHeight = tileHeights[i]
            if (container.parent == null) {
				gridLayout.addView(container, createTileLayoutParams(spanSize, tileHeight))
				container.tag = spanSize
			} else {
				val currentParams = container.layoutParams as? GridLayout.LayoutParams
				if (currentParams == null || currentParams.height != tileHeight || (container.tag as? Int) != spanSize) {
					container.layoutParams = createTileLayoutParams(spanSize, tileHeight)
					container.tag = spanSize
				}
			}

			val attachment = images.get(i)
			val fileType = attachment.e().ordinal
			var imageUrl = attachment.c() as String
			
			val playBtn = container.findViewWithTag<ImageView>("PLAY_BTN")
			val spoilerView = container.findViewWithTag<TextView>("SPOILER_VIEW")
			
			if (fileType == 0) {
				imageUrl = if (MosaicFork.lowImage) {
					imageUrl + "format=jpeg&width=500&height=500&"
				} else {
					imageUrl + "format=jpeg&"
				}
				MGImages.setImage(imageView, imageUrl)
				playBtn?.visibility = View.VISIBLE
				playBtn?.bringToFront()
			} else {
				if (imageUrl.contains(".gif", ignoreCase = true)) {
					if (MosaicFork.aniMode) imageUrl = imageUrl + "animated=true&format=webp&"
					if (!MosaicFork.autoGif) imageUrl = imageUrl + "format=jpeg&"
					if (MosaicFork.lowGif) imageUrl = imageUrl + "width=200&height=200&"
				} else if (MosaicFork.lowImage) {
					imageUrl = imageUrl + "width=500&height=500&"
				}

				MGImages.setImage(imageView, imageUrl)
				playBtn?.visibility = View.GONE
			}


			val isSpoiler = shouldSpoilered || attachment.h()

			if (isSpoiler) {
				spoilerView?.setOnClickListener {
					spoilerView.visibility = View.GONE
				}
				spoilerView?.visibility = View.VISIBLE
				spoilerView?.bringToFront()
			} else {
				spoilerView?.setOnClickListener(null)
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
        if (total == 3) return if (position == 0) 6 else 3
        if (total == 4) return 3

        if (total == 5 || total == 8) return if (position < 2) 3 else 2

        if (total == 6) return 2
        if (total == 7) return if (position == 0) 6 else 2
        if (total == 9) return 2
        if (total == 10) return if (position == 0) 6 else 2
        return 6
    }
}
