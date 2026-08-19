package com.tsq.plugins

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

import com.discord.widgets.chat.list.WidgetChatList
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapter
import com.discord.widgets.chat.list.entries.MessageEntry

import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.patcher.*;
import com.aliucord.settings.*;
import com.aliucord.api.PatcherAPI
import com.aliucord.utils.DimenUtils

object ForumLine {
	fun init(context: Context, patcher: PatcherAPI, logger: Logger) {
		try {
			val lineMethod by lazy { WidgetChatList::class.java.getDeclaredMethod("onViewBoundOrOnResume") }

			patcher.patch(lineMethod, Hook { param ->
				try {
					val thiz = param.thisObject as WidgetChatList
					val getBindingMethod = thiz.javaClass.getDeclaredMethod("getBinding") //private
					getBindingMethod.isAccessible = true
					val bindingObj = getBindingMethod.invoke(thiz)
						
					if (bindingObj == null) return@Hook
						
					val recyclerField = bindingObj.javaClass.getDeclaredField("b")
					recyclerField.isAccessible = true
					val recyclerView = recyclerField.get(bindingObj) as? RecyclerView

					val adapterField = thiz.javaClass.getDeclaredField("adapter")
					adapterField.isAccessible = true
					val chatAdapter = adapterField.get(thiz) as? WidgetChatListAdapter

					if (recyclerView != null && chatAdapter != null) {
						val tagKey = 0x7FF232F

						if (recyclerView.getTag(tagKey) == null) {
							recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
								private val paint = Paint().apply {
									color = Color.parseColor("#4E5058")
									strokeWidth = DimenUtils.dpToPx(5).toFloat()
								}
								
								override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
									val position = parent.getChildAdapterPosition(view)
									if (position == RecyclerView.NO_POSITION) return

									try {
										val entries = chatAdapter.data.list as? List<*>
											if (entries != null && position < entries.size) {
												val entry = entries[position]
												if (entry is MessageEntry && entry.isGuildForumPostFirstMessage) {
												outRect.bottom = DimenUtils.dpToPx(16)
											}
										}
									} catch (e: Exception) {
										logger.error("forumLine: 1", e)
									}
								}

								override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
									try {
										val entries = chatAdapter.getData().getList() as? List<*>
										if (entries == null) return
										
										for (i in 0 until parent.childCount) {
											val child = parent.getChildAt(i)
											var position = parent.getChildAdapterPosition(child)
											if (position == RecyclerView.NO_POSITION) continue

											if (position < entries.size) {
												val entry = entries.get(position)
												if (entry is MessageEntry && entry.isGuildForumPostFirstMessage) {
													val startX = parent.getPaddingLeft().toFloat()
													val endX = parent.getWidth() - parent.getPaddingRight().toFloat()
														
													val bottom = child.getBottom() + DimenUtils.dpToPx(8)
													val top = child.getTop().toFloat()
														
													c.drawLine(startX, bottom.toFloat(), endX, bottom.toFloat(), paint)
												}
											}
										}
									} catch (e: Exception) {
										logger.error("forumLine: 2", e)
									}
								}
							})

							recyclerView.setTag(tagKey, true)
						}
					}
				} catch (e: Exception) {
					logger.error("forumLine: 3", e)
				}
			})
		} catch (e: Exception) {
			logger.error("forumLine: 0", e)
		}
	}
}
