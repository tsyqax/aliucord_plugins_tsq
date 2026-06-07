version = "1.1.5" // Plugin version. Increment this to trigger an update
description = "make image to grid gird" // Plugin description that will be shown to user

aliucord {
    // Changelog of your plugin
    changelog.set(
        """
        # 1.1.5
        * Add support for Spoiler
        * Fix issue with video grid

        # 1.1.4
        * Fix bug when using without SwipeMediaViewer
        * sorry, I checked too late

        # 1.1.3
        * Change hard-coded PX to hard-coded DP

        # 1.1.2
        * Add support for SwipeMediaViewer
        * Increasing width and height (will go to setting later)

        # 1.1.1
        * Add margin 6px
        * Add rounding 8px

        # 1.1.0
        * Fixed Very Huge huge lag!
		
        # 1.0.0
        * IPR!
        * impressed by Mozaic of zt!
        """.trimIndent(),
    )
    // Image or Gif that will be shown at the top of your changelog page
    // changelogMedia.set("https://cool.png")

    // Add additional authors to this plugin
    // author("Name", 0L, hyperlink = true)
    // author("Name", 0L, hyperlink = true)

    // Excludes this plugin from publishing and global plugin repositories.
    // Set this to false if the plugin is unfinished
    deploy.set(true)

    // Builds and deploys this plugin but excludes it from global plugin repositories.
    // Set this if the plugin has reached EOL but a last update should still occur.
    // deployHidden.set(true)
}
