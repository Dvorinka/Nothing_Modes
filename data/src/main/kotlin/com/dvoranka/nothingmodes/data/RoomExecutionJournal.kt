package com.dvoranka.nothingmodes.data

import com.dvoranka.nothingmodes.data.dao.ExecutionJournalDao
import com.dvoranka.nothingmodes.data.entities.ActionResultEntity
import com.dvoranka.nothingmodes.engine.runtime.ExecutionCompletion
import com.dvoranka.nothingmodes.engine.runtime.ExecutionJournal

class RoomExecutionJournal(private val dao: ExecutionJournalDao) : ExecutionJournal {

    override suspend fun finish(completion: ExecutionCompletion) {
        // The completion record is stored via FireClaimDao in a full implementation.
        // Individual action results are recorded as they execute.
    }
}
