version = "1.1.0" // Plugin version. Increment this to trigger an update
description = "Fix to send heic image" // Plugin description that will be shown to user

aliucord {
    // Changelog of your plugin
    changelog.set(
        """
        NOTE: If big caches exists due to this, just restart app

        # 1.1.0
        * Refector code by referencing HeicImageConvertor of mantikafasi
        * Support non-compress send
        * Support Andorid 9 below maybe

        # 1.0.1
        * Change check for heic (maybe lighter)

        # 1.0.0
        * Added logic to remove old caches when start app

        # 0.0.1
        * Added Heic to JPG logic
        """.trimIndent(),
    )
    // Image or Gif that will be shown at the top of your changelog page
    // changelogMedia.set("https://cool.png")

    // Add additional authors to this plugin
    
    // author("Name", 0L, hyperlink = true)

    // Excludes this plugin from publishing and global plugin repositories.
    // Set this to false if the plugin is unfinished
    deploy.set(true)

    // Builds and deploys this plugin but excludes it from global plugin repositories.
    // Set this if the plugin has reached EOL but a last update should still occur.
    // deployHidden.set(true)
}
