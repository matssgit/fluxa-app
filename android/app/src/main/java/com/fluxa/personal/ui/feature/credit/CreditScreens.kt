package com.fluxa.personal.ui.feature.credit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fluxa.personal.R
import com.fluxa.personal.domain.model.CreditCardSummary
import com.fluxa.personal.domain.model.Installment
import com.fluxa.personal.domain.model.InstallmentStatus
import com.fluxa.personal.ui.common.BrazilDate
import com.fluxa.personal.ui.common.ConfirmArchiveSheet
import com.fluxa.personal.ui.common.DateField
import com.fluxa.personal.ui.common.EmptyStateCard
import com.fluxa.personal.ui.common.ErrorBanner
import com.fluxa.personal.ui.common.FeatureScreen
import com.fluxa.personal.ui.common.FieldError
import com.fluxa.personal.ui.common.FieldLabel
import com.fluxa.personal.ui.common.FluxaDatePicker
import com.fluxa.personal.ui.common.LoadingBlock
import com.fluxa.personal.ui.common.StatePill
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.components.FluxaAmount
import com.fluxa.personal.ui.designsystem.components.FluxaButton
import com.fluxa.personal.ui.designsystem.components.FluxaButtonSize
import com.fluxa.personal.ui.designsystem.components.FluxaButtonVariant
import com.fluxa.personal.ui.designsystem.components.FluxaCard
import com.fluxa.personal.ui.designsystem.components.FluxaChip
import com.fluxa.personal.ui.designsystem.components.FluxaInput
import com.fluxa.personal.ui.designsystem.components.FluxaSectionHeader
import com.fluxa.personal.ui.designsystem.components.FluxaSurfaceTone
import com.fluxa.personal.ui.feature.cash.TabScreen
import com.fluxa.personal.ui.feature.catalog.CategoryColors
import com.fluxa.personal.ui.feature.catalog.categoryColorFor
import com.fluxa.personal.ui.feature.catalog.categoryIconFor
import java.util.UUID

internal object CreditRoutes {
    const val ARG_CARD = "card"
    const val ARG_PURCHASE = "purchase"
    const val ARG_INSTALLMENT = "installment"
    const val NEW_CARD = "cards/new"
    const val CARD_DETAIL = "cards/{$ARG_CARD}"
    const val EDIT_CARD = "cards/{$ARG_CARD}/edit"
    const val NEW_PURCHASE = "purchases/new?$ARG_CARD={$ARG_CARD}"
    const val PURCHASE_DETAIL = "cards/{$ARG_CARD}/purchases/{$ARG_PURCHASE}"
    const val INSTALLMENT_DETAIL = "cards/{$ARG_CARD}/purchases/{$ARG_PURCHASE}/installments/{$ARG_INSTALLMENT}"
    fun detail(id: UUID) = "cards/$id"
    fun edit(id: UUID) = "cards/$id/edit"
    fun newPurchase(cardId: UUID?) = if (cardId == null) "purchases/new" else "purchases/new?$ARG_CARD=$cardId"
    fun purchase(cardId: UUID, purchaseId: UUID) = "cards/$cardId/purchases/$purchaseId"
    fun installment(cardId: UUID, purchaseId: UUID, installmentId: UUID) =
        "cards/$cardId/purchases/$purchaseId/installments/$installmentId"
}

// region Cartão visual

