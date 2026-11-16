package com.fluxa.personal.ui.feature.credit

import com.fluxa.personal.data.repository.CreditRepository
import com.fluxa.personal.domain.model.CreditCard
import com.fluxa.personal.domain.model.CreditCardDetails
import com.fluxa.personal.domain.model.CreditCardSummary
import com.fluxa.personal.domain.model.CreditResult
import com.fluxa.personal.domain.model.FinancialTransaction
import com.fluxa.personal.domain.model.Installment
import com.fluxa.personal.domain.model.InstallmentOrigin
import com.fluxa.personal.domain.model.InstallmentPaymentResult
import com.fluxa.personal.domain.model.Purchase
import com.fluxa.personal.domain.model.PurchaseDraft
import com.fluxa.personal.domain.model.PurchaseStatus
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Recorte do [CreditRepository] usado pela UI, só para permitir testes JVM (o repositório usa
 * `RoomDatabase.withTransaction`). Não acrescenta regra: em produção cada método delega ao repositório do Codex.
 */
internal interface CreditSource {
    suspend fun createCard(details: CreditCardDetails): CreditResult<CreditCard>
    suspend fun updateCard(id: UUID, details: CreditCardDetails): CreditResult<CreditCard>
    suspend fun archiveCard(id: UUID): Boolean
    suspend fun createPurchase(id: UUID, draft: PurchaseDraft): CreditResult<Purchase>
    suspend fun cancelPurchase(id: UUID): CreditResult<Purchase>
    fun observeCards(includeArchived: Boolean = false): Flow<List<CreditCardSummary>>
    fun observeCard(id: UUID): Flow<CreditCardSummary?>
    fun observePurchases(cardId: UUID, status: PurchaseStatus? = null): Flow<List<Purchase>>
    fun observeInstallments(purchaseId: UUID): Flow<List<Installment>>
    suspend fun payInstallment(installmentId: UUID, accountId: UUID, paidOn: LocalDate): InstallmentPaymentResult
    suspend fun undoInstallmentPayment(installmentId: UUID, expectedTransactionId: UUID): InstallmentPaymentResult
    fun observeInstallmentPayment(installmentId: UUID): Flow<FinancialTransaction?>
    fun observeInstallmentOrigin(installmentId: UUID): Flow<InstallmentOrigin?>
}

internal class RepositoryCreditSource(private val repository: CreditRepository) : CreditSource {
    override suspend fun createCard(details: CreditCardDetails) = repository.createCard(details)
    override suspend fun updateCard(id: UUID, details: CreditCardDetails) = repository.updateCard(id, details)
    override suspend fun archiveCard(id: UUID) = repository.archiveCard(id)
    override suspend fun createPurchase(id: UUID, draft: PurchaseDraft) = repository.createPurchase(id, draft)
    override suspend fun cancelPurchase(id: UUID) = repository.cancelPurchase(id)
    override fun observeCards(includeArchived: Boolean) = repository.observeCards(includeArchived)
    override fun observeCard(id: UUID) = repository.observeCard(id)
    override fun observePurchases(cardId: UUID, status: PurchaseStatus?) = repository.observePurchases(cardId, status)
    override fun observeInstallments(purchaseId: UUID) = repository.observeInstallments(purchaseId)
    override suspend fun payInstallment(installmentId: UUID, accountId: UUID, paidOn: LocalDate) =
        repository.payInstallment(installmentId, accountId, paidOn)
    override suspend fun undoInstallmentPayment(installmentId: UUID, expectedTransactionId: UUID) =
        repository.undoInstallmentPayment(installmentId, expectedTransactionId)
    override fun observeInstallmentPayment(installmentId: UUID) = repository.observeInstallmentPayment(installmentId)
    override fun observeInstallmentOrigin(installmentId: UUID) = repository.observeInstallmentOrigin(installmentId)
}
