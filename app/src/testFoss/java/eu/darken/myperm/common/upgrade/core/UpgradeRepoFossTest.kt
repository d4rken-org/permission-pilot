package eu.darken.myperm.common.upgrade.core

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import eu.darken.myperm.common.serialization.SerializationModule
import eu.darken.myperm.common.upgrade.UpgradeRepo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import java.io.IOException
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

class UpgradeRepoFossTest : BaseTest() {

    @BeforeEach
    fun setup() {

    }

    @AfterEach
    fun teardown() {

    }

    private val json = SerializationModule().json()

    private val record = FossUpgrade(
        upgradedAt = Instant.EPOCH,
        reason = FossUpgrade.Reason.GITHUB_SPONSORS,
    )

    // A stored record as the store itself hands it out: the serialized form under the production
    // key, so the read traverses the real reader too.
    private fun storedRecord(record: FossUpgrade): Preferences = mutablePreferencesOf(
        stringPreferencesKey("foss.upgrade") to json.encodeToString(FossUpgrade.serializer(), record)
    )

    /**
     * A REAL [FossCache] (and therefore a real `DataStoreValue`) over a store whose read flow the
     * test drives. The read path has to traverse the actual `strictFlow` — a faked cache value would
     * not prove that an IOException reaches the repo instead of being swallowed into "no record".
     */
    private fun createCache(storeData: Flow<Preferences>): FossCache {
        var stored: Preferences = emptyPreferences()
        val dataStore = mockk<DataStore<Preferences>>().apply {
            every { data } returns storeData
            // Writes stay in memory: only the read side is under test, but `update` has to run the
            // real transaction so persistUpgrade's create-only-if-absent still decides truthfully.
            coEvery { updateData(any()) } coAnswers {
                val transform = firstArg<suspend (Preferences) -> Preferences>()
                transform(stored).also { stored = it }
            }
        }
        return FossCache(dataStore, json)
    }

    // Real dispatchers, not a test scheduler: the shareIn sharing coroutine and the collectors have
    // to actually interleave here, and the whole point is that a failure settles instead of hanging.
    private fun createRepo(appScope: CoroutineScope, cache: FossCache) = UpgradeRepoFoss(
        appScope = appScope,
        fossCache = cache,
        webpageTool = mockk(),
    )

    @Test fun `test upgrade info pro status mapping`() {
        UpgradeRepoFoss.Info(
            isPro = false,
            upgradedAt = null,
        ).apply {
            type shouldBe UpgradeRepo.Type.FOSS
            isPro shouldBe false
        }

        UpgradeRepoFoss.Info(
            isPro = true,
            upgradedAt = Instant.EPOCH,
        ).isPro shouldBe true
    }

    @Test fun `a failing cache read surfaces as a settled error Info instead of hanging`(): Unit = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val repo = createRepo(scope, createCache(flow { throw IOException("cache broken") }))

            withTimeout(10_000) {
                repo.upgradeInfo.first().apply {
                    // Type and message: a bare non-null check would also pass on a swallow-and-wrap.
                    error.shouldBeInstanceOf<IOException>().message shouldBe "cache broken"
                    isPro shouldBe false
                    // The UI must be able to render this: an unsettled error is an endless spinner.
                    isSettled shouldBe true
                }
            }
        } finally {
            scope.cancel()
        }
    }

    @Test fun `a late cache failure keeps the last known entitlement`(): Unit = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val repo = createRepo(scope, createCache(flow {
                emit(storedRecord(record))
                throw IOException("cache broken later")
            }))

            withTimeout(10_000) {
                val infos = repo.upgradeInfo.take(2).toList()

                infos[0].apply {
                    isPro shouldBe true
                    error shouldBe null
                }
                // The entitlement we already saw must survive the read failure - a revoked Pro
                // status would kick a paying supporter back to the pitch.
                infos[1].apply {
                    isPro shouldBe true
                    error.shouldBeInstanceOf<IOException>()
                }
            }
        } finally {
            scope.cancel()
        }
    }

    @Test fun `a successful persist revives an error-stuck upgradeInfo`(): Unit = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            // First subscription fails, later ones read fine: the store recovered, but the shared
            // flow is still replaying the error Info to everyone.
            val subscriptions = AtomicInteger(0)
            val cache = createCache(flow {
                if (subscriptions.getAndIncrement() == 0) throw IOException("cache broken")
                emit(storedRecord(record))
            })
            val repo = createRepo(scope, cache)

            val received = Channel<UpgradeRepo.Info>(Channel.UNLIMITED)
            scope.launch { repo.upgradeInfo.collect { received.send(it) } }

            withTimeout(10_000) {
                received.receive().error.shouldBeInstanceOf<IOException>()

                // No explicit refresh() from the test: persist has to do the reviving itself,
                // otherwise the user's unlock never reaches the screen they are looking at.
                repo.persistUpgrade() shouldBe true

                received.receive().apply {
                    isPro shouldBe true
                    error shouldBe null
                }
            }
        } finally {
            scope.cancel()
        }
    }
}