/**
 * Cartão de crédito no estilo Fluxa: superfície escura (verde petróleo), forma folha, anel orgânico discreto na cor
 * escolhida. Sem marca ou identidade de banco. Os valores vêm de `CreditCardSummary` e passam por `FluxaAmount`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CreditCardVisual(summary: CreditCardSummary, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    val details = summary.card.details
    val accent = categoryColorFor(details.color)
    val archived = summary.card.archivedAt != null
    FluxaCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card-${details.name}"),
        tone = FluxaSurfaceTone.Inverse,
        featured = true,
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Forma orgânica discreta: dois anéis na cor de destaque, sem logotipo.
                    val r = size.minDimension * 0.42f
                    drawCircle(accent.copy(alpha = 0.35f), radius = r, center = Offset(size.width - r * 0.35f, r * 0.2f), style = Stroke(width = r * 0.22f))
                    drawCircle(accent.copy(alpha = 0.18f), radius = r * 0.55f, center = Offset(size.width - r * 0.2f, size.height - r * 0.1f))
                }
                .padding(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(12.dp).background(accent, FluxaTheme.shapes.pill))
                Text(
                    details.name,
                    style = type.titleSmall,
                    color = colors.onSurfaceInverse,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (archived) {
                    Text(
                        stringResource(R.string.card_archived_badge),
                        style = type.caption,
                        color = colors.onSurfaceInverse,
                        modifier = Modifier
                            .border(1.dp, colors.onSurfaceInverseMuted, FluxaTheme.shapes.pill)
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.card_available).uppercase(), style = type.overline, color = colors.onSurfaceInverseMuted)
            FluxaAmount(
                amount = summary.availableLimit,
                style = type.headline.merge(type.amount).copy(fontSize = type.headline.fontSize),
                color = colors.onSurfaceInverse,
                modifier = Modifier.testTag("card-available-${details.name}"),
            )
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledAmount(stringResource(R.string.card_committed), summary.committedAmount, "card-committed-${details.name}")
                LabeledAmount(stringResource(R.string.card_total_limit), summary.totalLimit, "card-total-${details.name}")
            }
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.card_closes_on, details.closingDay.day), style = type.label, color = colors.onSurfaceInverseMuted)
                Text(stringResource(R.string.card_due_on, details.dueDay.day), style = type.label, color = colors.onSurfaceInverseMuted)
            }
        }
    }
}

@Composable
private fun LabeledAmount(label: String, amount: com.fluxa.personal.core.money.Money, tag: String) {
    Column(modifier = Modifier.semantics(mergeDescendants = true) {}.testTag(tag)) {
        Text(label, style = FluxaTheme.typography.caption, color = FluxaTheme.colors.onSurfaceInverseMuted)
        FluxaAmount(amount = amount, color = FluxaTheme.colors.onSurfaceInverse)
    }
}

// endregion

// region Lista

@Composable
internal fun CardsScreen(
    viewModel: CardsViewModel,
    onToggleAmounts: () -> Unit,
    onCreate: () -> Unit,
    onOpen: (UUID) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TabScreen(route = "cards", title = stringResource(R.string.cards_title), overline = stringResource(R.string.cards_overline), onToggleAmounts = onToggleAmounts) {
        when {
            state.isLoading -> item { LoadingBlock() }
            state.errorMessage != null -> item { ErrorBanner(stringResource(state.errorMessage!!), onDismiss = null) }
            else -> {
                if (state.active.isEmpty()) {
                    item {
                        EmptyStateCard(stringResource(R.string.cards_empty_title), stringResource(R.string.cards_empty_body), FluxaIcons.Card)
                    }
                }
                items(state.active, key = { it.card.id }) { summary ->
                    CreditCardVisual(summary, onClick = { onOpen(summary.card.id) })
                }
                item {
                    FluxaButton(
                        text = stringResource(R.string.card_new),
                        onClick = onCreate,
                        leadingIcon = FluxaIcons.Plus,
                        variant = if (state.active.isEmpty()) FluxaButtonVariant.Primary else FluxaButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth().testTag("action-new-card"),
                    )
                }
                if (state.archived.isNotEmpty()) {
                    item {
                        FluxaButton(
                            text = if (state.showArchived) stringResource(R.string.cards_hide_archived)
                            else stringResource(R.string.cards_show_archived, state.archived.size),
                            onClick = { viewModel.setShowArchived(!state.showArchived) },
                            variant = FluxaButtonVariant.Secondary,
                            leadingIcon = FluxaIcons.Archive,
                            modifier = Modifier.testTag("toggle-archived-cards"),
                        )
                    }
                    if (state.showArchived) {
                        item { FluxaSectionHeader(title = stringResource(R.string.cards_archived_section)) }
                        items(state.archived, key = { it.card.id }) { summary ->
                            CreditCardVisual(summary, onClick = { onOpen(summary.card.id) })
                        }
                    }
                }
            }
        }
    }
}

// endregion

// region Detalhe do cartão

@Composable
internal fun CardDetailScreen(
    viewModel: CardDetailViewModel,
    onBack: () -> Unit,
    onEdit: (UUID) -> Unit,
    onNewPurchase: (UUID) -> Unit,
    onOpenPurchase: (UUID, UUID) -> Unit,
    onOpenInstallment: (UUID, UUID, UUID) -> Unit = { _, _, _ -> },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val summary = state.summary
    FeatureScreen(
        title = summary?.card?.details?.name ?: stringResource(R.string.card_detail_overline),
        overline = stringResource(R.string.card_detail_overline),
        onBack = onBack,
        modifier = Modifier.testTag("screen-card-detail"),
    ) {
        when {
            state.isLoading -> item { LoadingBlock() }
            state.errorMessage != null -> item { ErrorBanner(stringResource(state.errorMessage!!), onDismiss = null) }
            state.notFound || summary == null -> item { ErrorBanner(stringResource(R.string.error_card_not_found), onDismiss = null) }
            else -> {
                val cardId = summary.card.id
                val archived = summary.card.archivedAt != null
                item { CreditCardVisual(summary) }
                item { Text(stringResource(R.string.card_limit_explain), style = FluxaTheme.typography.caption, color = FluxaTheme.colors.textMuted) }
                if (archived) item { StatePill(stringResource(R.string.card_archived_note), FluxaIcons.Archive) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!archived) {
                            FluxaButton(
                                text = stringResource(R.string.new_purchase),
                                onClick = { onNewPurchase(cardId) },
                                leadingIcon = FluxaIcons.Plus,
                                modifier = Modifier.weight(1f).testTag("action-card-new-purchase"),
                            )
                        }
                        FluxaButton(
                            text = stringResource(R.string.card_edit),
                            onClick = { onEdit(cardId) },
                            variant = FluxaButtonVariant.Secondary,
                            modifier = Modifier.weight(1f).testTag("action-edit-card"),
                        )
                    }
                }
                item { FluxaSectionHeader(title = stringResource(R.string.card_detail_upcoming)) }
                if (state.upcoming.isEmpty()) {
                    item { Text(stringResource(R.string.card_detail_no_upcoming), style = FluxaTheme.typography.body, color = FluxaTheme.colors.textSecondary) }
                } else {
                    item {
                        FluxaCard(modifier = Modifier.fillMaxWidth().testTag("upcoming-list"), contentPadding = PaddingValues(vertical = 4.dp)) {
                            state.upcoming.take(UPCOMING_SHOWN).forEachIndexed { index, up ->
                                if (index > 0) Divider()
                                InstallmentRow(
                                    up.installment, up.count, title = up.purchase.details.title,
                                    onClick = { onOpenInstallment(up.purchase.details.cardId, up.purchase.id, up.installment.id) },
                                )
                            }
                        }
                    }
                }
                item { FluxaSectionHeader(title = stringResource(R.string.card_detail_purchases)) }
                if (state.purchases.isEmpty()) {
                    item { Text(stringResource(R.string.card_detail_no_purchases), style = FluxaTheme.typography.body, color = FluxaTheme.colors.textSecondary) }
                } else {
                    items(state.purchases, key = { it.purchase.id }) { item ->
                        PurchaseRow(item, onClick = { onOpenPurchase(cardId, item.purchase.id) })
                    }
                }
            }
        }
    }
}

private const val UPCOMING_SHOWN = 12

@Composable
private fun Divider() {
    Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(FluxaTheme.colors.border))
}

@Composable
internal fun PurchaseRow(item: PurchaseItem, onClick: () -> Unit) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    val details = item.purchase.details
    val cancelled = item.purchase.cancelledAt != null
    val meta = listOfNotNull(details.merchant, item.category?.details?.name, details.purchaseDate.format(BrazilDate)).joinToString(" · ")
    val statusText = stringResource(if (cancelled) R.string.purchase_status_cancelled else R.string.purchase_status_active)
    FluxaCard(
        modifier = Modifier.fillMaxWidth().testTag("purchase-${details.title}"),
        onClick = onClick,
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.semantics { stateDescription = statusText }) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(categoryColorFor(item.category?.details?.color).copy(alpha = 0.2f), FluxaTheme.shapes.pill)
                    .border(1.5.dp, if (cancelled) colors.textMuted else categoryColorFor(item.category?.details?.color), FluxaTheme.shapes.pill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(categoryIconFor(item.category?.details?.icon), contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(details.title, style = type.bodyStrong, color = if (cancelled) colors.textSecondary else colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(meta, style = type.caption, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(pluralStringResource(R.plurals.purchase_installments_count, details.installmentCount, details.installmentCount), style = type.caption, color = colors.textMuted)
                if (cancelled) StatePill(stringResource(R.string.purchase_status_cancelled), FluxaIcons.Close, modifier = Modifier.padding(top = 4.dp))
            }
            FluxaAmount(
                amount = details.total,
                color = if (cancelled) colors.textMuted else colors.textPrimary,
                style = type.amount.copy(textDecoration = if (cancelled) TextDecoration.LineThrough else null),
            )
        }
    }
}

/** Parcela persistida: número/total, valor, vencimento e estado (texto + ícone, não só cor). */
@Composable
internal fun InstallmentRow(installment: Installment, count: Int, title: String? = null, onClick: (() -> Unit)? = null) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    val status = when (installment.status) {
        InstallmentStatus.PENDING -> stringResource(R.string.installment_status_pending)
        InstallmentStatus.PAID -> stringResource(R.string.installment_status_paid)
        InstallmentStatus.CANCELLED -> stringResource(R.string.installment_status_cancelled)
    }
    val icon = when (installment.status) {
        InstallmentStatus.PENDING -> FluxaIcons.Calendar
        InstallmentStatus.PAID -> FluxaIcons.Check
        InstallmentStatus.CANCELLED -> FluxaIcons.Close
    }
    val cancelled = installment.status == InstallmentStatus.CANCELLED
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.installment_open), onClick = onClick) else Modifier)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) { stateDescription = status }
            .testTag("installment-${title ?: ""}-${installment.number}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.installment_number, installment.number, count),
            style = type.label,
            color = colors.onBrandTonal,
            modifier = Modifier.background(colors.brandTonal, FluxaTheme.shapes.pill).padding(horizontal = 10.dp, vertical = 4.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            if (title != null) Text(title, style = type.bodyStrong, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.installment_due, installment.dueDate.format(BrazilDate)), style = type.caption, color = colors.textSecondary)
            StatePill(status, icon, modifier = Modifier.padding(top = 4.dp))
        }
        FluxaAmount(
            amount = installment.amount,
            color = if (cancelled) colors.textMuted else colors.textPrimary,
            style = type.amount.copy(textDecoration = if (cancelled) TextDecoration.LineThrough else null),
        )
        if (onClick != null) Icon(FluxaIcons.ChevronRight, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
    }
}

