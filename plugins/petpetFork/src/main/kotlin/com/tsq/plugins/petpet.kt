package com.tsq.plugins

import android.content.Context
import android.os.Bundle
import android.net.Uri
import android.view.View
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

import com.aliucord.Http
import com.aliucord.Utils
import com.aliucord.Logger
import com.aliucord.PluginManager
import com.aliucord.api.SettingsAPI
import com.aliucord.fragments.SettingsPage
import com.aliucord.views.TextInput
import com.aliucord.views.Button
import com.aliucord.views.DangerButton

import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.CommandsAPI
import com.aliucord.entities.Plugin

import com.discord.api.commands.ApplicationCommandType
import com.discord.utilities.icon.IconUtils

import java.io.File
import java.io.FileOutputStream

@AliucordPlugin
class petpetFork: Plugin() {
	init { 
		settingsTab = SettingsTab(PSettings::class.java, SettingsTab.Type.PAGE).withArgs(settings)
	}
	
	// ----- settings start -----
	class PSettings(private val settings: SettingsAPI) : SettingsPage() {
		
		override fun onViewCreated(view: View, bundle: Bundle?) {
			super.onViewCreated(view, bundle)
			setActionBarTitle("petpetFork")
			setActionBarSubtitle("Settings!")
			
			val context = view.context
			val layout = getLinearLayout()

			val res_input = TextInput(context, "Resolution (default: 128)", settings.getInt("pet_res", 128).toString())
			val w_factor_input = TextInput(context, "Base Width Factor (default: 0.73)", settings.getFloat("pet_w_factor", 0.73f).toString())
			val w_j_input = TextInput(context, "Width J-Timeline Weight (default: 0.02)", settings.getFloat("pet_w_j", 0.02f).toString())
			val h_factor_input = TextInput(context, "Base Height Factor (default: 0.73)", settings.getFloat("pet_h_factor", 0.73f).toString())
			val h_j_input = TextInput(context, "Height J-Timeline Weight (default: 0.072)", settings.getFloat("pet_h_j", 0.072f).toString())
			val ox_input = TextInput(context, "Offset X Margin (default: 0.14)", settings.getFloat("pet_ox", 0.14f).toString())
			val oy_input = TextInput(context, "Offset Y Margin (default: 0.09)", settings.getFloat("pet_oy", 0.09f).toString())
			val delay_input = TextInput(context, "Frame Delay ms (default: 50, min: 20)", settings.getInt("pet_delay", 50).toString())
			val repeat_input = TextInput(context, "Loop Count (0 for Infinite)", settings.getInt("pet_repeat", 0).toString())
			val bg_input = TextInput(context, "Background Color (Hex or TRANSPARENT)", settings.getString("pet_bg", "TRANSPARENT"))

			val saveButton = Button(context)
			saveButton.setText("Save")
			saveButton.setOnClickListener({
				val resVal = runCatching { res_input.editText.text.toString().toInt() }.getOrDefault(128)
				val wFactorVal = runCatching { w_factor_input.editText.text.toString().toFloat() }.getOrDefault(0.73f)
				val wJVal = runCatching { w_j_input.editText.text.toString().toFloat() }.getOrDefault(0.02f)
				val hFactorVal = runCatching { h_factor_input.editText.text.toString().toFloat() }.getOrDefault(0.73f)
				val hJVal = runCatching { h_j_input.editText.text.toString().toFloat() }.getOrDefault(0.072f)
				val oxVal = runCatching { ox_input.editText.text.toString().toFloat() }.getOrDefault(0.14f)
				val oyVal = runCatching { oy_input.editText.text.toString().toFloat() }.getOrDefault(0.09f)
				val delayVal = runCatching { delay_input.editText.text.toString().toInt() }.getOrDefault(50)
				val bgVal = runCatching { val input = bg_input.editText.text.toString(); if (input.equals("TRANSPARENT", ignoreCase = true)) { "TRANSPARENT" } else { val fixedInput = if (input.startsWith("#")) input else "#$input"; Color.parseColor(fixedInput); fixedInput}}.getOrDefault("TRANSPARENT")
				val repeatVal = runCatching { repeat_input.editText.text.toString().toInt().coerceAtLeast(0) }.getOrDefault(0)
				
				settings.apply {
					setInt("pet_res", resVal)
					setFloat("pet_w_factor", wFactorVal)
					setFloat("pet_w_j", wJVal)
					setFloat("pet_h_factor", hFactorVal)
					setFloat("pet_h_j", hJVal)
					setFloat("pet_ox", oxVal)
					setFloat("pet_oy", oyVal)
					setInt("pet_delay", delayVal)
					setInt("pet_repeat", repeatVal)
					setString("pet_bg", bgVal)
				}
				Utils.showToast("SaveSave is done!", false)
			})
			
			val reset_button = DangerButton(context)
			reset_button.setText("Reset to Default")

			reset_button.setOnClickListener { v ->
				settings.apply {
					setInt("pet_res", 128)
					setFloat("pet_w_factor", 0.73f)
					setFloat("pet_w_j", 0.02f)
					setFloat("pet_h_factor", 0.73f)
					setFloat("pet_h_j", 0.072f)
					setFloat("pet_ox", 0.14f)
					setFloat("pet_oy", 0.09f)
					setInt("pet_delay", 40)
					setInt("pet_repeat", 0)
					setString("pet_bg", "TRANSPARENT")
				}
		
				res_input.editText.setText("128")
				w_factor_input.editText.setText("0.73")
				w_j_input.editText.setText("0.02")
				h_factor_input.editText.setText("0.73")
				h_j_input.editText.setText("0.072")
				ox_input.editText.setText("0.14")
				oy_input.editText.setText("0.09")
				delay_input.editText.setText("40")
				bg_input.editText.setText("TRANSPARENT")
				repeat_input.editText.setText("0")
				
				Utils.showToast("ResetReset is done!", false)
			}
			
			layout.apply {
				addView(res_input)
				addView(w_factor_input)
				addView(w_j_input)
				addView(h_factor_input)
				addView(h_j_input)
				addView(ox_input)
				addView(oy_input)
				addView(delay_input)
				addView(repeat_input)
				addView(bg_input)
				addView(saveButton)
				addView(reset_button)
			}
		}
	}
	// ----- settings end -----
	
