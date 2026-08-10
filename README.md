# Aliucord Plugins Tsq
This try to fix parts not covered by the official code.  
And... It is really bad to just make something and not release it.  

## NOTE
- Some plugins may contain bugs; if so, please create an issue.

## LIST
- [FriendFix](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/FriendFix.zip): Fixes issue of sending request to new username style user.
- [ThreadCMD](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/ThreadCMD.zip): Add /thread for making thread.
- [ForumTagFix](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/ForumTagFix.zip): Adds method for adding tags to post forum.
- [MediaChannelFix](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/MediaChannelFix.zip): Make MediaChannel to ForumChannel.
- [CopyBackTick](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/CopyBackTick.zip): Make copy to clipboard when you click backtick.
- [FileNameFix](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/FileNameFix.zip): Fix filename when it should unicoded
- [FixOnboardingFork](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/FixOnboardingFork.zip): add /onboarding command and menu on guild menu to do onboarding.
- [MosaicFork](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/MosaicFork.zip): Make images to grid images.
- [ServerNicknameFix](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/ServerNicknameFix.zip): Fix changing server nickname from profile menu.
- [UItweaks](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/UItweaks.zip): Add effort to make better ui.
- [petpetFork](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/petpetFork.zip): Pet someone 2 without api.
- [ChatLagFix](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/ChatLagFix.zip): Try to remove Typing lag.
- [ImageCodec](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/ImageCodec.zip): Prevent image to be file.

## Deprecated / Integrated
- [EmojiRank](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/EmojiRank.zip): Make recent category to guild top emojis.
- [ThreadDEL](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/ThreadDEL.zip): Add button to delete channel or thread on channel_list. --> UItweaks
- [HeicFix](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/HeicFix.zip): Make heic to jpg when you send image. --> ImageCodec

## For Personal (not for formal purposes)
- [MoreProfile](https://github.com/tsyqax/aliucord_plugins_tsq/raw/zips/MoreProfile.zip): MoreProfile 1.0.10 (can change display name). Not my code.
- [AlternativePluginDownloader](https://github.com/tsyqax/aliucord_plugins_tsq/raw/zips/AlternativePluginDownloader.zip): Just Add button plugin install for all channel
- [HttpDebuger](https://github.com/tsyqax/aliucord_plugins_tsq/raw/zips/HttpDebuger.zip): For debug, if you need, use it?
- ~~[QRCodeLoginFork](https://github.com/tsyqax/aliucord_plugins_tsq/raw/zips/QRCodeLoginFork.zip): Add password auth to QRCodeLogin.~~
- [QRCodeLogin](https://github.com/secp192k1/Aliucord-Plugins/raw/21770595a84b0961253155f0806e17d0f97db609/QRCodeLogin.zip): From Original Repo before deleted. I didn't write this.

## HOW TO USE?
Look **[THIS](https://github.com/tsyqax/aliucord_plugins_tsq/blob/main/USAGE.md)** **[THIS](https://github.com/tsyqax/aliucord_plugins_tsq/blob/main/USAGE.md)** **[THIS](https://github.com/tsyqax/aliucord_plugins_tsq/blob/main/USAGE.md)**

---
## If you?
### AvatarDeco (Giveup): Could you continue?
I don't like this whhhhhhhhhhhhhhhhhhhhhhhhhhhhy  
My brain is mmmmmmmmmmmmmmmmmmmmmmmmmmmmmelting  
So, If you have Idea, Do whatever you want with [this](https://github.com/tsyqax/aliucord_plugins_tsq/main/giveup/AvatarDeco.java) :>

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
2. ForumTagFix -> Hook Multipart into the Build to insert the tag list as payload_json.
3. MediaChannelFix -> When getting the channel type, hook it so that if it is 16, it becomes 15.
4. CopyBackTick -> Hook and add ClickSpan to SpannableStringBuilder.
5. FixOnboardingFix -> GET on /guilds/%s/onboarding and POST on /guilds/%s/onboarding-responses with newDiscordRequest();
6. MosaicFork -> Remove vidoes/images from Attachments and add custom entry. And Grid with GridLayout().
7. ServerNicknameFix -> Remove bio field when bio is empty (this means user may not nitro user).
8. ChatLagFix -> Block addTextChangedListener and delayed post

### How is HeicFix different from HeicImageConvertor plugin?
HeicImageConvertor processes HEIC conversion in memory.  
HeicFix loads the data into memory briefly and then caches it to a file for processing.  
Although the two logics may seem similar, if you need to save memory,  
I think HeicFix might be helpful (even if the effect is small).

### How is petpetFork different from petpet plugin?
The original petpet used an API and is now outdated.  
PetpetFork appeared earlier than the recently introduced improved petpet,  
and while the improved petpet uses an image synthesis method like petpetFork,  
it has fewer configurable variables than petpetFork.  
(Initially, petpetFork actually had fewer configurable options,  
but I listened something from them and added a lot of configuration variables.)

### How is ImageCodec different from HeicFix/HeicImageConvertor plugin?
HeicImageConvertor/HeicFix processes only .heic/.heif format.  
But ImageCodec aims to support as many image extensions as possible.  
(Ex/ heic/heif, bmp, jfif, apng, etc.)  
So, in fact, ImageCodec is practically the same as having HeicFix built-in.  
It was also made based on HeicFix.