// endregion

// region Formulário de cartão

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CardFormScreen(viewModel: CardFormViewModel, onDone: (CreditCompletion?) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmArchive by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.completion) { state.completion?.let(onDone) }
    FeatureScreen(
        title = stringResource(if (state.isEditing) R.string.card_edit else R.string.card_new),
        overline = stringResource(R.string.cards_title),
        onBack = { onDone(null) },
        modifier = Modifier.testTag("screen-card-form"),
        bottomAction = if (state.isLoading || state.notFound) null else {
            {
                FluxaButton(
                    text = stringResource(R.string.action_save),
                    onClick = viewModel::save,
                    size = FluxaButtonSize.Large,
                    loading = state.isSaving,
                    enabled = !state.isArchiving,
                    modifier = Modifier.fillMaxWidth().testTag("action-save"),
                )
            }
        },
    ) {
        when {
            state.isLoading -> item { LoadingBlock() }
            state.notFound -> item { ErrorBanner(stringResource(R.string.error_card_not_found), onDismiss = null) }
            else -> {
                state.operationError?.let { item { ErrorBanner(stringResource(it), onDismiss = viewModel::dismissError) } }
                if (state.isArchived) item { StatePill(stringResource(R.string.card_archived_note), FluxaIcons.Archive) }
                item {
                    FluxaInput(
                        value = state.name, onValueChange = viewModel::onNameChange, label = stringResource(R.string.label_name),
                        errorText = state.nameError?.let { stringResource(it) }, enabled = !state.isBusy,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        modifier = Modifier.testTag("field-card-name"),
                    )
                }
                item {
                    FluxaInput(
                        value = state.limitText, onValueChange = viewModel::onLimitChange, label = stringResource(R.string.card_field_limit),
                        placeholder = stringResource(R.string.accounts_field_amount_hint), supportingText = stringResource(R.string.card_field_limit_support),
                        errorText = state.limitError?.let { stringResource(it) }, enabled = !state.isBusy,
                        trailing = { Text("R$", style = FluxaTheme.typography.label, color = FluxaTheme.colors.textMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        modifier = Modifier.testTag("field-card-limit"),
                    )
                }
                item {
                    FluxaInput(
                        value = state.closingDayText, onValueChange = viewModel::onClosingDayChange, label = stringResource(R.string.card_field_closing),
                        placeholder = stringResource(R.string.card_field_day_hint), errorText = state.closingDayError?.let { stringResource(it) },
                        enabled = !state.isBusy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.testTag("field-card-closing"),
                    )
                }
                item {
                    FluxaInput(
                        value = state.dueDayText, onValueChange = viewModel::onDueDayChange, label = stringResource(R.string.card_field_due),
                        placeholder = stringResource(R.string.card_field_day_hint), errorText = state.dueDayError?.let { stringResource(it) },
                        supportingText = stringResource(R.string.card_days_help),
                        enabled = !state.isBusy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        modifier = Modifier.testTag("field-card-due"),
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FieldLabel(stringResource(R.string.card_field_color))
                        FlowRow(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ColorTile(null, state.color == null, stringResource(R.string.categories_none), !state.isBusy) { viewModel.onColorChange(null) }
                            CategoryColors.forEach { option ->
                                ColorTile(option.hex, state.color == option.hex, stringResource(option.label), !state.isBusy) { viewModel.onColorChange(option.hex) }
                            }
                        }
                    }
                }
                if (state.isEditing && !state.isArchived) {
                    item {
                        FluxaButton(
                            text = stringResource(R.string.action_archive), onClick = { confirmArchive = true },
                            variant = FluxaButtonVariant.Destructive, leadingIcon = FluxaIcons.Archive,
                            loading = state.isArchiving, enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth().testTag("action-archive"),
                        )
                    }
                }
            }
        }
    }
    if (confirmArchive) {
        ConfirmArchiveSheet(
            title = stringResource(R.string.card_archive_title),
            body = stringResource(R.string.card_archive_body),
            confirmLabel = stringResource(R.string.action_archive),
            onConfirm = { confirmArchive = false; viewModel.archive() },
            onDismiss = { confirmArchive = false },
        )
    }
}

@Composable
private fun ColorTile(hex: String?, selected: Boolean, description: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = FluxaTheme.colors
    val shape = FluxaTheme.shapes.medium
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(shape)
            .background(if (selected) colors.brandTonal else colors.surfaceSunken, shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.brand else colors.border, shape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description }
            .testTag("card-color-${hex ?: "none"}"),
        contentAlignment = Alignment.Center,
    ) {
        if (hex == null) Icon(FluxaIcons.Close, null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
        else Box(Modifier.size(24.dp).background(categoryColorFor(hex), FluxaTheme.shapes.pill))
        if (selected) {
            Box(Modifier.align(Alignment.TopEnd).padding(3.dp).size(16.dp).background(colors.brand, FluxaTheme.shapes.pill), contentAlignment = Alignment.Center) {
                Icon(FluxaIcons.Check, null, tint = colors.onBrand, modifier = Modifier.size(12.dp))
            }
        }
    }
}

// endregion

// region Formulário de compra

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PurchaseFormScreen(
    viewModel: PurchaseFormViewModel,
    onCreated: (PurchaseCreated) -> Unit,
    onBack: () -> Unit,
    onCreateCard: () -> Unit,
    onCreateCategory: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pickDate by rememberSaveable { mutableStateOf(false) }
    val colors = FluxaTheme.colors
    LaunchedEffect(state.created) { state.created?.let(onCreated) }
    FeatureScreen(
        title = stringResource(R.string.new_purchase),
        overline = stringResource(R.string.cards_title),
        onBack = onBack,
        modifier = Modifier.testTag("screen-purchase-form"),
        bottomAction = {
            state.firstError?.let { error ->
                Box(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive }.testTag("purchase-form-error")) {
                    FieldError(stringResource(error))
                }
            }
            FluxaButton(
                text = stringResource(R.string.action_save), onClick = viewModel::save, size = FluxaButtonSize.Large,
                loading = state.isSaving, modifier = Modifier.fillMaxWidth().testTag("action-save"),
            )
        },
    ) {
        state.operationError?.let { item { ErrorBanner(stringResource(it), onDismiss = viewModel::dismissError) } }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("group-card")) {
                FieldLabel(stringResource(R.string.purchase_field_card))
                if (state.cardsLoaded && state.cards.isEmpty()) {
                    Text(stringResource(R.string.purchase_no_cards), style = FluxaTheme.typography.body, color = colors.textSecondary)
                    FluxaButton(stringResource(R.string.card_new), onCreateCard, variant = FluxaButtonVariant.Secondary, leadingIcon = FluxaIcons.Plus, modifier = Modifier.testTag("action-create-card"))
                } else {
                    FlowRow(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.cards.forEach { summary ->
                            FluxaChip(
                                label = summary.card.details.name, selected = state.cardId == summary.card.id,
                                onSelectedChange = { viewModel.onCardChange(summary.card.id) }, leadingIcon = FluxaIcons.Card,
                                role = Role.RadioButton, enabled = !state.isSaving,
                                modifier = Modifier.testTag("card-option-${summary.card.details.name}"),
                            )
                        }
                    }
                    state.selectedCard?.let { selected ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) {}) {
                            Text(stringResource(R.string.purchase_card_available), style = FluxaTheme.typography.caption, color = colors.textSecondary)
                            FluxaAmount(amount = selected.availableLimit, style = FluxaTheme.typography.caption, color = colors.textSecondary)
                        }
                    }
                }
                state.cardError?.let { FieldError(stringResource(it)) }
            }
        }
        item {
            FluxaInput(
                value = state.amountText, onValueChange = viewModel::onAmountChange, label = stringResource(R.string.purchase_field_total),
                placeholder = stringResource(R.string.accounts_field_amount_hint), errorText = state.amountError?.let { stringResource(it) },
                enabled = !state.isSaving, trailing = { Text("R$", style = FluxaTheme.typography.label, color = colors.textMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier.testTag("field-purchase-amount"),
            )
        }
        item {
            FluxaInput(
                value = state.installmentsText, onValueChange = viewModel::onInstallmentsChange, label = stringResource(R.string.purchase_field_installments),
                errorText = state.installmentsError?.let { stringResource(it) },
                supportingText = state.installmentCount?.let { pluralStringResource(R.plurals.purchase_installments_count, it, it) + ". " + stringResource(R.string.purchase_schedule_after_save) },
                enabled = !state.isSaving, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.testTag("field-purchase-installments"),
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("group-purchase-category")) {
                FieldLabel(stringResource(R.string.purchase_field_category))
                if (state.categoriesLoaded && state.categories.isEmpty()) {
                    Text(stringResource(R.string.purchase_no_categories), style = FluxaTheme.typography.body, color = colors.textSecondary)
                    FluxaButton(stringResource(R.string.transaction_create_category), onCreateCategory, variant = FluxaButtonVariant.Secondary, leadingIcon = FluxaIcons.Plus, modifier = Modifier.testTag("action-create-category"))
                } else {
                    FlowRow(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.categories.forEach { category ->
                            FluxaChip(
                                label = category.details.name, selected = state.categoryId == category.id,
                                onSelectedChange = { viewModel.onCategoryChange(category.id) }, leadingIcon = categoryIconFor(category.details.icon),
                                role = Role.RadioButton, enabled = !state.isSaving,
                                modifier = Modifier.testTag("purchase-category-${category.details.name}"),
                            )
                        }
                    }
                }
                state.categoryError?.let { FieldError(stringResource(it)) }
            }
        }
        item {
            FluxaInput(
                value = state.title, onValueChange = viewModel::onTitleChange, label = stringResource(R.string.purchase_field_title),
                placeholder = stringResource(R.string.purchase_field_title_hint), errorText = state.titleError?.let { stringResource(it) },
                enabled = !state.isSaving, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.testTag("field-purchase-title"),
            )
        }
        item {
            FluxaInput(
                value = state.merchant, onValueChange = viewModel::onMerchantChange, label = stringResource(R.string.purchase_field_merchant),
                enabled = !state.isSaving, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                modifier = Modifier.testTag("field-purchase-merchant"),
            )
        }
        item {
            DateField(
                label = stringResource(R.string.purchase_field_date), supporting = null, date = state.purchaseDate,
                enabled = !state.isSaving, onClick = { pickDate = true }, errorText = state.dateError?.let { stringResource(it) },
                modifier = Modifier.testTag("field-purchase-date"),
            )
        }
        item {
            FluxaInput(
                value = state.notes, onValueChange = viewModel::onNotesChange, label = stringResource(R.string.purchase_field_notes),
                enabled = !state.isSaving, singleLine = false,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.testTag("field-purchase-notes"),
            )
        }
    }
    if (pickDate) {
        FluxaDatePicker(initial = state.purchaseDate, onPicked = { viewModel.onDateChange(it); pickDate = false }, onDismiss = { pickDate = false })
    }
}

