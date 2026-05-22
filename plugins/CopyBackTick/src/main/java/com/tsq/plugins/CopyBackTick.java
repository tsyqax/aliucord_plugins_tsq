package com.tsq.plugins;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;
import androidx.annotation.NonNull;
import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.*;
import com.discord.simpleast.code.CodeNode;

@AliucordPlugin(requiresRestart = false)
@SuppressWarnings("unused")
public class CopyBackTick extends Plugin {
	public static final Logger logger = new Logger("CopyBackTick");
	
    @Override
    public void start(@NonNull Context context) throws Throwable { 		
		patcher.patch(CodeNode.class.getDeclaredMethod("render", SpannableStringBuilder.class, Object.class), 
			new Hook(cf -> {
				try {
					SpannableStringBuilder builder = (SpannableStringBuilder) cf.args[0];
					CodeNode node = (CodeNode) cf.thisObject;
					String content = node.getContent();
					
					int end = builder.length();
					int start = end - content.length();
					
					if (start >= 0) {
						ClickableSpan clickSpan = new ClickableSpan() {
							@Override
							public void onClick(View widget) {
								Utils.setClipboard("backtick", content);
								Utils.showToast("copied!");
							}
							
							@Override
							public void updateDrawState(TextPaint ds) {
								ds.setUnderlineText(false);
							}
						};
						builder.setSpan(clickSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
				} catch (Exception e) {
					logger.error("ERRROR", e);
				}
			})
		);
    }

    @Override
    public void stop(@NonNull Context context) {
        patcher.unpatchAll();
    }
}
