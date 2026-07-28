package com.jjrapps.aquihaytomate.data.repository

import com.jjrapps.aquihaytomate.data.local.datastore.TimerStateDataSource
import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.repository.TimerStateRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class TimerStateRepositoryImpl @Inject constructor(
    private val dataSource: TimerStateDataSource,
) : TimerStateRepository {

    override val state: Flow<TimerState> = dataSource.state

    override suspend fun current(): TimerState = dataSource.current()

    override suspend fun write(state: TimerState) = dataSource.write(state)

    override suspend fun update(transform: (TimerState) -> TimerState?): Boolean =
        dataSource.update(transform)
}
