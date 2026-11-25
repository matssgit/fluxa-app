package com.fluxa.personal.rc

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.FluxaApplication
import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.*
import com.fluxa.personal.security.*
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Test APK only. Run seed against the exact v1.0.1 release (code 4), then verify against v1.0.2 (code 5) after adb install -r.
 * Never run a Gradle connected task between phases: it can replace/uninstall the target.
 */
internal object V102Evidence {
    val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()
    fun file(name: String) = File(app.filesDir, "v102-qa-$name.txt")
    fun phase(expected: String) { assumeTrue("Explicit v1.0.2 upgrade protocol only", InstrumentationRegistry.getArguments().getString("v102Phase") == expected) }
    fun version() = app.packageManager.getPackageInfo(app.packageName, 0).longVersionCode
    fun database(): String {
        val db = app.database.openHelper.writableDatabase
        val tables = mutableListOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_%' AND name NOT LIKE 'sqlite_%' ORDER BY name").use {
            while (it.moveToNext()) tables += it.getString(0)
        }
        val canonical = buildString {
            for (table in tables) {
                val rows = mutableListOf<String>()
                db.query("SELECT * FROM \"$table\"").use { cursor ->
                    while (cursor.moveToNext()) rows += (0 until cursor.columnCount).joinToString("|") { i ->
                        if (cursor.isNull(i)) "NULL" else cursor.getString(i).let { "${it.length}:$it" }
                    }
                }
                append(table).append(':').append(rows.size).append('\n')
                rows.sorted().forEach { append(it).append('\n') }
            }
        }
        return "v${db.version}\n" + MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray()).joinToString("") { "%02x".format(it) }
    }
    suspend fun settings() = app.preferences.read().toString() + "\n" + app.preferences.securitySettings.first().toString()
    suspend fun safe() = app.safeToSpend.observeSafeToSpend(30).first().toString()
    suspend fun save() {
        file("database").writeText(database())
        file("settings").writeText(settings())
        file("safe").writeText(safe())
        file("analytics").writeText((app.analytics.observeFinancialAnalytics().first() as FinancialAnalyticsState.Ready).snapshot.toString())
    }
    suspend fun assertPreserved(settings: Boolean = true) {
        assertEquals(file("database").readText(), database())
        if (settings) assertEquals(file("settings").readText(), settings())
        assertEquals(file("safe").readText(), safe())
        assertEquals(file("analytics").readText(), (app.analytics.observeFinancialAnalytics().first() as FinancialAnalyticsState.Ready).snapshot.toString())
    }
}

@RunWith(AndroidJUnit4::class)
class V102UpgradeSeedTest {
    @Test fun seedExactV101() = runBlocking {
        V102Evidence.phase("seed")
        val app = V102Evidence.app
        assertEquals(4L, V102Evidence.version())
        assertEquals(7, app.database.openHelper.writableDatabase.version)
        // Exclusive fresh AVD only; refuse to replace an existing dataset.
        app.database.openHelper.writableDatabase.query("SELECT COUNT(*) FROM accounts").use { it.moveToFirst(); assertEquals(0, it.getInt(0)) }
        val today = FinancialCalendar().today()
        V102Evidence.file("date").writeText(today.toString())
        val account = app.accounts.create(AccountDetails("FINAL Principal", AccountType.CHECKING, Money(500_000), LocalDate.of(2020,1,1)))
        app.accounts.create(AccountDetails("FINAL Carteira", AccountType.WALLET, Money(10_000), LocalDate.of(2020,1,1)))
        val food = app.categories.create(CategoryDetails("FINAL Mercado", TransactionDirection.EXPENSE, "cart", "#B9684F"))
        val salary = app.categories.create(CategoryDetails("FINAL Salario", TransactionDirection.INCOME, "work", "#35695A"))
        suspend fun tx(direction: TransactionDirection, cents: Long, day: LocalDate) {
            assertTrue(app.transactions.create(UUID.randomUUID(), TransactionDraft(direction, account, if(direction == TransactionDirection.INCOME) salary else food, Money(cents), day, "FINAL QA")) is TransactionResult.Saved)
        }
        tx(TransactionDirection.INCOME,620_000,today)
        tx(TransactionDirection.EXPENSE,30_000,today)
        tx(TransactionDirection.INCOME,200_000,today.withDayOfMonth(1).minusMonths(1))
        tx(TransactionDirection.EXPENSE,20_000,today.withDayOfMonth(1).minusMonths(1))
        val card = (app.credit.createCard(CreditCardDetails("FINAL Cartao",Money(500_000),BillingDay(10),BillingDay(17))) as CreditResult.Saved).value.id
        val purchase = UUID.randomUUID()
        assertTrue(app.credit.createPurchase(purchase,PurchaseDraft(card,food,"FINAL Compra",Money(60_000),2,today)) is CreditResult.Saved)
        val installment = app.credit.observeInstallments(purchase).first().first()
        assertTrue(app.credit.payInstallment(installment.id,account,today) is InstallmentPaymentResult.Paid)
        val sub = (app.subscriptions.create(SubscriptionDetails("FINAL Musica",Money(5_000),food,SubscriptionSource.Account(account)),SubscriptionFrequency.MONTHLY,today,BillingDay(today.dayOfMonth)) as SubscriptionResult.Saved).value.id
        assertTrue(app.subscriptions.paySubscriptionOccurrence(sub,today,today) is SubscriptionResult.Saved)
        assertTrue(app.subscriptions.create(SubscriptionDetails("FINAL Anual",Money(12_000),food,SubscriptionSource.Card(card)),SubscriptionFrequency.YEARLY,today,BillingDay(today.dayOfMonth)) is SubscriptionResult.Saved)
        val goal = (app.goals.createGoal(GoalDetails("FINAL Viagem",Money(300_000),account)) as GoalResult.Saved).value.id
        assertTrue(app.goals.allocateToGoal(goal,Money(150_000)) is GoalResult.Saved)
        app.preferences.apply(app.preferences.read().copy(theme="DARK",hideAmounts=true))
        app.preferences.writeSecuritySettings(SecuritySettings(true,AutoLockTimeout.IMMEDIATELY,true))
        assertTrue(app.safeToSpend.observeSafeToSpend(30).first() is SafeToSpendState.Ready)
        V102Evidence.save()
        println("V102_UPGRADE_BASELINE " + V102Evidence.database() + " " + V102Evidence.settings())
    }
}

