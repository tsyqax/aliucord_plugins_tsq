version = "1.1.4" // Plugin version. Increment this to trigger an update
description = "Add Method to do onboarding" // Plugin description that will be shown to user

aliucord {
    // Changelog of your plugin
    changelog.set(
        """
        # 1.1.4
        * Add support to change answers after complete onboarding
        * Better answer: if you did not choose, it did not selected
        * Better SeenTime

        # 1.1.3
        * Add support auto mode
        * This display onboarding if auto mode is on

        # 1.1.2
        * Add displaying emoji
        * Fix bug about title and improve UI

        # 1.1.1
        * Now can onboarding from guild menu
        * Guild Menu = the menu that appears when long-press guild icon

        # 1.1.0
        * Better UI: using SettingsPage
        * Support for dark theme: your eyes are now safe
        * Back to indicator of 1.0.1

        # 1.0.2
        * Change Title Indicator

        # 1.0.1
        * Better Exception?

        # 1.0.0
        * IPR!
        * refered from FixOnboarding by @scourage_main
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
