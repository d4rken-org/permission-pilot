package eu.darken.myperm.common.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import testhelper.BaseTest
import java.io.File

class DataStoreValueTest : BaseTest() {

    @TempDir
    lateinit var tempDir: File

    private lateinit var testScope: TestScope

    @BeforeEach
    fun setup() {
        testScope = TestScope(UnconfinedTestDispatcher() + Job())
    }

    private fun createDataStore() = PreferenceDataStoreFactory.create(
        scope = testScope,
        produceFile = { File(tempDir, "test.preferences_pb") }
    )

    @Test
    fun `read default boolean`() = runTest {
        val ds = createDataStore()
        val pref = ds.createValue("test.bool", true)
        pref.value() shouldBe true
    }

    @Test
    fun `write and read boolean`() = runTest {
        val ds = createDataStore()
        val pref = ds.createValue("test.bool", false)

        pref.value(true)
        pref.value() shouldBe true

        pref.value(false)
        pref.value() shouldBe false
    }

    @Test
    fun `read default int`() = runTest {
        val ds = createDataStore()
        val pref = ds.createValue("test.int", 42)
        pref.value() shouldBe 42
    }

    @Test
    fun `write and read int`() = runTest {
        val ds = createDataStore()
        val pref = ds.createValue("test.int", 0)

        pref.value(99)
        pref.value() shouldBe 99
    }

    @Test
    fun `read default long`() = runTest {
        val ds = createDataStore()
        val pref = ds.createValue("test.long", 9000L)
        pref.value() shouldBe 9000L
    }

    @Test
    fun `write and read long`() = runTest {
        val ds = createDataStore()
        val pref = ds.createValue("test.long", 0L)

        pref.value(9001L)
        pref.value() shouldBe 9001L
    }

    @Test
    fun `read default float`() = runTest {
        val ds = createDataStore()
        val pref = ds.createValue("test.float", 3.6f)
        pref.value() shouldBe 3.6f
    }

    @Test
    fun `write and read float`() = runTest {
        val ds = createDataStore()
        val pref = ds.createValue("test.float", 0f)

        pref.value(15000f)
        pref.value() shouldBe 15000f
    }

    @Test
    fun `read default string`() = runTest {
        val ds = createDataStore()
        val pref = ds.createValue("test.string", "default")
        pref.value() shouldBe "default"
    }

    @Test
    fun `write and read string`() = runTest {
        val ds = createDataStore()
        val pref = ds.createValue("test.string", "default")

        pref.value("updated")
        pref.value() shouldBe "updated"
    }

    @Test
    fun `flow emits default then updates`() = runTest {
        val ds = createDataStore()
        val pref = ds.createValue("test.int", 0)

        val emissions = mutableListOf<Int>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            pref.flow.take(3).toList(emissions)
        }

        pref.value(1)
        pref.value(2)

        job.join()
        emissions shouldBe listOf(0, 1, 2)
    }

    @Test
    fun `flow does not emit duplicate values`() = runTest {
        val ds = createDataStore()
        val pref = ds.createValue("test.int", 0)

        pref.value(5)

        val emissions = mutableListOf<Int>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            pref.flow.take(2).toList(emissions)
        }

        pref.value(5) // same value, should not emit
        pref.value(10) // different value, should emit

        job.join()
        emissions shouldBe listOf(5, 10)
    }

    @Test
    fun `update performs atomic read-modify-write`() = runTest {
        val ds = createDataStore()
        val pref = ds.createValue("test.int", 0)

        pref.update { it + 1 }
        pref.value() shouldBe 1

        pref.update { it + 10 }
        pref.value() shouldBe 11
    }

    @Test
    fun `valueBlocking getter reads value`() = runTest {
        val ds = createDataStore()
        val pref = ds.createValue("test.int", 42)

        pref.valueBlocking shouldBe 42

        pref.value(99)
        pref.valueBlocking shouldBe 99
    }

    @Test
    fun `valueBlocking setter writes value`() = runTest {
        val ds = createDataStore()
        val pref = ds.createValue("test.int", 0)

        pref.valueBlocking = 77
        pref.value() shouldBe 77
        pref.flow.first() shouldBe 77
    }

    @Test
    fun `multiple values share same DataStore`() = runTest {
        val ds = createDataStore()
        val boolPref = ds.createValue("test.bool", false)
        val intPref = ds.createValue("test.int", 0)
        val stringPref = ds.createValue("test.string", "default")

        boolPref.value(true)
        intPref.value(42)
        stringPref.value("hello")

        boolPref.value() shouldBe true
        intPref.value() shouldBe 42
        stringPref.value() shouldBe "hello"
    }
}
