version = "1.0.0" // Plugin version. Increment this to trigger an update
description = "Add thread/channel delete button on long tab menu" // Plugin description that will be shown to user

aliucord {
    // Changelog of your plugin
    changelog.set(
        """
        # 1.0.0
        * Fixed not displaying dialog when entered chan/thread settings or created them.

        # 0.0.2
        * Fixed an issue where the screen had to be manually changed to delete.
        * some change

        # 0.0.1
        * Initial plugin release!
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
