package com.sina.library.data.enums

import com.sina.library.R


enum class AttachIcons(val iconName: String, val iconId: String, val iconColor: Int) {
    ICON_CAMERA("Camera", "f182", R.color.pick_camera),
    ICON_VIDEO("Video", "f181", R.color.pick_video),
    ICON_GALLERY("Gallery", "f260", R.color.pick_gallery),
    ICON_AUDIO("Audio", "f1aa", R.color.pick_audio),
    ICON_FILES("File", "f264", R.color.pick_file),
    ICON_LOCATION("Location", "f1a7", R.color.pick_gps),
    ICON_CONTACT("Contact", "f000", R.color.gps_disable),
    ICON_RECORDING("Recording", "f2ea", R.color.pick_voice),
}