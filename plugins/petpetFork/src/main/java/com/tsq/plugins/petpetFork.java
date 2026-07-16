package com.tsq.plugins;

import android.content.Context;
import android.net.Uri;
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

import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;

import com.aliucord.entities.CommandContext;
import com.aliucord.entities.Plugin;

import com.discord.api.commands.ApplicationCommandType;
import com.discord.utilities.icon.IconUtils;

import java.util.Arrays;
import java.io.File;
import java.io.FileOutputStream;

import com.tsq.plugins.AnimatedGifEncoder;

@AliucordPlugin
@SuppressWarnings("unused")
public class petpetFork extends Plugin {
    public static final Logger logger = new Logger("petpetFork");

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
		
		int resolution = 128;
		
		Rect handDestRect = new Rect(0, 0, resolution, resolution);
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

		for (int i = 0; i < FRAMES; i++) {
			Bitmap canvasBitmap = Bitmap.createBitmap(resolution, resolution, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(canvasBitmap);
			canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);

			int[] jTimeline = new int[] { 0, 2, 4, 3, 1 };
			int j = jTimeline[i];

			double widthFactor = 0.73 + (j * 0.02);
			double heightFactor = 0.73 - (j * 0.072);
			
			double offsetXFactor = ((1.0 - widthFactor) * 0.5) + 0.14;
			double offsetYFactor = (1.0 - heightFactor) - 0.09;

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
		Http.Response res = new Http.Request(avatarUrl.replace(".webp", ".png")).execute();
		Bitmap avatarBitmap = BitmapFactory.decodeStream(res.stream());
		res.stream().close();

		Bitmap[] handFrames = splitSpriteToFrames(context);

		Bitmap[] completedFrames = createPetPetFrames(avatarBitmap, handFrames);

		File outputFile = File.createTempFile("petpet", ".gif", context.getCacheDir());

		AnimatedGifEncoder encoder = new AnimatedGifEncoder();
		encoder.start(new FileOutputStream(outputFile));
		encoder.setDelay(40); 
		encoder.setRepeat(0);
		encoder.setDispose(2);
		encoder.setTransparent(Color.TRANSPARENT);

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
