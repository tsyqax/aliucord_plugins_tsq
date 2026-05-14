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

### HeicFix NOTE
1. When you looked big cache due to this, just restart app, then it will clear old cache.
2. This maybe has some lag for some certain situation.

### Why Working
1. FriendFix -> Hook the UI to pass 12345 if there is nothing after #. Then, hook to replace 12345 with the string "null".
3. ForumTagFix -> Hook Multipart into the Build to insert the tag list as payload_json.
5. MediaChannelFix -> When getting the channel type, hook it so that if it is 16, it becomes 15.
7. HeicFix -> Hook Attachment and convert heic to jpg.

---
## Why is this necessary? (Maybe Radical)
### FriendFix
The official service has still not resolved the issue of being unable to add friends, which dates back two years. Despite having #601 PR, the problem has remained dormant in their struggles for over a year. Although this plugin started later than them, it simply solves the friend adding issue in a lighter and faster way.

### ThreadCMD
This adds a very simple command, but when experimental features and blocked solutions are being discussed in official issues, it resolves UI issues with a very simple command as users want.

### ForumTagFix
For some forum channels, applying tags may be mandatory. Regarding the question "Why is this mandatory?" raised by some, this is not a setting configured by unauthorized users, or, just usefulness (categorization) of tags in mind. When writing a forum post, if applicable tags are available, it adds a somewhat crude UI to allow you to select them. While this crude UI might be criticized by some, it is at least faster and works better than the official not working.

### MediaChannelFix
Media channels are a variation of forum channels, primarily featuring files and images. It is said to be a feature that is visible only on certain servers or can be added or activated if specific conditions are met. It is also reportedly related to server subscriptions. However, this does not excuse the need for all users. At the very least, this plugin adds a way for users to see and interact with content.
