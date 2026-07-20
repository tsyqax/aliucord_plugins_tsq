package com.tsq.plugins;

import android.content.Context;
import android.os.Bundle;
import android.net.Uri;
import android.view.View;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import androidx.annotation.NonNull; 

import com.aliucord.Http;
import com.aliucord.Main;
import com.aliucord.Utils;
import com.aliucord.Logger;
import com.aliucord.PluginManager;
import com.aliucord.api.SettingsAPI;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.views.TextInput;
import com.aliucord.views.Button;

import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;

import com.aliucord.entities.CommandContext;
import com.aliucord.entities.Plugin;

import com.discord.api.commands.ApplicationCommandType;
import com.discord.utilities.icon.IconUtils;
import com.discord.views.CheckedSetting;

import java.util.Arrays;
import java.io.File;
import java.io.FileOutputStream;
import java.util.function.Supplier;

import com.tsq.plugins.AnimatedGifEncoder;

@AliucordPlugin
@SuppressWarnings("unused")
public class petpetFork extends Plugin {
    public static final Logger logger = new Logger("petpetFork");

	public petpetFork() { 
		settingsTab = new SettingsTab(PSettings.class, SettingsTab.Type.PAGE).withArgs(settings);
	}
	
	// ----- settings start -----
	public static class PSettings extends SettingsPage {
		private final SettingsAPI settings;
		
		public PSettings(SettingsAPI settings) {
			this.settings = settings;
		}
		
		@Override
		public void onViewCreated(View view, Bundle bundle) {
			super.onViewCreated(view, bundle);
			setActionBarTitle("petpetFork");
			setActionBarSubtitle("Settings!");

			var context = view.getContext();
			var layout = getLinearLayout();

			var res_input = new TextInput(context, "Resolution (default: 128)", String.valueOf(settings.getInt("pet_res", 128)));
			var w_factor_input = new TextInput(context, "Base Width Factor (default: 0.73)", String.valueOf(settings.getFloat("pet_w_factor", 0.73f)));
			var w_j_input = new TextInput(context, "Width J-Timeline Weight (default: 0.02)", String.valueOf(settings.getFloat("pet_w_j", 0.02f)));
			var h_factor_input = new TextInput(context, "Base Height Factor (default: 0.73)", String.valueOf(settings.getFloat("pet_h_factor", 0.73f)));
			var h_j_input = new TextInput(context, "Height J-Timeline Weight (default: 0.072)", String.valueOf(settings.getFloat("pet_h_j", 0.072f)));
			var ox_input = new TextInput(context, "Offset X Margin (default: 0.14)", String.valueOf(settings.getFloat("pet_ox", 0.14f)));
			var oy_input = new TextInput(context, "Offset Y Margin (default: 0.09)", String.valueOf(settings.getFloat("pet_oy", 0.09f)));
			var delay_input = new TextInput(context, "Frame Delay ms (default: 50, min: 20)", String.valueOf(settings.getInt("pet_delay", 50)));
			var repeat_input = new TextInput(context, "Loop Count (0 for Infinite)", String.valueOf(settings.getInt("pet_repeat", 0)));
			var bg_input = new TextInput(context, "Background Color (Hex or TRANSPARENT)", settings.getString("pet_bg", "TRANSPARENT"));

			var saveButton = new Button(context);
			saveButton.setText("Save");
			saveButton.setOnClickListener(v -> {
				var resVal = ((Supplier<Integer>) () -> { try { return Integer.valueOf(res_input.getEditText().getText().toString()); } catch (Exception e) { return 128; }}).get();
				var wFactorVal = ((Supplier<Float>) () -> { try { return Float.valueOf(w_factor_input.getEditText().getText().toString()); } catch (Exception e) { return 0.73f; }}).get();
				var wJVal = ((Supplier<Float>) () -> { try { return Float.valueOf(w_j_input.getEditText().getText().toString()); } catch (Exception e) { return 0.02f; }}).get();
				var hFactorVal = ((Supplier<Float>) () -> { try { return Float.valueOf(h_factor_input.getEditText().getText().toString()); } catch (Exception e) { return 0.73f; }}).get();
				var hJVal = ((Supplier<Float>) () -> { try { return Float.valueOf(h_j_input.getEditText().getText().toString()); } catch (Exception e) { return 0.072f; }}).get();
				var oxVal = ((Supplier<Float>) () -> { try { return Float.valueOf(ox_input.getEditText().getText().toString()); } catch (Exception e) { return 0.14f; }}).get();
				var oyVal = ((Supplier<Float>) () -> { try { return Float.valueOf(oy_input.getEditText().getText().toString()); } catch (Exception e) { return 0.09f; }}).get();
				var delayVal = ((Supplier<Integer>) () -> { try { return Integer.valueOf(delay_input.getEditText().getText().toString()); } catch (Exception e) { return 50; }}).get();
				var bgVal = ((Supplier<String>) () -> { try { String input = bg_input.getEditText().getText().toString(); if (input.equalsIgnoreCase("TRANSPARENT")) return "TRANSPARENT"; if (!input.startsWith("#")) input = "#" + input; Color.parseColor(input); return input; } catch (Exception e) { return "TRANSPARENT"; }}).get();
				var repeatVal = ((Supplier<Integer>) () -> { try { int val = Integer.valueOf(repeat_input.getEditText().getText().toString()); return val < 0 ? 0 : val; } catch (Exception e) { return 0; }}).get();
				
				settings.setInt("pet_res", resVal);
				settings.setFloat("pet_w_factor", wFactorVal);
				settings.setFloat("pet_w_j", wJVal);
				settings.setFloat("pet_h_factor", hFactorVal);
				settings.setFloat("pet_h_j", hJVal);
				settings.setFloat("pet_ox", oxVal);
				settings.setFloat("pet_oy", oyVal);
				settings.setInt("pet_delay", delayVal);
				settings.setInt("pet_repeat", repeatVal);
				settings.setString("pet_bg", bgVal); 
			});

			layout.addView(res_input);
			layout.addView(w_factor_input);
			layout.addView(w_j_input);
			layout.addView(h_factor_input);
			layout.addView(h_j_input);
			layout.addView(ox_input);
			layout.addView(oy_input);
			layout.addView(delay_input);
			layout.addView(repeat_input);
			layout.addView(bg_input);
			layout.addView(saveButton);
		}
	}
	// ----- settings end -----
	
