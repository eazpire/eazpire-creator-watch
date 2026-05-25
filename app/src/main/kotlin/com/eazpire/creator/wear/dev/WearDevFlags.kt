package com.eazpire.creator.wear.dev

import com.eazpire.creator.wear.BuildConfig

/**
 * Debug-only: set [LAUNCH_JOB_ANIM_GALLERY] to true to open the local loader gallery on start.
 * Flip to false to use the normal app while iterating on other screens.
 */
object WearDevFlags {
    const val LAUNCH_JOB_ANIM_GALLERY = true

    fun shouldLaunchJobAnimGallery(): Boolean = BuildConfig.DEBUG && LAUNCH_JOB_ANIM_GALLERY
}
