version = "0.0.1" // Plugin version. Increment this to trigger an update
description = "Fixes friend request for new users" // Plugin description that will be shown to user

aliucord {
    // Changelog of your plugin
    changelog.set(
        """
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