@RunWith(AndroidJUnit4::class)
class V102UpgradeVerifyTest {
    @Test fun verifyExactV102AndOldDataAnalytics() = runBlocking {
        V102Evidence.phase("upgrade")
        assertEquals(5L,V102Evidence.version())
        V102Evidence.assertPreserved()
        verifyAnalytics()
        println("V102_UPGRADE_PRESERVED " + V102Evidence.database())
    }
    /** After switching to [v102Theme] in the release UI and force-stopping: only the theme may differ from the v1.0.1 baseline. */
    @Test fun verifyThemeChosenInTheUi() = runBlocking {
        V102Evidence.phase("theme")
        assertEquals(5L,V102Evidence.version())
        V102Evidence.assertPreserved(settings=false)
        val theme = requireNotNull(InstrumentationRegistry.getArguments().getString("v102Theme"))
        assertEquals(V102Evidence.file("settings").readText().replace("theme=DARK","theme=$theme"),V102Evidence.settings())
    }
    private suspend fun verifyAnalytics() {
        val app = V102Evidence.app
        val s = (app.analytics.observeFinancialAnalytics().first() as FinancialAnalyticsState.Ready).snapshot
        assertEquals(LocalDate.parse(V102Evidence.file("date").readText()),s.asOfDate)
        assertEquals(Money(620_000),s.monthSummary.incomeRealized)
        assertEquals(Money(65_000),s.monthSummary.expenseRealized)
        assertEquals(Money(555_000),s.monthSummary.netResult)
        assertEquals(4L,s.monthSummary.transactionCount)
        assertEquals(Money(65_000),s.categorySpending.single().amount)
        assertEquals(100,s.categorySpending.single().percentage)
        assertEquals(6,s.sixMonthEvolution.size)
        assertEquals(Money(200_000),s.sixMonthEvolution[s.sixMonthEvolution.lastIndex-1].income)
        assertEquals(Money(20_000),s.sixMonthEvolution[s.sixMonthEvolution.lastIndex-1].expense)
        assertEquals(Money(1_095_000),s.projection.availableNow)
        val safe = (app.safeToSpend.observeSafeToSpend(30).first() as SafeToSpendState.Ready).result
        assertEquals(safe.availableNow,s.projection.availableNow)
        assertEquals(s.projection.availableNow-s.projection.obligationsTotal,s.projection.projectedEndingBalance)
        assertEquals(2L,s.subscriptionAnalytics.activeCount)
        assertEquals(Money(6_000),s.subscriptionAnalytics.monthlyEquivalentCost)
        assertEquals(Money(72_000),s.subscriptionAnalytics.annualProjectedCost)
        assertTrue(s.insights.any { it.type == FinancialInsightType.TOP_CATEGORY })
        assertTrue(s.insights.any { it.type == FinancialInsightType.POSITIVE_SAVINGS })
    }
}
