package com.tsq.ThreadCmd;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.*;
import com.discord.utilities.rest.RestAPI;
import com.discord.stores.StoreStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull; 
import java.nio.charset.StandardCharsets; 
import okhttp3.MediaType; 
import okhttp3.RequestBody;
import okhttp3.Request;
import okhttp3.MultipartBody;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;

import com.aliucord.api.CommandsAPI;
import com.discord.api.commands.ApplicationCommandType;
import java.util.Arrays;
import java.util.Collections;

@AliucordPlugin
@SuppressWarnings("unused")
public class ThreadCmd extends Plugin {
    public static final Logger logger = new Logger("ThreadCmd");

    @Override
    public void start(@NonNull Context context) throws Throwable {
        commands.registerCommand(
            "thread",
            "Make thread here!",
            Arrays.asList(
				Utils.createCommandOption(
					ApplicationCommandType.STRING, 
					"name", 
					"Name", 
					null, 
					true
				)
			),
            ctx -> {
				try {
					String threadName = ctx.getRequiredString("name");
					//int time = (int) ctx.getRequiredLong("time");
					long channelId = ctx.getChannelId();
					RestAPI.api.createThreadWithMessage(
					    channelId,
						threadName,
						"01010",
						new ArrayList<Long>(),
						new ArrayList<Long>(),
						11,
						1440,
						new MultipartBody.Part[0]
					).V(result -> {});
					return new CommandsAPI.CommandResult("Thread '" + threadName + "' is generated. \n" + "Note: If it's not created, you might not have the required permissions.", null, false);
                } catch (Throwable t) {
					logger.error("Thread creation failed: ", t);
                    return new CommandsAPI.CommandResult("Error: '" + t.getMessage() , null, false);
					
                }
            }
        );
    }

    @Override
    public void stop(@NonNull Context context) {
        patcher.unpatchAll();
    }
}