    @Override
    public void start(@NonNull Context context) throws Throwable {
		if (PluginManager.plugins.containsKey("PetPet") && PluginManager.isPluginEnabled("PetPet")) {
            Utils.showToast("If two, Error? Incompatible? maybe.", true);
            PluginManager.disablePlugin("PetPet");
        }
		
        commands.registerCommand(
            "petpet",
            "You are ready to pet someone.",
            Arrays.asList(
				Utils.createCommandOption(
					ApplicationCommandType.USER, 
					"name", 
					"The user to pet", 
					null, 
					true,
					false
				)
			),
            ctx -> {
				try {
					var user = ctx.getRequiredUser("name");
					var avatar = IconUtils.getForUser(user);
					
					File file = makeGifFile(avatar, context);
					
					ctx.addAttachment(Uri.fromFile(file).toString(), "petpet.gif");
					return new CommandsAPI.CommandResult("");
				} catch (Throwable t) {
					logger.error("Thread creation failed: ", t);
                    return new CommandsAPI.CommandResult("Error: " + t.getMessage() , null, false);
					
                }
            }
        );
		
		commands.registerCommand(
            "peturl",
            "petpet for URL",
            Arrays.asList(
				Utils.createCommandOption(
					ApplicationCommandType.STRING, 
					"url", 
					"The url to pet", 
					null, 
					true,
					false
				)
			),
            ctx -> {
				try {
					String target = ctx.getRequiredString("url");
					
					File file = makeGifFile(target, context);
					
					ctx.addAttachment(Uri.fromFile(file).toString(), "petpet.gif");
					return new CommandsAPI.CommandResult("");
				} catch (Throwable t) {
					logger.error("Thread creation failed: ", t);
                    return new CommandsAPI.CommandResult("Error: " + t.getMessage() , null, false);
					
                }
            }
        );
		
    }
	
