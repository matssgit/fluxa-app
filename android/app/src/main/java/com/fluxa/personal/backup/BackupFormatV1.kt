package com.fluxa.personal.backup

import com.google.gson.JsonObject
import java.time.Instant

internal const val FLUXA_BACKUP_MIME = "application/vnd.fluxa.backup"
internal const val FLUXA_BACKUP_EXTENSION = ".fluxa"

internal enum class BackupError {
    INVALID, CORRUPTED, VERSION_UNSUPPORTED, INTEGRITY_FAILED, RELATION_INVALID,
    EXPORT_FAILED, RESTORE_FAILED, RESTORE_CONFLICT, TOO_LARGE,
    PASSWORD_REQUIRED, PASSWORD_TOO_SHORT, PASSWORD_INVALID_ENCODING, INVALID_PASSWORD,
    CRYPTO_UNSUPPORTED, DECRYPTION_FAILED, ENCRYPTION_FAILED,
}

internal sealed interface BackupResult<out T> {
    data class Success<T>(val value: T) : BackupResult<T>
    data class Failure(val error: BackupError) : BackupResult<Nothing>
}

internal class BackupException(val error: BackupError) : Exception(error.name)

internal data class BackupManifestV1(
    val formatVersion: Int,
    val createdAt: Instant,
    val appVersion: String,
    val databaseSchemaVersion: Int,
    val currency: String,
    val encryption: String,
    val dataSha256: String,
    val recordCounts: Map<String, Int>,
)

internal data class PortableSettingsV1(val theme: String, val hideAmounts: Boolean)

/** Versioned records use the fixed field contracts below, independently of Room entities. */
internal data class BackupRowV1(val fields: JsonObject)
internal data class BackupDataV1(
    val tables: Map<String, List<BackupRowV1>>,
    val settings: PortableSettingsV1,
)
internal data class BackupDocumentV1(val manifest: BackupManifestV1, val data: BackupDataV1)
internal class PreparedFluxaBackup internal constructor(internal val document: BackupDocumentV1) {
    val manifest: BackupManifestV1 get() = document.manifest
}
internal data class RestoreReceipt(val settingsApplied: Boolean)

internal enum class BackupFieldType { TEXT, LONG, BOOLEAN, DATE, INSTANT, UUID, ENUM }
internal data class FieldV1(
    val name: String,
    val type: BackupFieldType,
    val nullable: Boolean,
    val storedAsDateText: Boolean,
    val options: Set<String> = emptySet(),
)
internal data class ReferenceV1(val field: String, val table: String, val target: String)
internal data class TableV1(
    val name: String,
    val fields: List<FieldV1>,
    val keys: List<String>,
    val references: List<ReferenceV1>,
)