// endregion

// region Detalhe da compra

@Composable
internal fun PurchaseDetailScreen(
    viewModel: PurchaseDetailViewModel,
    onBack: () -> Unit,
    onCancelled: () -> Unit,
    onOpenInstallment: (UUID) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmCancel by rememberSaveable { mutableStateOf(false) }
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    LaunchedEffect(state.cancelled) { if (state.cancelled) onCancelled() }
    FeatureScreen(
        title = state.purchase?.details?.title ?: stringResource(R.string.purchase_detail_title),
        overline = stringResource(R.string.purchase_detail_title),
        onBack = onBack,
        modifier = Modifier.testTag("screen-purchase-detail"),
    ) {
        val purchase = state.purchase
        when {
            state.isLoading -> item { LoadingBlock() }
            state.notFound || purchase == null -> item { ErrorBanner(stringResource(state.operationError ?: R.string.error_purchase_not_found), onDismiss = null) }
            else -> {
                val d = purchase.details
                state.operationError?.let { item { ErrorBanner(stringResource(it), onDismiss = viewModel::dismissError) } }
                if (state.isCancelled) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(colors.surfaceSunken, FluxaTheme.shapes.medium).padding(16.dp).testTag("purchase-cancelled-note"),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(FluxaIcons.Close, null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                            Text(stringResource(R.string.purchase_cancelled_note), style = type.label, color = colors.textSecondary)
                        }
                    }
                }
                item {
                    FluxaCard(modifier = Modifier.fillMaxWidth().testTag("purchase-summary")) {
                        Text(stringResource(R.string.purchase_field_total).uppercase(), style = type.overline, color = colors.textMuted)
                        FluxaAmount(
                            amount = d.total,
                            style = type.headline.merge(type.amount).copy(
                                fontSize = type.headline.fontSize,
                                textDecoration = if (state.isCancelled) TextDecoration.LineThrough else null,
                            ),
                            color = if (state.isCancelled) colors.textMuted else colors.textPrimary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(pluralStringResource(R.plurals.purchase_installments_count, d.installmentCount, d.installmentCount), style = type.label, color = colors.textSecondary)
                        Text(listOfNotNull(d.merchant, state.category?.details?.name).joinToString(" · "), style = type.body, color = colors.textSecondary)
                        Text(stringResource(R.string.purchase_meta_date, d.purchaseDate.format(BrazilDate)), style = type.caption, color = colors.textMuted)
                        state.card?.let { Text(it.card.details.name, style = type.caption, color = colors.textMuted) }
                        d.notes?.let { Text(it, style = type.body, color = colors.textSecondary, modifier = Modifier.padding(top = 8.dp)) }
                        StatePill(
                            stringResource(if (state.isCancelled) R.string.purchase_status_cancelled else R.string.purchase_status_active),
                            if (state.isCancelled) FluxaIcons.Close else FluxaIcons.Check,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                item { FluxaSectionHeader(title = stringResource(R.string.purchase_detail_installments)) }
                item {
                    FluxaCard(modifier = Modifier.fillMaxWidth().testTag("installment-list"), contentPadding = PaddingValues(vertical = 4.dp)) {
                        state.installments.forEachIndexed { index, installment ->
                            if (index > 0) Divider()
                            // Parcela cancelada fica só leitura; pendente/paga abre o detalhe (pagar ou desfazer).
                            InstallmentRow(
                                installment, state.installments.size,
                                onClick = if (installment.status == InstallmentStatus.CANCELLED) null else ({ onOpenInstallment(installment.id) }),
                            )
                        }
                    }
                }
                if (!state.isCancelled && !state.canCancel) {
                    item { StatePill(stringResource(R.string.purchase_paid_blocks_cancel), FluxaIcons.Alert) }
                }
                if (state.canCancel) {
                    item {
                        FluxaButton(
                            text = stringResource(R.string.purchase_cancel_action), onClick = { confirmCancel = true },
                            variant = FluxaButtonVariant.Destructive, leadingIcon = FluxaIcons.Close, loading = state.isCancelling,
                            modifier = Modifier.fillMaxWidth().testTag("action-cancel-purchase"),
                        )
                    }
                }
            }
        }
    }
    if (confirmCancel) {
        ConfirmArchiveSheet(
            title = stringResource(R.string.purchase_cancel_title),
            body = stringResource(R.string.purchase_cancel_body),
            confirmLabel = stringResource(R.string.purchase_cancel_action),
            dismissLabel = stringResource(R.string.purchase_cancel_keep),
            confirmIcon = FluxaIcons.Close,
            onConfirm = { confirmCancel = false; viewModel.cancelPurchase() },
            onDismiss = { confirmCancel = false },
        )
    }
}

// endregion
