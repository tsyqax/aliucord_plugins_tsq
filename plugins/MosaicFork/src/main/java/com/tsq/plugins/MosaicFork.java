package com.aliucord.plugins;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.widget.GridLayout;
import android.util.DisplayMetrics;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Animatable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.Gravity;
import android.graphics.Color;
import android.view.ViewOutlineProvider;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.*;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.PluginManager;
import com.aliucord.api.SettingsAPI;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.views.TextInput;
import com.aliucord.views.Button;

import com.discord.embed.RenderableEmbedMedia;
import com.discord.models.message.Message;
import com.discord.models.member.GuildMember;

import com.discord.widgets.chat.list.adapter.WidgetChatListAdapter;
import com.discord.widgets.chat.list.entries.ChatListEntry;
import com.discord.widgets.chat.list.entries.AttachmentEntry;
import com.discord.widgets.chat.list.entries.EmbedEntry;
import com.discord.widgets.chat.list.entries.AutoModSystemMessageEmbedEntry;
import com.discord.widgets.media.WidgetMedia;
import com.discord.widgets.chat.list.InlineMediaView;

import com.discord.views.CheckedSetting;

import com.discord.stores.StoreMessageState;
import com.discord.stores.StoreUserSettings;
import com.discord.stores.StoreStream;

import com.discord.utilities.mg_recycler.MGRecyclerViewHolder;
import com.discord.utilities.embed.EmbedResourceUtils;

import com.discord.api.channel.Channel;
import com.discord.api.message.attachment.MessageAttachment;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import com.discord.utilities.images.MGImages;
import com.facebook.drawee.view.SimpleDraweeView;


@AliucordPlugin
public class MosaicFork extends Plugin {
	private final Logger logger = new Logger("MosaicFork");
	private static final int MOSAIC_VIEW_TYPE = 1234;
	private static int realWidth;
	private static int screenWidth;
	private static int targetHeight;
	private static int paddingLeft;
	private static float density;
	private static int targetHeightDP = 145; //380px at me
	private static int paddingLeftDP = 57; //150px at me
	private StoreUserSettings storeUserSettings = StoreStream.getUserSettings();

	public MosaicFork() { 
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
			setActionBarTitle("MosaicFork");
			setActionBarSubtitle("Settings!");

			var context = view.getContext();
			var layout = getLinearLayout();

			var width_input = new TextInput(context, "Width Ratio (0.0 ~ 1.0)", String.valueOf(settings.getFloat("width", 0.83f)));
			var height_input = new TextInput(context, "Height DP (default: 145)", String.valueOf(settings.getInt("height", 145)));
			var padding_input = new TextInput(context, "Padding DP (default: 57)", String.valueOf(settings.getInt("padding", 57)));
			
			CheckedSetting autoGif = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, "Toggle auto play Gif","");
			autoGif.setChecked(settings.getBool("autoGif", true));
			autoGif.setOnCheckedListener(Boolean -> {
				settings.setBool("autoGif", Boolean);
			});
			
