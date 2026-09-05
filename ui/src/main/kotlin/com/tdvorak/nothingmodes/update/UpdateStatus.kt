package com.tdvorak.nothingmodes.update

enum class UpdateStatus {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    AVAILABLE,
    DOWNLOADING,
    DOWNLOADED,
    NEEDS_PERMISSION,
}
