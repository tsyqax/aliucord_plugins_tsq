package com.tsq.plugins

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
import com.discord.simpleast.code.CodeNode


@AliucordPlugin
class CopyBackTick: Plugin() {
	override fun start(context: Context) {
		val renderMethod by lazy { CodeNode::class.java.getDeclaredMethod("render", SpannableStringBuilder::class.java, Any::class.java) }
		patcher.patch(renderMethod, Hook { param ->
			try {
				val builder = param.args[0] as SpannableStringBuilder
				val node = param.thisObject as CodeNode<*>
				val content = node.content as CharSequence
				
				val end = builder.length
				val start = end - content.length
				
				if (start >= 0) {
					val clickSpan = object: ClickableSpan() {
						override fun onClick(widget: View) {
							Utils.setClipboard("backtick", content)
							Utils.showToast("copied!")
						}
						
						override fun updateDrawState(ds: TextPaint) {
							ds.isUnderlineText = false
						}
					}
					
					builder.setSpan(clickSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
				}	
				
			} catch (e: Exception) {
				logger.error("BackTick", e)
			}
		})
	}

    override fun stop(context: Context) { patcher.unpatchAll() }
}
