version = "1.0.2" // Plugin version. Increment this to trigger an update
description = "add /onboarding command to do onboarding" // Plugin description that will be shown to user

aliucord {
    // Changelog of your plugin
    changelog.set(
        """
        ## NOTE: This is temporary patch while waiting official backport!

        # 1.0.2
        * Change Title Indicator

        # 1.0.1
        * Better Exception?

        # 1.0.0
        * IPR!
        * refered from FixOnboarding by @scourage_main

        # JUST THINKING
        * If you heard there is a better onboarding plugin, I'm so sad.
        * At least, this plugin works in reality rather than being a mirage.
        * Then what is worse?
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