			CheckedSetting ani_webp = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, "Use Animated Webp instead of Gif","");
			ani_webp.setChecked(settings.getBool("ani_webp", false));
			ani_webp.setOnCheckedListener(Boolean -> {
				settings.setBool("ani_webp", Boolean);
			});
			
			CheckedSetting lowGif = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, "Downgrade Gifs Preview Quality","");
			lowGif.setChecked(settings.getBool("lowGif", true));
			lowGif.setOnCheckedListener(Boolean -> {
				settings.setBool("lowGif", Boolean);
			});
			
			CheckedSetting lowImage = Utils.createCheckedSetting(context, CheckedSetting.ViewType.SWITCH, "Downgrade Images Preview Quality","");
			lowImage.setChecked(settings.getBool("lowImage", false));
			lowImage.setOnCheckedListener(Boolean -> {
				settings.setBool("lowImage", Boolean);
			});
			
			var saveButton = new Button(context);
			saveButton.setText("Save");
			saveButton.setOnClickListener(v -> {
				var widthVal = ((Supplier<Float>) () -> { try { return Float.valueOf(width_input.getEditText().getText().toString()); } catch (Exception e) { return 0.83f; }}).get();
				var heightVal = ((Supplier<Integer>) () -> { try { return Integer.valueOf(height_input.getEditText().getText().toString()); } catch (Exception e) { return 145; }}).get();
				var paddingVal = ((Supplier<Integer>) () -> { try { return Integer.valueOf(padding_input.getEditText().getText().toString()); } catch (Exception e) { return 57; }}).get();
				
				settings.setFloat("width", widthVal);
				settings.setInt("height", heightVal);
				settings.setInt("padding", paddingVal);

				Utils.promptRestart();
			});

			layout.addView(width_input);
			layout.addView(height_input);
			layout.addView(padding_input);
			layout.addView(autoGif);
			layout.addView(ani_webp);
			layout.addView(lowGif);
			layout.addView(lowImage);
			layout.addView(saveButton);
		}
	}
	// ----- settings end -----
	
	@Override
	public void start(Context context) throws Throwable {
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		density = dm.density;
		screenWidth = dm.widthPixels; 
		
		Float inputWidth = settings.getFloat("width", 0.83f);
		targetHeightDP = settings.getInt("height", 145);
		paddingLeftDP = settings.getInt("padding", 57);
		
		realWidth = (int) (screenWidth * inputWidth);
		targetHeight = (int) (targetHeightDP * density + 0.5f);
		paddingLeft = (int) (paddingLeftDP * density + 0.5f);
		
		Method createEmbedEntriesMethod = ChatListEntry.Companion.getClass().getDeclaredMethod("createEmbedEntries", Message.class, StoreMessageState.State.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class, Channel.class, GuildMember.class, Map.class, Map.class);
		//  Message, StoreMessageState.State, boolean, boolean, boolean, boolean, boolean, Channel, GuildMember, Map, Map
		patcher.patch(createEmbedEntriesMethod, new Hook(param -> {
			List<Object> originalList = (List<Object>) param.getResult();
			Message msg  = (Message) param.args[0];
				
			if (!storeUserSettings.getIsAttachmentMediaInline() || originalList == null || originalList.isEmpty()) {
				return;
			}
				
			if (originalList.size() <= 1) {
				return; 
			}
				
			List<MessageAttachment> images = new ArrayList<>();
				
			// for (Object unKnown: originalList)
			for (int i = originalList.size() - 1; i >= 0; i--) { //reverse
				Object entry = originalList.get(i);
					
				if (entry != null && entry.getClass() == AttachmentEntry.class) {
						
					AttachmentEntry attachmentEntry = (AttachmentEntry) entry;
					MessageAttachment attachment = attachmentEntry.getAttachment();
					int fileType = attachment.e().ordinal();
						
					if (fileType == 0 || fileType == 1) { 
						images.add(attachment);
						originalList.remove(i);
					}
				}
			}

			if (images.size() > 1) {
				Collections.reverse(images); 
				originalList.add(0, new MosaicEntry(images, msg));
			}

			param.setResult(originalList);
		}));

		Method chatListAdapterMethod = WidgetChatListAdapter.class.getDeclaredMethod("onCreateViewHolder", ViewGroup.class, int.class);
		patcher.patch(chatListAdapterMethod, new Hook(param -> {
			int viewType = (int) param.args[1];
			ViewGroup parent = (ViewGroup) param.args[0];
			WidgetChatListAdapter adapter = (WidgetChatListAdapter) param.thisObject;
				
			if (viewType == MOSAIC_VIEW_TYPE) {
				GridLayout gridLayout = new GridLayout(parent.getContext());
				
				ConstraintLayout rootWrapper = new ConstraintLayout(parent.getContext());
				rootWrapper.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				
				gridLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)); // before: MATCH_PARENT
				gridLayout.setPadding(paddingLeft, 0, 0, 0); 
				
				ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				params.horizontalBias = 0.0f;
				params.constrainedWidth = true;
				params.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
				params.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID;
				
				rootWrapper.addView(gridLayout, params);
				
				//MosaicViewHolder mosaicViewHolder = new MosaicViewHolder(gridLayout, adapter);
				MosaicViewHolder mosaicViewHolder = new MosaicViewHolder(rootWrapper, gridLayout, adapter);
    
				param.setResult(mosaicViewHolder); 
			}
		}));
	}
	
	@Override
	public void stop(Context context) {
		patcher.unpatchAll();
	}


	public static class MosaicEntry extends ChatListEntry {
		private final List<MessageAttachment> images;
		private final String uniqueKey;
		private Message msg;

		public MosaicEntry(List<MessageAttachment> images, Message msg) {
			super();
			this.images = images;
			this.uniqueKey = String.valueOf(msg.getId()); 
			this.msg = msg;
		}

		public List<MessageAttachment> getImages() {
			return this.images;
		}

		@Override
		public int getType() {
			return MOSAIC_VIEW_TYPE;
		}
		
		@Override
		public String getKey() {
			return this.uniqueKey;
		}

		public Message getMsg() {
			return this.msg;
		}
	}


	public class MosaicViewHolder extends MGRecyclerViewHolder<WidgetChatListAdapter, ChatListEntry> {
		private final GridLayout gridLayout;
		//private final List<SimpleDraweeView> cachedImageViews = new ArrayList<>(); //for reuse
		private final HashSet<Integer> isOpened = new HashSet<>();
		private WidgetChatListAdapter.EventHandler evhandler;

		public MosaicViewHolder(GridLayout gridLayout, WidgetChatListAdapter adapter) {
			super(gridLayout, adapter);
			this.gridLayout = gridLayout;
			
			ViewGroup.LayoutParams gridParams = gridLayout.getLayoutParams();
			gridParams.width = realWidth;
				
			gridLayout.setLayoutParams(gridParams);
			
			this.evhandler = this.adapter.getEventHandler(); 
		}
		
		public MosaicViewHolder(View itemView, GridLayout gridLayout, WidgetChatListAdapter adapter) {
			super(itemView, adapter);
			this.gridLayout = gridLayout;
			
			ViewGroup.LayoutParams gridParams = gridLayout.getLayoutParams();
			gridParams.width = realWidth;
				
			gridLayout.setLayoutParams(gridParams);
			
			this.evhandler = this.adapter.getEventHandler(); 
		}

		public GridLayout getGridLayout() {
			return this.gridLayout;
		}
		
		@Override
		public void onConfigure(int position, ChatListEntry data) {
			if (!(data instanceof MosaicEntry)) return;

			MosaicEntry mosaicEntry = (MosaicEntry) data;
			List<MessageAttachment> images = mosaicEntry.getImages();
			Message msg = mosaicEntry.getMsg(); 
			int total = images.size();

			gridLayout.setColumnCount(6);

			int currentChildCount = gridLayout.getChildCount();
			int gifCount = 0;

			if (currentChildCount > total) {
				gridLayout.removeViews(total, currentChildCount - total);
			}
			
			long guildId = StoreStream.getGuildSelected().getSelectedGuildId();
			boolean shouldSpoilered = false;
			
			try {
				Class<?> cl = PluginManager.plugins.get("BetterSpoiler").getClass();
				Object bsInstance = PluginManager.plugins.get("BetterSpoiler");
				Method shouldMethod = cl.getDeclaredMethod("shouldEnableSpoiler", Message.class, long.class);
				shouldMethod.setAccessible(true);
				shouldSpoilered = (boolean) shouldMethod.invoke(bsInstance, msg, guildId);
			} catch (NullPointerException e) {
			} catch (Exception e) {
				logger.error("ERR05", e);
			} 

			for (int i = 0; i < total; i++) {
				FrameLayout container;
				SimpleDraweeView imageView;
				int spanSize = getSpanSize(total, i);

				if (i < currentChildCount) {
					container = (FrameLayout) gridLayout.getChildAt(i);
					imageView = (SimpleDraweeView) container.getChildAt(0);
					//imageView.setImageURI((String) null); 
				} else {
					container = new FrameLayout(gridLayout.getContext());
					imageView = new SimpleDraweeView(gridLayout.getContext());
					imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
					imageView.setOutlineProvider(new android.view.ViewOutlineProvider() {
						@Override
						public void getOutline(View view, Outline outline) {
							outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 8);
						}
					});
					imageView.setClipToOutline(true);
					container.addView(imageView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
					gridLayout.addView(container);
				}

				GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1);
				GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED, spanSize, 1f);
				GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, colSpec);
				params.setMargins(6, 6, 6, 6);
					
				params.width = 0;
				params.height = targetHeight; 
				container.setLayoutParams(params);

				MessageAttachment attachment = images.get(i);
				int fileType = attachment.e().ordinal();
				//int targetWidth = screenWidth / 2;
				String imageUrl = attachment.c(); 
				Boolean aniMode = settings.getBool("ani_webp", false);
				Boolean lowGif = settings.getBool("lowGif", true);
				Boolean lowImage = settings.getBool("lowImage", false);
				Boolean autoGif = settings.getBool("autoGif", true);
				
				if (fileType == 0) {
					if (lowImage) {
						imageUrl = attachment.c() + "format=jpeg&width=500&height=500&";
					} else {
						imageUrl = attachment.c() + "format=jpeg&";
					}
					
					MGImages.setImage(imageView, imageUrl);

					if (container.getChildCount() == 1) {
						ImageView playButton = new ImageView(gridLayout.getContext());
						playButton.setImageResource(android.R.drawable.ic_media_play);
						playButton.setColorFilter(android.graphics.Color.WHITE);
						
						GradientDrawable circleBg = new GradientDrawable();
						circleBg.setShape(GradientDrawable.OVAL);
						circleBg.setColor(android.graphics.Color.parseColor("#80000000"));
						playButton.setBackground(circleBg);
						
						int padding = (int) (8 * gridLayout.getContext().getResources().getDisplayMetrics().density);
						playButton.setPadding(padding, padding, padding, padding);
						
						int btnSize = (int) (52 * gridLayout.getContext().getResources().getDisplayMetrics().density);
						FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(btnSize, btnSize);
						btnParams.gravity = Gravity.CENTER;

						container.addView(playButton, btnParams);
					}
				} else {
					if (container.getChildCount() > 1 && !(container.getChildAt(1) instanceof TextView)) {
						container.removeViewAt(1); 
					}
					
					if (imageUrl.toLowerCase().contains(".gif")) {
						if (aniMode) {
							imageUrl = imageUrl + "animated=true&format=webp&";
						}
						
						//logger.info("why this is not work:" + storeUserSettings.getIsAutoPlayGifsEnabled());
						if (!autoGif) {
							imageUrl = imageUrl + "format=jpeg&";
						}
						
						if (lowGif) {
							imageUrl = imageUrl + "width=200&height=200&";
						}
					} else {
						if (lowImage) {
							imageUrl = imageUrl + "width=500&height=500&";
						}
					}

					MGImages.setImage(imageView, imageUrl);  //log-resolution preview
				}
				
				boolean hasSpoilerView = false;
				
				if (container.getChildCount() > 0 && container.getChildAt(container.getChildCount() - 1) instanceof TextView) {
					hasSpoilerView = true;
				}
				
				if ((shouldSpoilered || attachment.h())) {
					if (!hasSpoilerView) {
						if (!isOpened.contains(i)) {							
							TextView spoilerOverlay = new TextView(gridLayout.getContext());
							spoilerOverlay.setText("SPOILER");
							spoilerOverlay.setTextColor(android.graphics.Color.WHITE);
							spoilerOverlay.setGravity(android.view.Gravity.CENTER);
							spoilerOverlay.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
							spoilerOverlay.setTextSize(13);

							spoilerOverlay.setBackgroundColor(android.graphics.Color.parseColor("#FF2F3136"));

							spoilerOverlay.setOutlineProvider(new android.view.ViewOutlineProvider() {
								@Override
								public void getOutline(View view, Outline outline) {
									outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 8);
								}
							});
							spoilerOverlay.setClipToOutline(true);
							
							final int finalidx = i;

							spoilerOverlay.setOnClickListener(v -> {
								View overlay = v; 
								if (overlay.getParent() instanceof FrameLayout) {
									FrameLayout parent = (FrameLayout) overlay.getParent();
									parent.removeView(overlay);
								}
								isOpened.add(finalidx);
							});

							container.addView(spoilerOverlay, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
						}
					}
				} else {
					if (hasSpoilerView) {
						container.removeViewAt(container.getChildCount() - 1);
					}
				}
				
				imageView.setOnClickListener(v -> {
					try {
						Class<?> cl = PluginManager.plugins.get("SwipeMediaViewer").getClass(); //yeeeeeees
						if (cl == null) {
							WidgetMedia.Companion.launch(v.getContext(), attachment);
							//Utils.showToast("SMV is not found");
						} else {
							Method launchGroupMethod = cl.getDeclaredMethod("launchGroup", Context.class, List.class, MessageAttachment.class);
							launchGroupMethod.setAccessible(true);
							launchGroupMethod.invoke(null, v.getContext(), images, attachment);
						}
					} catch (NullPointerException e) {
						WidgetMedia.Companion.launch(v.getContext(), attachment);
						//Utils.showToast("SMV is not found");
					} 
					catch (Exception e) {
						e.printStackTrace();
					}
				});
			}
			
			WidgetChatListAdapter.EventHandler handler = this.evhandler;

			if (handler != null) {
				this.itemView.setOnClickListener(v -> {
					handler.onMessageClicked(msg, false);
				});

				this.itemView.setOnLongClickListener(v -> {
					handler.onMessageLongClicked(msg, "", false);
					return true;
				});
			}
		}

		private int getSpanSize(int total, int position) {
			if (total == 1) return 6;
			if (total == 2) return 3;

			if (total == 3) {
				return (position < 2) ? 3 : 6;
			}

			if (total >= 4 && total <= 6) {
				if (total == 4) return 3;
				if (total == 5) return (position < 3) ? 2 : 3;
				if (total == 6) return 2;
			}

			if (total >= 7 && total <= 9) {
				if (total == 7) return (position < 3) ? 2 : 3;
				if (total == 8) return (position < 6) ? 2 : 3;
				if (total == 9) return 2;
			}

			if (total == 10) {
				return (position < 9) ? 2 : 6;
			}

			return 6;
		}
	}
}
