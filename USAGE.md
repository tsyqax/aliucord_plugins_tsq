## Usage

### FriendFix
- You can add friends on the 'Add Friend' screen.
- If you enter the name without a '#', the request is sent without the '#'.
- If you include a '#' when entering the name, the request is sent with the '#' included (following the old naming system).

### ThreadCMD
- Use the `/thread {name}` command to create a thread.
- Surprisingly, this is a feature backported from RN.

### ForumTagFix
- (Before 1.3.0) If the forum channel has tags, the tag selector appears when creating a post.
- (Before 1.3.0) If there are no tags, the tag selector does not appear.
- (Since 1.2.0) To change a post's tags, long-press the post on the post list screen; an 'Edit Tag' option will appear in the menu.
- (Since 1.2.0) The tag editing function works only if the forum post is not archived.
- (Since 1.3.0) "Select Tags" is displayed if tags are available.
- (Since 1.3.0) After selecting tags, you must press the "OK" button to save them.

### ThreadDEL
- Allows you to delete threads or channels from the list screen.
- Simply long-press the item you wish to delete.
- Go to the plugin settings to change the button text.

### MediaChannelFix
- No additional configuration is required.

### HeicFix
- No additional configuration is required.

### CopyBackTick
- No additional configuration is required.
- Click the backtick symbol to copy. - Surprisingly, this is a feature backported from RN.

### FileNameFix
- No additional configuration is required.

### EmojiRank (Deprecated)
- No additional configuration is required.
- This feature modifies the 'Recent' section of the emoji picker.
- You can disable the plugin if you do not wish to use this feature.

### FixOnboardingFork
- This is an unofficial successor to the FixOnboarding plugin.
(It is actually implemented in a completely different way.)
- If you join a server but messages fail to send, it may be due to an onboarding issue. Please follow the steps below.
- (Since 1.3.0) Enable the 'Old Style' option if you prefer the classic onboarding screen.

\<How to use Onboarding>
- Use the `/onboarding` command.
- (Since 1.1.1) You can also start onboarding via the guild menu that appears when you long-press the guild icon.
- (Since 1.1.3) If 'Auto Mode' is enabled, onboarding is automatically checked when joining a server.

### MosaicFork
- This is an unofficial successor to the Mozaic plugin.
(It is actually implemented differently.)
- Below are detailed explanations of the settings.

\<Settings>
- (Since v1.1.8) Width Ratio: The width the grid occupies relative to the full screen.
- (Since v1.1.8) Height DP: The height the grid occupies.
- (Since v1.1.8) Padding DP: The distance of the grid from the left edge. - (Since 1.1.9) Animated WebP: An option to render as WebP instead of GIF.
- (Since 1.1.9) Low GIF Preview: Lowers the resolution of GIF files within the grid.
- (Since 1.1.9) Low Image Preview: Lowers the resolution of image files within the grid.
- (Since 1.1.10) Auto Play GIF: Determines whether GIF files play automatically. This operates independently of the app's general settings; it is recommended to turn this off if performance lags due to a large number of GIFs.
- (Since 1.2.5) SquareMode: Renders grid images as close to a square shape as possible (may not be a perfect square).

### ServerNicknameFix
- No additional configuration required.

### UItweaks
- ThreadDEL: Same functionality as the aforementioned ThreadDEL.
- ForumLine: Adds a separator line at the bottom of forum posts for visual distinction.
- (Since 1.0.2) RuleChIcon: A feature carried over from BetterChannelIcon; changes the icon for "rules" channels to a law book.
- (Since 1.0.3) PluralFix: Fixes display errors in languages ​​that do not distinguish between singular and plural forms.
- (Since 1.0.4) ProfileDeco: Displays decorations on the user profile screen. (Beta)

### petpetFork
- `/petpet (user)`: Pets a specified user.
- (Since 1.2.1) `/peturl (link)`: Pets a specified image.
- (Since 1.2.0) I don't quite recall the details of the settings options. Feel free to experiment with them one by one.

### ChatLagFix
- Listener Delay: Determines the delay before the listener becomes active. The listener must be active for features like mentions to work.
- Chat Length: Determines the minimum message length required to trigger this delay function. The default was likely 1000.
- Only Fast Mode: Disables the listener. While most features will not function, this mode is optimized for sending text-only messages. Note that text is not saved while typing.

### ImageCodec
- No additional configuration is required.
- Supported codecs: heic, heif, hif, bmp, avif, dib, jfif, jfi, jpe, pjpeg, pjpg, apng
