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

/** Test APK only. Run seed against the exact v1.0.0 release (code 3), then verify against v1.0.1 (code 4) after adb install -r.
 * Never run a Gradle connected task between phases: it can replace/uninstall the target.
 */
internal object V101Evidence {
    val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()
    fun file(name: String) = File(app.filesDir, "v101-qa-$name.txt")
    fun phase(expected: String) { assumeTrue("Explicit v1.0.1 upgrade protocol only", InstrumentationRegistry.getArguments().getString("v101Phase") == expected) }
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
class V101UpgradeSeedTest {
    @Test fun seedExactV100() = runBlocking {
        V101Evidence.phase("seed")
        val app = V101Evidence.app
        assertEquals(3L, V101Evidence.version())
        assertEquals(7, app.database.openHelper.writableDatabase.version)
        // Exclusive fresh AVD only; refuse to replace an existing dataset.
        app.database.openHelper.writableDatabase.query("SELECT COUNT(*) FROM accounts").use { it.moveToFirst(); assertEquals(0, it.getInt(0)) }
        val today = FinancialCalendar().today()
        V101Evidence.file("date").writeText(today.toString())
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
        V101Evidence.save()
        println("V101_UPGRADE_BASELINE " + V101Evidence.database() + " " + V101Evidence.settings())
    }
}

@RunWith(AndroidJUnit4::class)
class V101UpgradeVerifyTest {
    @Test fun verifyExactV101AndOldDataAnalytics() = runBlocking {
        V101Evidence.phase("upgrade")
        assertEquals(4L,V101Evidence.version())
        V101Evidence.assertPreserved()
        verifyAnalytics()
        println("V101_UPGRADE_PRESERVED " + V101Evidence.database())
    }
    /** After switching to [v101Theme] in the release UI and force-stopping: only the theme may differ from the v1.0.0 baseline. */
    @Test fun verifyThemeChosenInTheUi() = runBlocking {
        V101Evidence.phase("theme")
        assertEquals(4L,V101Evidence.version())
        V101Evidence.assertPreserved(settings=false)
        val theme = requireNotNull(InstrumentationRegistry.getArguments().getString("v101Theme"))
        assertEquals(V101Evidence.file("settings").readText().replace("theme=DARK","theme=$theme"),V101Evidence.settings())
    }
    private suspend fun verifyAnalytics() {
        val app = V101Evidence.app
        val s = (app.analytics.observeFinancialAnalytics().first() as FinancialAnalyticsState.Ready).snapshot
        assertEquals(LocalDate.parse(V101Evidence.file("date").readText()),s.asOfDate)
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
