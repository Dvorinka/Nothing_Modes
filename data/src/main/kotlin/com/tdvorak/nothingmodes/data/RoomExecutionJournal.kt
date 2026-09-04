package com.tdvorak.nothingmodes.data

import com.tdvorak.nothingmodes.data.dao.ExecutionJournalDao
import com.tdvorak.nothingmodes.data.dao.FireClaimDao
import com.tdvorak.nothingmodes.data.entities.ActionResultEntity
import com.tdvorak.nothingmodes.data.entities.FireClaimEntity
import com.tdvorak.nothingmodes.engine.runtime.ExecutionCompletion
import com.tdvorak.nothingmodes.engine.runtime.ExecutionJournal
import com.tdvorak.nothingmodes.engine.runtime.ExecutionStatus

class RoomExecutionJournal(
    private val dao: ExecutionJournalDao,
    private val fireClaimDao: FireClaimDao,
) : ExecutionJournal {

    override suspend fun finish(completion: ExecutionCompletion) {
        val succeeded = if (completion.status == ExecutionStatus.COMPLETED) completion.actionCount else 0
        val failed = completion.actionCount - succeeded
        val status = when (completion.status) {
            ExecutionStatus.COMPLETED -> "COMPLETED"
            ExecutionStatus.FAILED -> "FAILED"
            ExecutionStatus.CANCELLED -> "CANCELLED"
            ExecutionStatus.SUPPRESSED_COOLDOWN -> "SUPPRESSED"
            ExecutionStatus.SUPPRESSED_NOT_ELIGIBLE -> "SUPPRESSED"
        }
        fireClaimDao.complete(
            executionId = completion.executionId,
            status = status,
            atMillis = completion.atMillis,
            succeeded = succeeded,
            failed = failed,
        )
    }
}
