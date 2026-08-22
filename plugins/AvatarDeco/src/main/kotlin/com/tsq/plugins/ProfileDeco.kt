package com.tsq.plugins

import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.content.ContextWrapper
import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import com.aliucord.Http
import com.aliucord.Logger
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
import com.discord.widgets.user.usersheet.WidgetUserSheet
import com.discord.widgets.user.profile.UserProfileHeaderView
import com.facebook.drawee.view.SimpleDraweeView
import org.json.JSONArray
import org.json.JSONObject
import java.util.HashMap
import kotlin.concurrent.thread

@AliucordPlugin
class ProfileDeco : Plugin() {

	override fun start(context: Context) {
		
		val target by lazy { WidgetUserSheet::class.java.getDeclaredMethod("onCreateView", LayoutInflater::class.java, ViewGroup::class.java, Bundle::class.java) }
		
		patcher.patch(target, Hook { param -> 
			val us = param.thisObject as WidgetUserSheet
			val rootView = param.result as ViewGroup
			val userId = us.getArgumentsOrDefault().getLong("ARG_USER_ID")

			thread {
				try {
					val targetUrl = "/users/$userId/profile"
					val response = Http.Request.newDiscordRequest(targetUrl, "GET").execute()
					val content = response.text()

					var skuId = ""

					if (!content.isNullOrEmpty()) {
						val profileJson = JSONObject(content)
						if (profileJson.has("user_profile")) {
							val userProfileObj = profileJson.getJSONObject("user_profile")
							if (userProfileObj.has("profile_effect")) {
								val profileEffectObj = userProfileObj.getJSONObject("profile_effect")
								if (profileEffectObj.has("sku_id")) {
									skuId = profileEffectObj.getString("sku_id")
									logger.info("sku_id: $skuId")
								}
							}
						}
						
						if (skuId.isEmpty()) return@thread
						val storeUrl = "/collectibles-products/$skuId"
						val storeResponse = Http.Request.newDiscordRequest(storeUrl, "GET").execute()
						val storeContent = storeResponse.text()
						
						if (!storeContent.isNullOrEmpty()) {
							val storeJson = JSONObject(storeContent)
							if (storeJson.has("items")) {
								val itemsArray = storeJson.getJSONArray("items")
								if (itemsArray.length() > 0) {
									val itemObj = itemsArray.getJSONObject(0)
									if (itemObj.has("reducedMotionSrc") && !itemObj.isNull("reducedMotionSrc")) {
										val thumbnail = itemObj.getString("reducedMotionSrc")
										applyDecorationView(rootView, thumbnail)
									}
								}
							}
						}

					}
				} catch (e: Exception) {}
			}
		})
		
		patcher.patch(UserProfileHeaderView::class.java.getDeclaredConstructor(Context::class.java, AttributeSet::class.java), Hook { param ->
			try {
				val headerView = param.thisObject as UserProfileHeaderView
				val bindingField = UserProfileHeaderView::class.java.getDeclaredField("binding").apply {
					isAccessible = true
				}
				val bindingObj = bindingField.get(headerView) ?: return@Hook

				val bannerField = bindingObj.javaClass.getDeclaredField("c").apply {
					isAccessible = true
				}
				val bannerView = bannerField.get(bindingObj) as? View ?: return@Hook

				bannerView.post {
					val isCustomBanner = if (bannerView is SimpleDraweeView) {
						bannerView.hasHierarchy() && bannerView.topLevelDrawable != null && 
						bannerView.topLevelDrawable.javaClass.name.contains("Bitmap", ignoreCase = true)
					} else {
						false
					}
				
					val params = bannerView.layoutParams
					if (!isCustomBanner && params != null && bannerView.height > 0) {
						val params = bannerView.layoutParams
						params.height = 256
						bannerView.layoutParams = params
						bannerView.requestLayout()
					}
				}
			} catch (e: Exception) {}
		})
	}
	
	private fun applyDecorationView(rootView: ViewGroup, introUrl: String, loopUrl: String = "", duration: Long = 0L) {
		val context = rootView.context
		val activity = context.findActivity() ?: return 

		rootView.post {
			try {
				if (!rootView.isAttachedToWindow) return@post
				val targetWidth = rootView.width
				val targetHeight = rootView.height

				val targetY = rootView.y

				if (targetWidth <= 0 || targetHeight <= 0) return@post

				val realContainer = if (rootView.childCount > 0) {
					rootView.getChildAt(0) as? ViewGroup ?: rootView
				} else {
					rootView
				}

				val decoViewTagKey = 0x7FFFFFEF
				
				val oldContainer = realContainer.findViewById<View>(decoViewTagKey)
				if (oldContainer != null) realContainer.removeView(oldContainer)

				val effectLayout = FrameLayout(context).apply {
					id = decoViewTagKey

					val params = FrameLayout.LayoutParams(targetWidth, targetHeight).apply {
						topMargin = targetY.toInt()
					}
					layoutParams = params

					translationY = targetY
				}

				val introImageView = SimpleDraweeView(context).apply {
					layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
					isClickable = false
				}

				if (loopUrl.isNotEmpty()) {
					val loopImageView = SimpleDraweeView(context).apply {
						layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
						visibility = View.GONE
						isClickable = false
					}

					effectLayout.addView(loopImageView)
					effectLayout.addView(introImageView)
					realContainer.addView(effectLayout)

					introImageView.setImageURI(Uri.parse(introUrl + ".png?passthrough=true"))
					loopImageView.setImageURI(Uri.parse(loopUrl + ".png?passthrough=true"))

					Handler(Looper.getMainLooper()).postDelayed({
						if (effectLayout.isAttachedToWindow) {
							introImageView.visibility = View.GONE
							loopImageView.visibility = View.VISIBLE
						}
					}, duration)

				} else {
					effectLayout.addView(introImageView)
					realContainer.addView(effectLayout)

					introImageView.setImageURI(Uri.parse(introUrl))
				}

				effectLayout.isClickable = false
				effectLayout.isFocusable = false

				effectLayout.setOnTouchListener { view, event ->
					view.animate().alpha(0.3f).setDuration(300L).start()
					false
				}


			} catch (e: Exception) {
				logger.error(e)
			}
		}
	}

	private fun Context.findActivity(): Activity? {
		var context = this
		while (context is ContextWrapper) {
			if (context is Activity) return context
			context = context.baseContext
		}
		return null
	}

	override fun stop(context: Context) {
		patcher.unpatchAll()
	}
}
