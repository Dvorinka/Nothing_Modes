package com.tdvorak.nothingmodes.data

import com.tdvorak.nothingmodes.engine.runtime.ModeActivationProvider
import com.tdvorak.nothingmodes.data.dao.ModeActivationDao

class RoomModeActivationProvider(
    private val dao: ModeActivationDao,
) : ModeActivationProvider {
    override suspend fun activeModeIds(): List<String> = dao.activeModeIds()
}
