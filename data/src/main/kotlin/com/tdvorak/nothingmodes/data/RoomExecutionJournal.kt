package com.tdvorak.nothingmodes.data

import com.tdvorak.nothingmodes.data.dao.ExecutionJournalDao
import com.tdvorak.nothingmodes.data.entities.ActionResultEntity
import com.tdvorak.nothingmodes.engine.runtime.ExecutionCompletion
import com.tdvorak.nothingmodes.engine.runtime.ExecutionJournal

class RoomExecutionJournal(private val dao: ExecutionJournalDao) : ExecutionJournal {

    override suspend fun finish(completion: ExecutionCompletion) {
        // The completion record is stored via FireClaimDao in a full implementation.
        // Individual action results are recorded as they execute.
    }
}
