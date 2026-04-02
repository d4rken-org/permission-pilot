package eu.darken.myperm.permissions.core.known

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test
import testhelper.BaseTest
import kotlin.reflect.full.isSubclassOf

/**
 * Ensures the manual `values` lists match the actual sealed subclasses.
 * Uses kotlin-reflect (fine in JVM tests) to discover the ground truth.
 */
class SealedClassValuesTest : BaseTest() {

    private inline fun <reified T : Any> reflectSealedObjects(): List<T> =
        T::class.nestedClasses
            .filter { it.isSubclassOf(T::class) }
            .mapNotNull { it.objectInstance }
            .filterIsInstance<T>()

    @Test
    fun `APerm values list matches sealed subclasses`() {
        val reflected = reflectSealedObjects<APerm>()
        APerm.values shouldHaveSize reflected.size
        APerm.values.map { it.id } shouldContainExactlyInAnyOrder reflected.map { it.id }
    }

    @Test
    fun `AExtraPerm values list matches sealed subclasses`() {
        val reflected = reflectSealedObjects<AExtraPerm>()
        AExtraPerm.values shouldHaveSize reflected.size
        AExtraPerm.values.map { it.id } shouldContainExactlyInAnyOrder reflected.map { it.id }
    }

    @Test
    fun `APermGrp values list matches sealed subclasses`() {
        val reflected = reflectSealedObjects<APermGrp>()
        APermGrp.values shouldHaveSize reflected.size
        APermGrp.values.map { it.id } shouldContainExactlyInAnyOrder reflected.map { it.id }
    }

    // AKnownPkg is not tested here: its Pkg.Id constructor calls Process.myUserHandle()
    // which requires Android framework mocking. Only 8 items — low drift risk.
}