	override fun start(context: Context) {
		if (PluginManager.plugins.containsKey("PetPet") && PluginManager.isPluginEnabled("PetPet")) {
            Utils.showToast("If two, Error? Incompatible? maybe.", true)
            PluginManager.disablePlugin("PetPet")
        }

        commands.registerCommand(
            "petpet",
            "You are ready to pet someone.",
            listOf(
				Utils.createCommandOption(
					ApplicationCommandType.USER, 
					"name", 
					"The user to pet", 
					null, 
					true,
					false
				)
			),
			{ ctx ->
				try {
					val user = ctx.getRequiredUser("name")
					val avatar = IconUtils.getForUser(user)
					val file = makeGifFile(avatar, context)
					ctx.addAttachment(Uri.fromFile(file).toString(), "petpet.gif")
					CommandsAPI.CommandResult("")
				} catch (t: Throwable) {
					logger.error("Thread creation failed: ", t)
                    CommandsAPI.CommandResult("Error: " + t.message , null, false)
					
                }
            }
        )
		
		commands.registerCommand(
            "peturl",
            "petpet for URL",
            listOf(
				Utils.createCommandOption(
					ApplicationCommandType.STRING, 
					"url", 
					"The url to pet", 
					null, 
					true,
					false
				)
			),
            { ctx ->
				try {
					val target = ctx.getRequiredString("url")
					val file = makeGifFile(target, context)
					ctx.addAttachment(Uri.fromFile(file).toString(), "petpet.gif")
					CommandsAPI.CommandResult("")
				} catch (t: Throwable) {
					logger.error("Thread creation failed: ", t)
                    CommandsAPI.CommandResult("Error: " + t.message , null, false)
                }
            }
        )
    }
	
