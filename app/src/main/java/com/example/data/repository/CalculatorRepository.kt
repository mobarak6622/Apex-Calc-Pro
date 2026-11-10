package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CalculationRecord
import com.example.data.model.CloudBackupVault
import com.example.data.model.WorkspaceWindow
import com.example.security.CryptoVaultManager
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class CalculatorRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- 1. Calculation History Operations ---

    fun observeCalculations(): Flow<List<CalculationRecord>> = flow {
        val uid = auth.currentUser?.uid
        val targetUid = uid ?: "unauthenticated_probe"
        val path = "users/$targetUid/calculations"
        emitAll(
            db.collection("users")
                .document(targetUid)
                .collection("calculations")
                .whereEqualTo("userId", targetUid)
                .snapshots()
                .map { snapshot ->
                    val list = snapshot.toObjects(
                        CalculationRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    list.sortedWith(
                        compareByDescending<CalculationRecord> { it.isPinned }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createCalculation(
        workspaceId: String,
        title: String,
        expression: String,
        result: String,
        category: String = "SCIENTIFIC",
        isPinned: Boolean = false
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("calculations").document()
        val rawMeta = JSONObject().apply {
            put("expression", expression)
            put("result", result)
            put("title", title)
            put("workspaceId", workspaceId)
        }.toString()
        val encryptedBlob = CryptoVaultManager.encrypt(rawMeta)

        val payload = mapOf(
            "userId" to uid,
            "workspaceId" to workspaceId.take(64).ifBlank { "ws_primary" },
            "title" to title.take(120).ifBlank { "Calculation" },
            "expression" to expression.take(1000).ifBlank { "0" },
            "result" to result.take(500).ifBlank { "0" },
            "encryptedPayload" to encryptedBlob.take(4000),
            "isPinned" to isPinned,
            "category" to category.take(40).ifBlank { "SCIENTIFIC" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getCalculationById(ownerUid: String, calcId: String): Result<CalculationRecord?> {
        val docRef = db.collection("users").document(ownerUid).collection("calculations").document(calcId)
        return try {
            val snapshot = docRef.get().await()
            val record = snapshot.toObject(
                CalculationRecord::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(record)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCalculation(record: CalculationRecord): Result<Unit> {
        val uid = requireUserId()
        require(record.id.isNotBlank()) { "Calculation ID must not be blank for update" }
        requireNotNull(record.createdAt) { "Calculation createdAt must not be null for update" }
        val docRef = db.collection("users").document(uid).collection("calculations").document(record.id)
        return try {
            docRef.set(record.copy(updatedAt = null)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCalculation(calcId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("calculations").document(calcId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- 2. Multiple Calculation Windows (Workspaces) Operations ---

    fun observeWorkspaces(): Flow<List<WorkspaceWindow>> = flow {
        val uid = auth.currentUser?.uid
        val targetUid = uid ?: "unauthenticated_probe"
        val path = "users/$targetUid/workspaces"
        emitAll(
            db.collection("users")
                .document(targetUid)
                .collection("workspaces")
                .whereEqualTo("userId", targetUid)
                .snapshots()
                .map { snapshot ->
                    val list = snapshot.toObjects(
                        WorkspaceWindow::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    list.sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createWorkspace(
        name: String,
        currentExpression: String = "",
        currentResult: String = "0",
        memoryValue: String = "0",
        angleMode: String = "DEG",
        accentColorHex: String = "#00E5FF"
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("workspaces").document()
        val validAngle = if (angleMode == "RAD") "RAD" else "DEG"
        val payload = mapOf(
            "userId" to uid,
            "name" to name.take(80).ifBlank { "Workspace" },
            "currentExpression" to currentExpression.take(1000),
            "currentResult" to currentResult.take(500),
            "memoryValue" to memoryValue.take(100),
            "angleMode" to validAngle,
            "accentColorHex" to accentColorHex.take(16).ifBlank { "#00E5FF" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateWorkspace(window: WorkspaceWindow): Result<Unit> {
        val uid = requireUserId()
        require(window.id.isNotBlank()) { "Workspace ID must not be blank for update" }
        requireNotNull(window.createdAt) { "Workspace createdAt must not be null for update" }
        val docRef = db.collection("users").document(uid).collection("workspaces").document(window.id)
        return try {
            docRef.set(window.copy(updatedAt = null)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteWorkspace(workspaceId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("workspaces").document(workspaceId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- 3. Encrypted Cloud Backup Vault Operations ---

    fun observeBackups(): Flow<List<CloudBackupVault>> = flow {
        val uid = auth.currentUser?.uid
        val targetUid = uid ?: "unauthenticated_probe"
        val path = "users/$targetUid/backups"
        emitAll(
            db.collection("users")
                .document(targetUid)
                .collection("backups")
                .whereEqualTo("userId", targetUid)
                .snapshots()
                .map { snapshot ->
                    val list = snapshot.toObjects(
                        CloudBackupVault::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createCloudBackup(
        backupName: String,
        providerTag: String,
        rawJsonSnapshot: String,
        itemCount: Int
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("backups").document()
        val encrypted = CryptoVaultManager.encrypt(rawJsonSnapshot.ifBlank { "{}" }).take(50000)
        val checksum = CryptoVaultManager.sha256Hex(encrypted)
        val payload = mapOf(
            "userId" to uid,
            "backupName" to backupName.take(120).ifBlank { "Cloud Vault Snapshot" },
            "providerTag" to providerTag.take(40).ifBlank { "FIREBASE_CLOUD" },
            "encryptedData" to encrypted,
            "checksumSha256" to checksum,
            "itemCount" to itemCount.coerceIn(0, 10000).toLong(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudBackup(backupId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("backups").document(backupId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
