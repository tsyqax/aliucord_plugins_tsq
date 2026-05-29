# Aliucord Plugins Tsq
This fixes parts not covered by the official code.  
Note: Some parts may contain bugs; if so, please create an issue.

## LIST
- [FriendFix](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/FriendFix.zip): Fixes issue of sending request to new username style user.
- [ThreadCMD](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/ThreadCMD.zip): Add /thread for making thread.
- [ForumTagFix](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/ForumTagFix.zip): Adds method for adding tags to post forum.
- [ThreadDEL](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/ThreadDEL.zip): Add button to delete channel or thread on channel_list.
- [MediaChannelFix](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/MediaChannelFix.zip): Make MediaChannel to ForumChannel.
- [HeicFix](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/HeicFix.zip): Make heic to jpg when you send image.
- [CopyBackTick](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/CopyBackTick.zip): Make copy to clipboard when you click backtick.
- [FileNameFix](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/FileNameFix.zip): Fix filename when it shoulded unicoded (but not restore yet)
- [EmojiRank](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/EmojiRank.zip): Make recent category to guild top emojis.
- [FixOnboardingFork](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/FixOnboardingFork.zip): add /onboarding command to do onboarding.
- [MosaicFork](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/MosaicFork.zip): Make images to grid images.

## HOW TO USE?
Look **[THIS](https://github.com/tsyqax/aliucord_plugins_tsq/blob/main/USAGE.md)**

### HeicFix NOTE
1. When you looked big cache due to this, just restart app, then it will clear old cache.
2. This maybe has some lag for some certain situation.

### MosaicFork NOTE
1. This may cause lag, I already try to reduce, but that is not easy.
2. This is not compatible with SwipeMediaViewer, but they are not crash or bug, just not swiped with this.
3. This is not perfect, can have bugs, etc.

### Why Working
1. FriendFix -> Hook the UI to pass 12345 if there is nothing after #. Then, hook to replace 12345 with the string "null".
3. ForumTagFix -> Hook Multipart into the Build to insert the tag list as payload_json.
5. MediaChannelFix -> When getting the channel type, hook it so that if it is 16, it becomes 15.
7. HeicFix -> Hook Attachment(with stackTrace) and convert heic to jpg.
8. CopyBackTick -> Hook and add ClickSpan to SpannableStringBuilder.
10. FixOnboardingFix -> GET on /guilds/%s/onboarding and POST on /guilds/%s/onboarding-responses with newDiscordRequest();
11. MosaicFork -> Remove vidoes/images from Attachments and add custom entry. And Grid with GridLayout().

---
## Says
### What is Fork?
It refers to the unofficial succession of plugins that solve the same problem. Maybe remaking?   
Usually, the term "Fork" implies an improved version, but the forks I create have similar flows but often different logic.  
However, I refer to them as "Forks" of the originals out of courtesy and respect for the developers.

### Is it Backport?
※ modified = not same exactly original  
※ simplified = small than original

#### Yes
- FriendFix
- ThreadCMD
- ForumTagFix (modified)
- threadDEL
- MediaChannelFix
- HeicFix
- CopyBackTick
- FilenameFix (simplified)
- EmojiRank (modified)
- FixOnboardingFix (modified, simplified)
- MosaicFork
