package com.tsq.plugins;

import android.content.Context;
import android.os.Looper;
import android.os.Handler;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.Selection;
import android.util.Printer;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.view.View;
import android.widget.TextView;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.*;
import com.aliucord.Logger;
import com.aliucord.api.SettingsAPI;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.views.TextInput;
import com.aliucord.views.Button;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.function.Supplier;

import com.discord.views.CheckedSetting;
import com.discord.widgets.chat.input.WidgetChatInput;
import com.discord.widgets.chat.input.WidgetChatInputEditText;
import com.discord.widgets.chat.input.ChatInputViewModel;
import com.discord.widgets.chat.input.autocomplete.InputEditTextAction;
import com.discord.widgets.chat.input.autocomplete.InputAutocomplete;

import com.lytefast.flexinput.widget.FlexEditText;


@AliucordPlugin
public class ChatLagFix extends Plugin {
	private static final Logger logger = new Logger("ChatLagFix");
	
	public ChatLagFix() { 
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
			setActionBarTitle("ChatLagFix");
			setActionBarSubtitle("Settings!");

			var context = view.getContext();
			var layout = getLinearLayout();

			var chat = new TextInput(context, "Chat Length to working", String.valueOf(settings.getInt("chat", 1000)));
			var delay = new TextInput(context, "Listener Delay (default: 500)", String.valueOf(settings.getInt("delay", 500)));

			CheckedSetting power = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, "Only Fast mode","");
			power.setChecked(settings.getBool("power", false));
			power.setOnCheckedListener(Boolean -> {
				settings.setBool("power", Boolean);
				if (Boolean) {
					Utils.showToast("WARN: Mention and others may not be working!");
					Utils.showToast("WARN: Some text can be diminished!");
				}
				Utils.promptRestart();
			});

			
			var saveButton = new Button(context);
			saveButton.setText("Save");
			saveButton.setOnClickListener(v -> {
				var chatVal = ((Supplier<Integer>) () -> { try { return Integer.valueOf(chat.getEditText().getText().toString()); } catch (Exception e) { return 1000; }}).get();
				var delayVal = ((Supplier<Integer>) () -> { try { return Integer.valueOf(delay.getEditText().getText().toString()); } catch (Exception e) { return 500; }}).get();
				
				settings.setInt("delay", delayVal);
				settings.setInt("chat", chatVal);

				Utils.promptRestart();
			});

			layout.addView(delay);
			layout.addView(chat);
			layout.addView(power);
			layout.addView(saveButton);
		}
	}
	// ----- settings end -----
	
	// Thanks for someone(Anonymous) to help me
	@Override
	public void start(Context context) {
		int delay = settings.getInt("delay", 500);
		int chat = settings.getInt("chat", 500);
		Boolean power = settings.getBool("power", false);
		
		try {
			Method addListenerMethod = TextView.class.getDeclaredMethod("addTextChangedListener", TextWatcher.class);

			patcher.patch(addListenerMethod, new PreHook(param -> {
				if (param.thisObject instanceof FlexEditText) {

					if (param.args[0] instanceof TextWatcher) {
						final TextWatcher original = (TextWatcher) param.args[0];

						// new watcher
						param.args[0] = new TextWatcher() {
							private final Handler handler = new Handler(Looper.getMainLooper());
							private Runnable pendingRunnable = null;
							private boolean stoped = false;
							
							@Override
							public void beforeTextChanged(CharSequence s, int start, int count, int after) {
								if (s == null || s.length() < chat) {
									original.beforeTextChanged(s, start, count, after);
									return;
								}

								if (!stoped) {
									original.beforeTextChanged(s, start, count, after);
								}
							}

							@Override
							public void onTextChanged(CharSequence s, int start, int before, int count) {
								if (s == null || s.length() < chat) {
									original.onTextChanged(s, start, before, count);
									return;
								}
								if (!stoped) {
									original.onTextChanged(s, start, before, count);
								}
							}

							@Override
							public void afterTextChanged(Editable s) {
								if (s == null) return;
								final int textLength = s.length();
								
								if (textLength == 0 && stoped) {
									stoped = false;
									if (pendingRunnable != null) {
										handler.removeCallbacks(pendingRunnable);
										pendingRunnable = null;
									}
									original.afterTextChanged(s);
									return;
								}

								if (textLength < chat) {
									original.afterTextChanged(s);
									return;
								}

								stoped = true;

								if (pendingRunnable != null) {
									handler.removeCallbacks(pendingRunnable);
								}

								pendingRunnable = new Runnable() {
									@Override
									public void run() {
										stoped = false;

										original.beforeTextChanged(s, 0, textLength, textLength);
										original.onTextChanged(s, 0, 0, textLength);
										original.afterTextChanged(s);
									}
								};
								
								if (!power) {
									handler.postDelayed(pendingRunnable, delay);
								}
							}
						};
					}
				}
			}));
		} catch (NoSuchMethodException e) {
			logger.error("addTextChangedListener", e);
		}

		try {
			Method getTextMethod = WidgetChatInputEditText.class.getDeclaredMethod("getText");

			patcher.patch(getTextMethod, new PreHook(param -> {
				WidgetChatInputEditText widgetInstance = (WidgetChatInputEditText) param.thisObject;
				
				try {
					java.lang.reflect.Field editTextField = WidgetChatInputEditText.class.getDeclaredField("editText");
					editTextField.setAccessible(true);
					FlexEditText flexEditText = (FlexEditText) editTextField.get(widgetInstance);

					if (flexEditText != null && flexEditText.getText() != null) {
						CharSequence originalText = flexEditText.getText();
						
						if (originalText.length() >= chat) {
							String finalRawText = originalText.toString();
							param.setResult(finalRawText);
							return;
						}
					}
				} catch (Exception e) {
					logger.error("getText2", e);
				}
			}));
		} catch (Throwable e) {
			logger.error("getText", e);
		}

	}

	@Override
	public void stop(Context context) {
		patcher.unpatchAll();
	}
}
