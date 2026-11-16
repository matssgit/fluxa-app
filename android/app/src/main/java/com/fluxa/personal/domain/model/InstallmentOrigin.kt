package com.fluxa.personal.domain.model

import java.util.UUID

internal data class InstallmentOrigin(val installmentId: UUID, val purchaseId: UUID, val cardId: UUID)
