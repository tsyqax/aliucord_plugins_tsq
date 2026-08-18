version = "1.0.3" // Plugin version. Increment this to trigger an update
description = "Add better UI something" // Plugin description that will be shown to user

aliucord {
    // Changelog of your plugin
    changelog.set(
        """
        # 1.0.3
        * Add PluralFix
        * Rewrite to Kotlin!
        * by lazy

        # 1.0.2
        * Add RuleChIcon

        # 1.0.1
        * Fixed some bug

        # 1.0.0
        * IPR!
        * Add ThreadDEL (Add button to delete)
        * Add ForumLine (Add gray line to first forum message)
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
