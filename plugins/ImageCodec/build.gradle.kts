version = "0.0.2" // Plugin version. Increment this to trigger an update
description = "Prevent image to be file" // Plugin description that will be shown to user

aliucord {
    // Changelog of your plugin
    changelog.set(
        """
        # 0.0.2
        * Rewrite to Kotlin!
        * by lazy
        
        # 0.0.1
        * IPR!
        * Added: heic, heif, hif, bmp, avif, dib
        * Added: jfif, jfi, jpe, pjpeg, pjpg
        * Added: apng
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
