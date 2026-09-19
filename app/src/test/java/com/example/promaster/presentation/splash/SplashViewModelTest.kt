package com.example.promaster.presentation.splash

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun unauthenticatedUser_transitionsToUnauthenticated() = runTest(testDispatcher) {
        val viewModel = SplashViewModel(
            isAuthenticatedProvider = { false },
            isOnboardingCompletedProvider = { false }
        )

        testDispatcher.scheduler.advanceTimeBy(1300)
        testDispatcher.scheduler.runCurrent()

        assertEquals(StartupState.UNAUTHENTICATED, viewModel.startupState.value)
    }

    @Test
    fun authenticatedUser_withoutOnboarding_transitionsToOnboardingRequired() = runTest(testDispatcher) {
        val viewModel = SplashViewModel(
            isAuthenticatedProvider = { true },
            isOnboardingCompletedProvider = { false }
        )

        testDispatcher.scheduler.advanceTimeBy(1300)
        testDispatcher.scheduler.runCurrent()

        assertEquals(StartupState.ONBOARDING_REQUIRED, viewModel.startupState.value)
    }

    @Test
    fun authenticatedUser_withOnboardingCompleted_transitionsToReady() = runTest(testDispatcher) {
        val viewModel = SplashViewModel(
            isAuthenticatedProvider = { true },
            isOnboardingCompletedProvider = { true }
        )

        testDispatcher.scheduler.advanceTimeBy(1300)
        testDispatcher.scheduler.runCurrent()

        assertEquals(StartupState.READY, viewModel.startupState.value)
    }

    @Test
    fun authCheckFails_transitionsToUnauthenticatedSafely() = runTest(testDispatcher) {
        val viewModel = SplashViewModel(
            isAuthenticatedProvider = { throw RuntimeException("Auth service offline") },
            isOnboardingCompletedProvider = { false }
        )

        testDispatcher.scheduler.advanceTimeBy(1300)
        testDispatcher.scheduler.runCurrent()

        assertEquals(StartupState.UNAUTHENTICATED, viewModel.startupState.value)
    }
}
