version = "1.2.0" // Plugin version. Increment this to trigger an update
description = "Pet someone 2" // Plugin description that will be shown to user

aliucord {
    // Changelog of your plugin
    changelog.set(
        """
        # 1.2.0
        * Added settings for resolution, width/height factors, offsets, frame delay, loop count, background color, and so on.

        # 1.1.9
        * IPR!
        * Impressed from petpet by ThatWolf, Alyaxia
        """.trimIndent(),
    )
    // Image or Gif that will be shown at the top of your changelog page
    // changelogMedia.set("https://cool.png")

    // Add additional authors to this plugin
   // author("ThatWolf", 0L, hyperlink = false)
    //author("Alyaxia", 0L, hyperlink = false)
    // author("Name", 0L, hyperlink = true)

    // Excludes this plugin from publishing and global plugin repositories.
    // Set this to false if the plugin is unfinished
    deploy.set(true)

    // Builds and deploys this plugin but excludes it from global plugin repositories.
    // Set this if the plugin has reached EOL but a last update should still occur.
    // deployHidden.set(true)
}