	private Bitmap[] splitSpriteToFrames(Context context) {
		int resId = resources.getIdentifier("petpet_sprite", "drawable", "com.tsq.plugins");
		
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inScaled = false;
		
		Bitmap spriteBitmap = BitmapFactory.decodeResource(resources, resId, options);
		
		int frameWidth = 112; 
		int frameHeight = 112;
		
		Bitmap[] handFrames = new Bitmap[5];
		
		for (int i = 0; i < 5; i++) {
			handFrames[i] = Bitmap.createBitmap(spriteBitmap, i * frameWidth, 0, frameWidth, frameHeight);
		}

		spriteBitmap.recycle();
		
		return handFrames;
	}
	
	
	private Bitmap[] createPetPetFrames(Bitmap avatarBitmap, Bitmap[] handFrames) {
		int FRAMES = 5;

		Bitmap[] completedFrames = new Bitmap[FRAMES];
		
		int resolution = settings.getInt("pet_res", 128);

		float petWFactor = settings.getFloat("pet_w_factor", 0.73f);
		float petWJ = settings.getFloat("pet_w_j", 0.02f);
		float petHFactor = settings.getFloat("pet_h_factor", 0.73f);
		float petHJ = settings.getFloat("pet_h_j", 0.072f);
		float petOX = settings.getFloat("pet_ox", 0.14f);
		float petOY = settings.getFloat("pet_oy", 0.09f);
		
		String petBgStr = settings.getString("pet_bg", "TRANSPARENT");
		int bgColor = Color.TRANSPARENT;
		if (!petBgStr.equalsIgnoreCase("TRANSPARENT")) {
			bgColor = Color.parseColor(petBgStr);
		}
		
		Rect handDestRect = new Rect(0, 0, resolution, resolution);
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
		int[] jTimeline = new int[] { 0, 2, 4, 3, 1 };

		for (int i = 0; i < FRAMES; i++) {
			Bitmap canvasBitmap = Bitmap.createBitmap(resolution, resolution, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(canvasBitmap);
			//canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);
			canvas.drawColor(bgColor);
			
			int j = jTimeline[i];
			double widthFactor = petWFactor + (j * petWJ);
			double heightFactor = petHFactor - (j * petHJ);
			
			double offsetXFactor = ((1.0 - widthFactor) * 0.5) + petOX;
			double offsetYFactor = (1.0 - heightFactor) - petOY;

			int left = (int) Math.round(resolution * offsetXFactor);
			int top = (int) Math.round(resolution * offsetYFactor);
			int right = left + (int) Math.round(resolution * widthFactor);
			int bottom = top + (int) Math.round(resolution * heightFactor);
			
			Rect avatarDestRect = new Rect(left, top, right, bottom);

			Rect avatarSrcRect = new Rect(0, 0, avatarBitmap.getWidth(), avatarBitmap.getHeight());
			canvas.drawBitmap(avatarBitmap, avatarSrcRect, avatarDestRect, paint);
			
			Rect handSrcRect = new Rect(0, 0, handFrames[i].getWidth(), handFrames[i].getHeight());
			canvas.drawBitmap(handFrames[i], handSrcRect, handDestRect, paint);

			completedFrames[i] = canvasBitmap;
		}
		
		return completedFrames;
	}

	
	private File makeGifFile(String avatarUrl, Context context) throws Throwable {
		String finalUrl = avatarUrl;
		if (avatarUrl.toLowerCase().endsWith(".webp")) {
			finalUrl = avatarUrl.replace(".webp", ".png");
		}
		
		Http.Response res = new Http.Request(finalUrl).execute();
		Bitmap avatarBitmap = BitmapFactory.decodeStream(res.stream());
		res.stream().close();

		Bitmap[] handFrames = splitSpriteToFrames(context);

		Bitmap[] completedFrames = createPetPetFrames(avatarBitmap, handFrames);

		File outputFile = File.createTempFile("petpet", ".gif", context.getCacheDir());
		
		int userRepeat = settings.getInt("pet_repeat", 0);

		int userDelay = settings.getInt("pet_delay", 40); 
		if (userDelay < 20) userDelay = 20;
		
		String bgColor = settings.getString("pet_bg", "TRANSPARENT");

		AnimatedGifEncoder encoder = new AnimatedGifEncoder();
		encoder.start(new FileOutputStream(outputFile));
		encoder.setDelay(userDelay); 
		encoder.setRepeat(userRepeat);
		encoder.setDispose(2);
		if (bgColor.equalsIgnoreCase("TRANSPARENT")) {
			encoder.setTransparent(Color.TRANSPARENT);
		}

		for (int i = 0; i < 5; i++) {
			encoder.addFrame(completedFrames[i]);
		}
		encoder.finish();

		avatarBitmap.recycle();
		
		for (int i = 0; i < 5; i++) {
			handFrames[i].recycle();
			completedFrames[i].recycle();
		}

		outputFile.deleteOnExit();
		return outputFile;
	}

    @Override
    public void stop(@NonNull Context context) {
        patcher.unpatchAll();
    }
}
