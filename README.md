# Aliucord Plugins
This fixes parts not covered by the official code.  

Note: Some parts may contain bugs; if so, please create an issue.

## LIST
- [FriendFix](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/FriendFix.zip): Fixes issue of sending request to new username style user.
- [ThreadCMD](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/ThreadCMD.zip): Add /thread for making thread.
- [ForumTagFix](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/ForumTagFix.zip): Adds method for adding tags to post forum.
- [ThreadDEL (Alpha)](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/ThreadDEL.zip): Add button to delete channel or thread on channel_list. (NOTE: this is alpha)
- [MediaChannelFix](https://github.com/tsyqax/aliucord_plugins_tsq/raw/builds/MediaChannelFix.zip): Make MediaChannel to ForumChannel.

### ThreadDEL Temporary Fix (for Alpha)
1. When make channel or thread -> Restart app, then confirmDialog will be visual.
2. When click 'OK' button on confirmDialog -> change selected channel (or UI renewal), then Channel/Thread will be deleted.
3. Other bugs -> Maybe Fixed if you restart app.

### Tech or Show Inst (?)
1. FriendFix -> Hook the UI to pass 12345 if there is nothing after #. Then, hook to replace 12345 with the string "null".
3. ForumTagFix -> Hook Multipart into the Build to insert the tag list as payload_json.
5. When getting the channel type, hook it so that if it is 16, it becomes 15.