	private fun splitSpriteToFrames(context: Context): Array<Bitmap> {
		val resId = resources?.getIdentifier("petpet_sprite", "drawable", "com.tsq.plugins")
		
		val options = BitmapFactory.Options()
		options.inScaled = false
		
		val spriteBitmap = BitmapFactory.decodeResource(resources, resId!!, options)
		
		val frameWidth = 112 
		val frameHeight = 112
		
		val handFrames = Array<Bitmap>(5) { i ->
			Bitmap.createBitmap(spriteBitmap, i * frameWidth, 0, frameWidth, frameHeight)
		}

		spriteBitmap.recycle()
		
		return handFrames
	}
	
	
	private fun createPetPetFrames(avatarBitmap: Bitmap, handFrames: Array<Bitmap>): Array<Bitmap?> {
		val FRAMES = 5

		val completedFrames = arrayOfNulls<Bitmap>(FRAMES)
		
		val resolution = settings.getInt("pet_res", 128)

		val petWFactor = settings.getFloat("pet_w_factor", 0.73f)
		val petWJ = settings.getFloat("pet_w_j", 0.02f)
		val petHFactor = settings.getFloat("pet_h_factor", 0.73f)
		val petHJ = settings.getFloat("pet_h_j", 0.072f)
		val petOX = settings.getFloat("pet_ox", 0.14f)
		val petOY = settings.getFloat("pet_oy", 0.09f)
		
		val petBgStr = settings.getString("pet_bg", "TRANSPARENT")
		var bgColor = Color.TRANSPARENT
		
		if (!petBgStr.equals("TRANSPARENT", ignoreCase = true)) {
			bgColor = Color.parseColor(petBgStr)
		}
		
		val handDestRect = Rect(0, 0, resolution, resolution)
		val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
		val jTimeline = arrayOf(0, 2, 4, 3, 1)

		for (i in 0 until FRAMES) {
			val canvasBitmap = Bitmap.createBitmap(resolution, resolution, Bitmap.Config.ARGB_8888)
			val canvas = Canvas(canvasBitmap)
			//canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
			canvas.drawColor(bgColor)
			
			val j = jTimeline[i]
			val widthFactor = petWFactor + (j * petWJ)
			val heightFactor = petHFactor - (j * petHJ)
			
			val offsetXFactor = ((1.0 - widthFactor) * 0.5) + petOX
			val offsetYFactor = (1.0 - heightFactor) - petOY

			val left = Math.round(resolution * offsetXFactor)
			val top = Math.round(resolution * offsetYFactor)
			val right = left + Math.round(resolution * widthFactor)
			val bottom = top + Math.round(resolution * heightFactor)
			
			val avatarDestRect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())

			val avatarSrcRect = Rect(0, 0, avatarBitmap.getWidth(), avatarBitmap.getHeight())
			canvas.drawBitmap(avatarBitmap, avatarSrcRect, avatarDestRect, paint)
			
			val handSrcRect = Rect(0, 0, handFrames[i].getWidth(), handFrames[i].getHeight())
			canvas.drawBitmap(handFrames[i], handSrcRect, handDestRect, paint)

			completedFrames[i] = canvasBitmap
		}
		
		return completedFrames
	}
	
	private fun makeGifFile(avatarUrl: String, context: Context): File {
		var finalUrl = avatarUrl
		if (avatarUrl.lowercase().endsWith(".webp")) finalUrl = avatarUrl.replace(".webp", ".png")
		
		val res = Http.Request(finalUrl).execute()
		val avatarBitmap = BitmapFactory.decodeStream(res.stream())
		res.stream().close()

		val handFrames = splitSpriteToFrames(context)
		val completedFrames = createPetPetFrames(avatarBitmap, handFrames)
		val outputFile = File.createTempFile("petpet", ".gif", context.cacheDir)
		val userRepeat = settings.getInt("pet_repeat", 0)
		var userDelay = settings.getInt("pet_delay", 40)
		val bgColor = settings.getString("pet_bg", "TRANSPARENT")
		val encoder = AnimatedGifEncoder()
		
		if (userDelay < 20) userDelay = 20
		
		encoder.apply {
			start(FileOutputStream(outputFile))
			setDelay(userDelay) 
			setRepeat(userRepeat)
			setDispose(2)
		}
		
		if (bgColor.equals("TRANSPARENT", ignoreCase = true)) encoder.setTransparent(Color.TRANSPARENT)
		for (i in 0 until 5) encoder.addFrame(completedFrames[i])
		
		encoder.finish()
		avatarBitmap.recycle()
		
		for (i in 0 until 5) {
			handFrames[i].recycle()
			completedFrames[i]?.recycle()
		}

		outputFile.deleteOnExit()
		return outputFile
	}

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
