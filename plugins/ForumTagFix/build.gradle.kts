version = "1.1.2" // Plugin version. Increment this to trigger an update
description = "Fix Tag When post forum" // Plugin description that will be shown to user

aliucord {
    // Changelog of your plugin
    changelog.set(
        """
        # 1.1.2
        * Remove Debug code
        * Change changelog lol

        # 1.1.1
        * Refactor UI Logic
        * background improvement
        * Text Fix.

        # 1.0.2
        * selectedTagIds.clear();
        * background color

        # 1.0.0
        * Initial plugin release!
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
