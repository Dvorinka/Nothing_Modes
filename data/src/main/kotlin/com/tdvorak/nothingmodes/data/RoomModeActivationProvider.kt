package com.tdvorak.nothingmodes.data

import com.tdvorak.nothingmodes.data.dao.ModeActivationDao
import com.tdvorak.nothingmodes.engine.runtime.ModeActivationProvider

class RoomModeActivationProvider(
    private val dao: ModeActivationDao,
) : ModeActivationProvider {
    override suspend fun activeModeIds(): List<String> = dao.activeModeIds()
}
