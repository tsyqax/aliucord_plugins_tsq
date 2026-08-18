version = "1.2.3" // Plugin version. Increment this to trigger an update
description = "Make image to grid gird" // Plugin description that will be shown to user

aliucord {
    // Changelog of your plugin
    changelog.set(
        """
        # 1.2.3
        * Rewrite to Kotlin!
        * by lazy
        * Optimize plugin checking, spoiler view, play btn


        # 1.2.2
        * Fix setting text of padding
        * delete targetWidth (maybe optimization)

        # 1.2.1
        * Fix width (like MessageWidthFix)
        * targetWidth Optimized

        # 1.2.0
        * Fix flash bug of static image (not gif, im sad)
        * Make Spoiler logic better before than
        * Some Optimized

        # 1.1.11
        * Fix image order with Collections.reverse()

        # 1.1.10
        * Add setting for toggle of auto playing gif
        * Why this not work: getIsAutoPlayGifsEnabled()

        # 1.1.9
        * Some Optimized
        * Add setting for animated Webp(instead of Gif), low quality
        * If you have lag due to gifs, turn on low quality gifs!

        # 1.1.8
        * Add setting for width, height, padding

        # 1.1.7
        * Add support for BetterSpoiler

        # 1.1.6
        * Add support for disabled preview

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
        * impressed from Mozaic of zt
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
