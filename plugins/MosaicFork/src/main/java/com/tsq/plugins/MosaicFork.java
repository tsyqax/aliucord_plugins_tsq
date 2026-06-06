package com.aliucord.plugins;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.GridLayout;
import android.util.DisplayMetrics;
import android.graphics.Outline;

import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.*;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.PluginManager;


import com.discord.embed.RenderableEmbedMedia;
import com.discord.models.message.Message;
import com.discord.models.member.GuildMember;
import com.discord.api.message.attachment.MessageAttachment;

import com.discord.widgets.chat.list.adapter.WidgetChatListAdapter;
import com.discord.widgets.chat.list.entries.ChatListEntry;
import com.discord.widgets.chat.list.entries.AttachmentEntry;
import com.discord.widgets.chat.list.entries.EmbedEntry;
import com.discord.widgets.chat.list.entries.AutoModSystemMessageEmbedEntry;
import com.discord.widgets.media.WidgetMedia;
import com.discord.widgets.chat.list.InlineMediaView.ViewParams;

import com.discord.stores.StoreMessageState;

import com.discord.utilities.mg_recycler.MGRecyclerViewHolder;
import com.discord.utilities.embed.EmbedResourceUtils;

import com.discord.api.channel.Channel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
	private static final int targetHeightDP = 145; //380px at me
	private static final int paddingLeftDP = 57; //150px at me
	
	@Override
	public void start(Context context) throws Throwable {
		
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		density = dm.density;
		screenWidth = dm.widthPixels; 
		realWidth = (int) (screenWidth * 0.83f);
		targetHeight = (int) (targetHeightDP * density + 0.5f);
		paddingLeft = (int) (paddingLeftDP * density + 0.5f);
		
		Method createEmbedEntriesMethod = ChatListEntry.Companion.getClass().getDeclaredMethod("createEmbedEntries", Message.class, StoreMessageState.State.class, boolean.class, boolean.class, boolean.class, boolean.class, boolean.class, Channel.class, GuildMember.class, Map.class, Map.class);
		
		//  Message, StoreMessageState.State, boolean, boolean, boolean, boolean, boolean, Channel, GuildMember, Map, Map
		patcher.patch(createEmbedEntriesMethod, new Hook(param -> {
			List<Object> originalList = (List<Object>) param.getResult();
			
			if (originalList == null || originalList.isEmpty()) {
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
				originalList.add(0, new MosaicEntry(images));
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
				gridLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
				gridLayout.setPadding(paddingLeft, 0, 0, 0); 
				
				MosaicViewHolder mosaicViewHolder = new MosaicViewHolder(gridLayout, adapter);
				
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

		public MosaicEntry(List<MessageAttachment> images) {
			super();
			this.images = images;
			this.uniqueKey = UUID.randomUUID().toString();
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
	}


	public static class MosaicViewHolder extends MGRecyclerViewHolder<WidgetChatListAdapter, ChatListEntry> {
		private final GridLayout gridLayout;
		//private final List<SimpleDraweeView> cachedImageViews = new ArrayList<>(); //for reuse

		public MosaicViewHolder(GridLayout gridLayout, WidgetChatListAdapter adapter) {
			super(gridLayout, adapter);
			this.gridLayout = gridLayout;
			
			ViewGroup.LayoutParams gridParams = gridLayout.getLayoutParams();
			gridParams.width = realWidth;
				
			gridLayout.setLayoutParams(gridParams);
		}

		public GridLayout getGridLayout() {
			return this.gridLayout;
		}
		
		@Override
		public void onConfigure(int position, ChatListEntry data) {
			if (!(data instanceof MosaicEntry)) return;

			MosaicEntry mosaicEntry = (MosaicEntry) data;
			List<MessageAttachment> images = mosaicEntry.getImages();
			int total = images.size();

			gridLayout.setColumnCount(6);

			int currentChildCount = gridLayout.getChildCount();

			if (currentChildCount > total) {
				gridLayout.removeViews(total, currentChildCount - total);
			}

			for (int i = 0; i < total; i++) {
				SimpleDraweeView imageView;
				int spanSize = getSpanSize(total, i);

				if (i < currentChildCount) {
					imageView = (SimpleDraweeView) gridLayout.getChildAt(i);
				} else {
					imageView = new SimpleDraweeView(gridLayout.getContext());
					imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
					imageView.setOutlineProvider(new android.view.ViewOutlineProvider() {
						@Override
						public void getOutline(View view, Outline outline) {
							outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 8);
						}
					});
					imageView.setClipToOutline(true);
					gridLayout.addView(imageView);
				}

				GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1);
				GridLayout.Spec colSpec = GridLayout.spec(GridLayout.UNDEFINED, spanSize, 1f);
				GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, colSpec);
				params.setMargins(6, 6, 6, 6);
				
				params.width = 0;
				params.height = targetHeight; 
				imageView.setLayoutParams(params);

				MessageAttachment attachment = images.get(i);
				int fileType = attachment.e().ordinal();
				String imageUrl = attachment.c();
				int targetWidth = screenWidth / 2;
				
				MGImages.setImage(imageView, imageUrl, targetWidth, targetHeight);  //log-resolution preview

				imageView.setOnClickListener(v -> {
					try {
						Class<?> cl = PluginManager.plugins.get("SwipeMediaViewer").getClass(); //yeeeeeees
						if (cl == null) {
							WidgetMedia.Companion.launch(v.getContext(), attachment);
							Utils.showToast("SMV is not found");
						} else {
							Method launchGroupMethod = cl.getDeclaredMethod("launchGroup", Context.class, List.class, MessageAttachment.class);
							launchGroupMethod.setAccessible(true);
							launchGroupMethod.invoke(null, v.getContext(), images, attachment);
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
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
