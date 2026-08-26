package com.tsq.plugins

import android.content.Context
import android.content.res.TypedArray
import android.content.res.Resources
import android.icu.text.PluralRules
import android.text.SpannableStringBuilder

import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.patcher.*;
import com.aliucord.settings.*;
import com.aliucord.api.PatcherAPI

import b.a.k.b
import com.discord.i18n.RenderContext
import com.discord.utilities.locale.LocaleManager
import com.discord.views.ToolbarTitleLayout
import java.util.Locale

object PluralFix {

	fun init(context: Context, patcher: PatcherAPI, logger: Logger) {
		val placeholderRegex = Regex("\\{[a-zA-Z]+\\}")
		
		val quantiStringMethod by lazy { b::class.java.getDeclaredMethod("f", CharSequence::class.java, Array<Any>::class.java, RenderContext::class.java) }
		patcher.patch(quantiStringMethod, Hook { param ->
			val isNoDistinctionLocale = PluralRules.forLocale(Locale.getDefault()).keywords.size == 1
			
			if (isNoDistinctionLocale) {
				val resultCharSequence = param.result as? CharSequence

				if (resultCharSequence != null) {
					val formatArgs = param.args[1] as? Array<*>
					val quantityStr = formatArgs?.getOrNull(0)?.toString() ?: "1"

					val builder = SpannableStringBuilder(resultCharSequence)

					val matcher = placeholderRegex.toPattern().matcher(builder)
					if (matcher.find()) {
						val start = matcher.start()
						val end = matcher.end()
						builder.replace(start, end, quantityStr)
					}
					param.result = builder as CharSequence
				}
			}
		})
		
		val getTextMethod by lazy { Resources::class.java.getDeclaredMethod("getText", Int::class.javaPrimitiveType) }
		val getTextMethod2 by lazy { TypedArray::class.java.getDeclaredMethod("getText", Int::class.javaPrimitiveType) }
		val getTextMethod3 by lazy { TypedArray::class.java.getDeclaredMethod("getString", Int::class.javaPrimitiveType) }
		
        val emojiTextHook = Hook { param ->
			val currentLocale = LocaleManager().getPrimaryLocale(context)
			
			if (currentLocale.language == "ko") {
				val originalStr = param.result?.toString()

				if (originalStr != null && originalStr.contains("이모티콘")) {
					var fixedStr = originalStr
					
					fixedStr = fixedStr.replace(Regex("이모티콘이에요"), "이모지예요")
					fixedStr = fixedStr.replace(Regex("이모티콘은"), "이모지는")
					fixedStr = fixedStr.replace(Regex("이모티콘을"), "이모지를")
					fixedStr = fixedStr.replace(Regex("이모티콘이"), "이모지가")
					fixedStr = fixedStr.replace(Regex("이모티콘과"), "이모지와")
					fixedStr = fixedStr.replace(Regex("이모티콘으로"), "이모지로")

					fixedStr = fixedStr.replace("이모티콘", "이모지")

					param.result = fixedStr
				}
			}
		}
		patcher.patch(getTextMethod, emojiTextHook)
		patcher.patch(getTextMethod2, emojiTextHook)
		patcher.patch(getTextMethod3, emojiTextHook)
	}

}
