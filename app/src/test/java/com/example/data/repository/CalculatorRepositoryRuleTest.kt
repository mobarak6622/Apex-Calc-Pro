package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CalculatorRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveCalculation_authenticatedOwner_emitsRealtimeRecord() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = CalculatorRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCalculation(
                workspaceId = "ws_main",
                title = "Quadratic Discriminant",
                expression = "b^2 - 4*a*c",
                result = "49",
                category = "SCIENTIFIC",
                isPinned = true
            )
        }
        assertTrue("Expected calculation creation to succeed", createResult.isSuccess)
        val calcId = createResult.getOrThrow()
        assertTrue(calcId.isNotEmpty())

        val records = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCalculations().first { list -> list.any { it.id == calcId } }
        }
        assertTrue(records.any { it.id == calcId && it.result == "49" })
    }

    @Test
    fun createWorkspaceAndBackup_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = CalculatorRepository(firestore)

        val wsResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createWorkspace(
                name = "Quantum Mechanics Window",
                currentExpression = "6.626e-34 * 5e14",
                currentResult = "3.313E-19",
                memoryValue = "3.313E-19",
                angleMode = "RAD",
                accentColorHex = "#00E5FF"
            )
        }
        assertTrue(wsResult.isSuccess)

        val backupResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCloudBackup(
                backupName = "Nightly Encrypted Vault",
                providerTag = "GOOGLE_DRIVE",
                rawJsonSnapshot = """{"workspaces":1,"calculations":1}""",
                itemCount = 2
            )
        }
        assertTrue(backupResult.isSuccess)
    }

    @Test
    fun getCalculationById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = CalculatorRepository(firestore)
        val calcId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createCalculation(
                workspaceId = "ws_1",
                title = "Alice Private Tax",
                expression = "150000 * 0.25",
                result = "37500"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = CalculatorRepository(firestore)
        val crossResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getCalculationById(aliceUid, calcId)
        }
        assertTrue("Expected cross-user access to fail", crossResult.isFailure)
        val firestoreEx = crossResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeCalculations_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = CalculatorRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeCalculations().first()
            }
            fail("Expected unauthenticated observation to throw PERMISSION_DENIED")
        } catch (e: Exception) {
            val firestoreEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_calc@test.com"
        const val BOB_EMAIL = "bob_calc@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
