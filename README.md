# Aliucord Plugins Tsq
This try to fix parts not covered by the official code.  
And... It is really bad to just make something and not release it.  

## NOTE
- Some plugins may contain bugs; if so, please create an issue.

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
- [FixOnboardingFork](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/FixOnboardingFork.zip): add /onboarding command and menu on guild menu to do onboarding.
- [MosaicFork](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/MosaicFork.zip): Make images to grid images.
- [ServerNicknameFix](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/ServerNicknameFix.zip): Fix changing server nickname from profile menu.

## Deprecated (only some part of work correctly)
- [EmojiRank](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/EmojiRank.zip): Make recent category to guild top emojis.

## For Personal (not for formal purposes)
- [MoreProfile](https://github.com/tsyqax/aliucord_plugins_tsq/raw/zips/MoreProfile.zip): MoreProfile 1.0.10 version. This is not the code I wrote.
- [AlternativePluginDownloader](https://github.com/tsyqax/aliucord_plugins_tsq/raw/zips/AlternativePluginDownloader.zip): Just Add button plugin install when not support's channel
- [HttpDebuger](https://github.com/tsyqax/aliucord_plugins_tsq/raw/zips/HttpDebuger.zip): For debug, if you need, use it?
- [QRCodeLoginFork](https://github.com/tsyqax/aliucord_plugins_tsq/raw/zips/QRCodeLoginFork.zip): Add password auth to QRCodeLogin. 

## HOW TO USE?
Look **[THIS](https://github.com/tsyqax/aliucord_plugins_tsq/blob/main/USAGE.md)** **[THIS](https://github.com/tsyqax/aliucord_plugins_tsq/blob/main/USAGE.md)** **[THIS](https://github.com/tsyqax/aliucord_plugins_tsq/blob/main/USAGE.md)**

---
## If you?
### Translatable (Draft): Do you have Idea?
I just think of translatable plugin (providing translated texts for app and plugin)  
But I don't know about how to implement that and other detail ideas (at least now).  
So, If you have Idea, please provide that [here](https://github.com/tsyqax/aliucord_plugins_tsq/issues/5) :>
  

---
## Says
### What is Fork?
It refers to the unofficial succession of plugins that solve the same problem. Maybe remaking?   
Usually, the term "Fork" implies an improved version, but the forks I create have similar flows but often different logic.  
However, I refer to them as "Forks" of the originals out of courtesy and respect for the developers.

### Why they Working
1. FriendFix -> Hook the UI to pass 12345 if there is nothing after #. Then, hook to replace 12345 with the string "null".
3. ForumTagFix -> Hook Multipart into the Build to insert the tag list as payload_json.
5. MediaChannelFix -> When getting the channel type, hook it so that if it is 16, it becomes 15.
8. CopyBackTick -> Hook and add ClickSpan to SpannableStringBuilder.
10. FixOnboardingFix -> GET on /guilds/%s/onboarding and POST on /guilds/%s/onboarding-responses with newDiscordRequest();
11. MosaicFork -> Remove vidoes/images from Attachments and add custom entry. And Grid with GridLayout().
12. ServerNicknameFix -> Remove bio field when bio is empty (this means user may not nitro user).

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
- ~~EmojiRank (modified)~~
- FixOnboardingFix (modified, simplified)
- MosaicFork
- ServerNicknameFix