/** Frozen v1 wire contract. Future database changes belong in the persistence adapter. */
internal object BackupFormatV1 {
    const val FORMAT_VERSION = 1
    const val SCHEMA_VERSION = 7
    val tables = listOf(
        TableV1("local_schema_marker", listOf(
            FieldV1("id", BackupFieldType.LONG, nullable = false, storedAsDateText = false),
        ), keys = listOf("id"), references = listOf()),
        TableV1("accounts", listOf(
            FieldV1("id", BackupFieldType.UUID, nullable = false, storedAsDateText = false),
            FieldV1("name", BackupFieldType.TEXT, nullable = false, storedAsDateText = false),
            FieldV1("type", BackupFieldType.ENUM, nullable = false, storedAsDateText = false, options = setOf("CHECKING", "WALLET", "SAVINGS")),
            FieldV1("openingBalanceCents", BackupFieldType.LONG, nullable = false, storedAsDateText = false),
            FieldV1("openingDate", BackupFieldType.DATE, nullable = false, storedAsDateText = true),
            FieldV1("includeInAvailable", BackupFieldType.BOOLEAN, nullable = false, storedAsDateText = false),
            FieldV1("createdAt", BackupFieldType.INSTANT, nullable = false, storedAsDateText = false),
            FieldV1("updatedAt", BackupFieldType.INSTANT, nullable = false, storedAsDateText = false),
            FieldV1("archivedAt", BackupFieldType.INSTANT, nullable = true, storedAsDateText = false),
        ), keys = listOf("id"), references = listOf()),
        TableV1("categories", listOf(
            FieldV1("id", BackupFieldType.UUID, nullable = false, storedAsDateText = false),
            FieldV1("name", BackupFieldType.TEXT, nullable = false, storedAsDateText = false),
            FieldV1("kind", BackupFieldType.ENUM, nullable = false, storedAsDateText = false, options = setOf("INCOME", "EXPENSE")),
            FieldV1("icon", BackupFieldType.TEXT, nullable = true, storedAsDateText = false),
            FieldV1("color", BackupFieldType.TEXT, nullable = true, storedAsDateText = false),
            FieldV1("archivedAt", BackupFieldType.INSTANT, nullable = true, storedAsDateText = false),
        ), keys = listOf("id"), references = listOf()),
        TableV1("credit_cards", listOf(
            FieldV1("id", BackupFieldType.UUID, nullable = false, storedAsDateText = false),
            FieldV1("name", BackupFieldType.TEXT, nullable = false, storedAsDateText = false),
            FieldV1("totalLimitCents", BackupFieldType.LONG, nullable = false, storedAsDateText = false),
            FieldV1("closingDay", BackupFieldType.LONG, nullable = false, storedAsDateText = false),
            FieldV1("dueDay", BackupFieldType.LONG, nullable = false, storedAsDateText = false),
            FieldV1("color", BackupFieldType.TEXT, nullable = true, storedAsDateText = false),
            FieldV1("archivedAt", BackupFieldType.INSTANT, nullable = true, storedAsDateText = false),
            FieldV1("createdAt", BackupFieldType.INSTANT, nullable = false, storedAsDateText = false),
            FieldV1("updatedAt", BackupFieldType.INSTANT, nullable = false, storedAsDateText = false),
        ), keys = listOf("id"), references = listOf()),
        TableV1("subscriptions", listOf(
            FieldV1("id", BackupFieldType.UUID, nullable = false, storedAsDateText = false),
            FieldV1("frequency", BackupFieldType.ENUM, nullable = false, storedAsDateText = false, options = setOf("MONTHLY", "YEARLY")),
            FieldV1("anchorDate", BackupFieldType.DATE, nullable = false, storedAsDateText = false),
            FieldV1("billingDay", BackupFieldType.LONG, nullable = false, storedAsDateText = false),
            FieldV1("status", BackupFieldType.ENUM, nullable = false, storedAsDateText = false, options = setOf("ACTIVE", "PAUSED", "CANCELLED")),
            FieldV1("createdAt", BackupFieldType.INSTANT, nullable = false, storedAsDateText = false),
            FieldV1("updatedAt", BackupFieldType.INSTANT, nullable = false, storedAsDateText = false),
            FieldV1("pausedAt", BackupFieldType.INSTANT, nullable = true, storedAsDateText = false),
            FieldV1("cancelledAt", BackupFieldType.INSTANT, nullable = true, storedAsDateText = false),
            FieldV1("name", BackupFieldType.TEXT, nullable = false, storedAsDateText = false),
            FieldV1("amountCents", BackupFieldType.LONG, nullable = false, storedAsDateText = false),
            FieldV1("categoryId", BackupFieldType.UUID, nullable = false, storedAsDateText = false),
            FieldV1("accountId", BackupFieldType.UUID, nullable = true, storedAsDateText = false),
            FieldV1("cardId", BackupFieldType.UUID, nullable = true, storedAsDateText = false),
        ), keys = listOf("id"), references = listOf(ReferenceV1("categoryId", "categories", "id"), ReferenceV1("accountId", "accounts", "id"), ReferenceV1("cardId", "credit_cards", "id"))),
        TableV1("credit_purchases", listOf(
            FieldV1("id", BackupFieldType.UUID, nullable = false, storedAsDateText = false),
            FieldV1("cardId", BackupFieldType.UUID, nullable = false, storedAsDateText = false),
            FieldV1("categoryId", BackupFieldType.UUID, nullable = false, storedAsDateText = false),
            FieldV1("title", BackupFieldType.TEXT, nullable = false, storedAsDateText = false),
            FieldV1("merchant", BackupFieldType.TEXT, nullable = true, storedAsDateText = false),
            FieldV1("notes", BackupFieldType.TEXT, nullable = true, storedAsDateText = false),
            FieldV1("totalCents", BackupFieldType.LONG, nullable = false, storedAsDateText = false),
            FieldV1("installmentCount", BackupFieldType.LONG, nullable = false, storedAsDateText = false),
            FieldV1("purchaseDate", BackupFieldType.DATE, nullable = false, storedAsDateText = false),
            FieldV1("firstDueDate", BackupFieldType.DATE, nullable = false, storedAsDateText = false),
            FieldV1("createdAt", BackupFieldType.INSTANT, nullable = false, storedAsDateText = false),
            FieldV1("updatedAt", BackupFieldType.INSTANT, nullable = false, storedAsDateText = false),
            FieldV1("cancelledAt", BackupFieldType.INSTANT, nullable = true, storedAsDateText = false),
            FieldV1("subscriptionId", BackupFieldType.UUID, nullable = true, storedAsDateText = false),
            FieldV1("occurrenceDate", BackupFieldType.DATE, nullable = true, storedAsDateText = false),
        ), keys = listOf("id"), references = listOf(ReferenceV1("cardId", "credit_cards", "id"), ReferenceV1("categoryId", "categories", "id"), ReferenceV1("subscriptionId", "subscriptions", "id"))),
        TableV1("credit_installments", listOf(
            FieldV1("id", BackupFieldType.UUID, nullable = false, storedAsDateText = false),
            FieldV1("purchaseId", BackupFieldType.UUID, nullable = false, storedAsDateText = false),
            FieldV1("number", BackupFieldType.LONG, nullable = false, storedAsDateText = false),
            FieldV1("amountCents", BackupFieldType.LONG, nullable = false, storedAsDateText = false),
            FieldV1("dueDate", BackupFieldType.DATE, nullable = false, storedAsDateText = false),
            FieldV1("status", BackupFieldType.ENUM, nullable = false, storedAsDateText = false, options = setOf("PENDING", "PAID", "CANCELLED")),
        ), keys = listOf("id"), references = listOf(ReferenceV1("purchaseId", "credit_purchases", "id"))),
        TableV1("financial_transactions", listOf(
            FieldV1("id", BackupFieldType.UUID, nullable = false, storedAsDateText = false),
            FieldV1("accountId", BackupFieldType.UUID, nullable = false, storedAsDateText = false),
            FieldV1("categoryId", BackupFieldType.UUID, nullable = false, storedAsDateText = false),
            FieldV1("direction", BackupFieldType.ENUM, nullable = false, storedAsDateText = false, options = setOf("INCOME", "EXPENSE")),
            FieldV1("amountCents", BackupFieldType.LONG, nullable = false, storedAsDateText = false),
            FieldV1("occurredOn", BackupFieldType.DATE, nullable = false, storedAsDateText = false),
            FieldV1("description", BackupFieldType.TEXT, nullable = true, storedAsDateText = false),
            FieldV1("createdAt", BackupFieldType.INSTANT, nullable = false, storedAsDateText = false),
            FieldV1("updatedAt", BackupFieldType.INSTANT, nullable = false, storedAsDateText = false),
            FieldV1("cancelledAt", BackupFieldType.INSTANT, nullable = true, storedAsDateText = false),
            FieldV1("installmentId", BackupFieldType.UUID, nullable = true, storedAsDateText = false),
            FieldV1("subscriptionId", BackupFieldType.UUID, nullable = true, storedAsDateText = false),
            FieldV1("occurrenceDate", BackupFieldType.DATE, nullable = true, storedAsDateText = false),
        ), keys = listOf("id"), references = listOf(ReferenceV1("accountId", "accounts", "id"), ReferenceV1("categoryId", "categories", "id"), ReferenceV1("installmentId", "credit_installments", "id"), ReferenceV1("subscriptionId", "subscriptions", "id"))),
        TableV1("subscription_terms", listOf(
            FieldV1("subscriptionId", BackupFieldType.UUID, nullable = false, storedAsDateText = false),
            FieldV1("effectiveOn", BackupFieldType.DATE, nullable = false, storedAsDateText = false),
            FieldV1("enabled", BackupFieldType.BOOLEAN, nullable = false, storedAsDateText = false),
            FieldV1("name", BackupFieldType.TEXT, nullable = false, storedAsDateText = false),
            FieldV1("amountCents", BackupFieldType.LONG, nullable = false, storedAsDateText = false),
            FieldV1("categoryId", BackupFieldType.UUID, nullable = false, storedAsDateText = false),
            FieldV1("accountId", BackupFieldType.UUID, nullable = true, storedAsDateText = false),
            FieldV1("cardId", BackupFieldType.UUID, nullable = true, storedAsDateText = false),
        ), keys = listOf("subscriptionId", "effectiveOn"), references = listOf(ReferenceV1("subscriptionId", "subscriptions", "id"))),
        TableV1("subscription_resolutions", listOf(
            FieldV1("subscriptionId", BackupFieldType.UUID, nullable = false, storedAsDateText = false),
            FieldV1("occurrenceDate", BackupFieldType.DATE, nullable = false, storedAsDateText = false),
            FieldV1("status", BackupFieldType.ENUM, nullable = false, storedAsDateText = false, options = setOf("PENDING", "PAID", "CHARGED", "SKIPPED")),
            FieldV1("resultId", BackupFieldType.UUID, nullable = true, storedAsDateText = false),
            FieldV1("name", BackupFieldType.TEXT, nullable = false, storedAsDateText = false),
            FieldV1("amountCents", BackupFieldType.LONG, nullable = false, storedAsDateText = false),
            FieldV1("categoryId", BackupFieldType.UUID, nullable = false, storedAsDateText = false),
            FieldV1("accountId", BackupFieldType.UUID, nullable = true, storedAsDateText = false),
            FieldV1("cardId", BackupFieldType.UUID, nullable = true, storedAsDateText = false),
        ), keys = listOf("subscriptionId", "occurrenceDate"), references = listOf(ReferenceV1("subscriptionId", "subscriptions", "id"))),
        TableV1("goals", listOf(
            FieldV1("id", BackupFieldType.UUID, nullable = false, storedAsDateText = false),
            FieldV1("name", BackupFieldType.TEXT, nullable = false, storedAsDateText = false),
            FieldV1("targetAmountCents", BackupFieldType.LONG, nullable = false, storedAsDateText = false),
            FieldV1("targetDate", BackupFieldType.DATE, nullable = true, storedAsDateText = false),
            FieldV1("accountId", BackupFieldType.UUID, nullable = false, storedAsDateText = false),
            FieldV1("createdAt", BackupFieldType.INSTANT, nullable = false, storedAsDateText = false),
            FieldV1("updatedAt", BackupFieldType.INSTANT, nullable = false, storedAsDateText = false),
            FieldV1("completedAt", BackupFieldType.INSTANT, nullable = true, storedAsDateText = false),
            FieldV1("archivedAt", BackupFieldType.INSTANT, nullable = true, storedAsDateText = false),
        ), keys = listOf("id"), references = listOf(ReferenceV1("accountId", "accounts", "id"))),
        TableV1("goal_movements", listOf(
            FieldV1("id", BackupFieldType.UUID, nullable = false, storedAsDateText = false),
            FieldV1("goalId", BackupFieldType.UUID, nullable = false, storedAsDateText = false),
            FieldV1("type", BackupFieldType.ENUM, nullable = false, storedAsDateText = false, options = setOf("ALLOCATE", "RELEASE")),
            FieldV1("amountCents", BackupFieldType.LONG, nullable = false, storedAsDateText = false),
            FieldV1("occurredOn", BackupFieldType.DATE, nullable = false, storedAsDateText = false),
            FieldV1("createdAt", BackupFieldType.INSTANT, nullable = false, storedAsDateText = false),
        ), keys = listOf("id"), references = listOf(ReferenceV1("goalId", "goals", "id"))),
    )
}
