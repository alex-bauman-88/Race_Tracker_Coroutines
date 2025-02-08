/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.racetracker

import com.example.racetracker.ui.RaceParticipant
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic

// Higher-order function to handle mock lifecycle
private fun withMockedLog(block: () -> Unit) {
    try {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.e(any(), any()) } returns 0
        block()
    } finally {
        unmockkStatic(android.util.Log::class)
    }
}

class RaceParticipantTest {
    private val raceParticipant = RaceParticipant(
        name = "Test",
        maxProgress = 100,
        progressDelayMillis = 500L,
        initialProgress = 0,
        progressIncrement = 1
    )

    // The runTest builder creates a test coroutine scope where we can control virtual time.
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun raceParticipant_RaceStarted_ProgressUpdated() = runTest {
        val expectedProgress = 1
        launch { raceParticipant.run() } // this coroutine runs concurrently with the rest of the test
        advanceTimeBy(raceParticipant.progressDelayMillis) // simulates the passage of time without actually waiting
        runCurrent() // executes any coroutines that are ready to run after the time advancement ("the virtual time has passed, now execute the code that was waiting for that delay")
        assertEquals(expectedProgress, raceParticipant.currentProgress)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun raceParticipant_RaceFinished_ProgressUpdated() = runTest {
        launch { raceParticipant.run() }
        advanceTimeBy(raceParticipant.maxProgress * raceParticipant.progressDelayMillis)
        runCurrent()
        assertEquals(raceParticipant.maxProgress, raceParticipant.currentProgress)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun raceParticipant_RacePaused_ProgressUpdated() {
        withMockedLog {
            runTest {
                val expectedProgress = 5
                val racerJob = launch { raceParticipant.run() }
                advanceTimeBy(expectedProgress * raceParticipant.progressDelayMillis)
                runCurrent()
                racerJob.cancelAndJoin()
                assertEquals(expectedProgress, raceParticipant.currentProgress)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun raceParticipant_RacePausedAndResumed_ProgressUpdated() {
        withMockedLog {
            runTest {
                val expectedProgress = 5
                repeat(2) {
                    val racerJob = launch { raceParticipant.run() }
                    advanceTimeBy(expectedProgress * raceParticipant.progressDelayMillis)
                    runCurrent()
                    racerJob.cancelAndJoin()
                }
                assertEquals(expectedProgress * 2, raceParticipant.currentProgress)
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun raceParticipant_ProgressIncrementZero_ExceptionThrown() = runTest {
        RaceParticipant(name = "Progress Test", progressIncrement = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun raceParticipant_MaxProgressZero_ExceptionThrown() {
        RaceParticipant(name = "Progress Test", maxProgress = 0)
    }
}
