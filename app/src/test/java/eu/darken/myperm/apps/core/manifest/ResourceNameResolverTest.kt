package eu.darken.myperm.apps.core.manifest

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import eu.darken.myperm.apps.core.Pkg
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testhelper.BaseTest

class ResourceNameResolverTest : BaseTest() {

    private val ownerPkg = Pkg.Name("com.example.app")

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var appResources: Resources
    private lateinit var frameworkResources: Resources

    private lateinit var resolver: ResourceNameResolver

    @BeforeEach
    fun setup() {
        context = mockk()
        packageManager = mockk()
        appResources = mockk()
        frameworkResources = mockk()

        every { context.packageManager } returns packageManager
        every { context.resources } returns frameworkResources
        every { packageManager.getResourcesForApplication(ownerPkg.value) } returns appResources

        resolver = ResourceNameResolver(context)
    }

    private fun stubAppRes(id: Int, type: String, entry: String, pkg: String = ownerPkg.value) {
        every { appResources.getResourceTypeName(id) } returns type
        every { appResources.getResourceEntryName(id) } returns entry
        every { appResources.getResourcePackageName(id) } returns pkg
    }

    private fun stubFrameworkRes(id: Int, type: String, entry: String) {
        every { frameworkResources.getResourceTypeName(id) } returns type
        every { frameworkResources.getResourceEntryName(id) } returns entry
        every { frameworkResources.getResourcePackageName(id) } returns "android"
    }

    @Test
    fun `owner-package reference renders without package prefix`() {
        val id = 0x7F020001
        stubAppRes(id, type = "drawable", entry = "ic_launcher")

        resolver.forPackage(ownerPkg).resolve(id) shouldBe "@drawable/ic_launcher"
    }

    @Test
    fun `framework reference routes through context resources and renders with android prefix`() {
        // Framework package id is 0x01 (top byte of the resource id).
        val id = 0x01010009
        stubFrameworkRes(id, type = "attr", entry = "protectionLevel")

        resolver.forPackage(ownerPkg).resolve(id) shouldBe "@android:attr/protectionLevel"

        // Must not have consulted app resources for a framework id.
        verify(exactly = 0) { appResources.getResourceTypeName(any()) }
    }

    @Test
    fun `other-package reference renders with explicit package prefix`() {
        val id = 0x7F040001
        stubAppRes(id, type = "string", entry = "external_label", pkg = "com.other.pkg")

        resolver.forPackage(ownerPkg).resolve(id) shouldBe "@com.other.pkg:string/external_label"
    }

    @Test
    fun `NameNotFoundException leaves app lookups returning null but framework still works`() {
        every { packageManager.getResourcesForApplication(ownerPkg.value) } throws
            PackageManager.NameNotFoundException("not installed")

        val scope = resolver.forPackage(ownerPkg)

        // App id (top byte 0x7F) has no app Resources available -> null without calling anything.
        scope.resolve(0x7F020001).shouldBeNull()

        // Framework id (top byte 0x01) should still resolve via context.resources.
        stubFrameworkRes(0x01010009, type = "attr", entry = "protectionLevel")
        scope.resolve(0x01010009) shouldBe "@android:attr/protectionLevel"
    }

    @Test
    fun `Resources NotFoundException surfaces as null`() {
        val id = 0x7F020999
        every { appResources.getResourceTypeName(id) } throws Resources.NotFoundException("missing")

        resolver.forPackage(ownerPkg).resolve(id).shouldBeNull()
    }

    @Test
    fun `resourceId zero is guarded without calling Resources`() {
        resolver.forPackage(ownerPkg).resolve(0).shouldBeNull()

        verify(exactly = 0) { appResources.getResourceTypeName(any()) }
        verify(exactly = 0) { frameworkResources.getResourceTypeName(any()) }
    }

    @Test
    fun `resourceId -1 is guarded without calling Resources`() {
        // 0xFFFFFFFF encoded as signed Int.
        resolver.forPackage(ownerPkg).resolve(-1).shouldBeNull()

        verify(exactly = 0) { appResources.getResourceTypeName(any()) }
        verify(exactly = 0) { frameworkResources.getResourceTypeName(any()) }
    }

    @Test
    fun `successful resolutions are cached after the first call`() {
        val id = 0x7F020001
        stubAppRes(id, type = "drawable", entry = "ic_launcher")

        val scope = resolver.forPackage(ownerPkg)
        scope.resolve(id)
        scope.resolve(id)
        scope.resolve(id)

        verify(exactly = 1) { appResources.getResourceTypeName(id) }
    }

    @Test
    fun `null results are cached via containsKey to avoid repeated lookups`() {
        val id = 0x7F020999
        every { appResources.getResourceTypeName(id) } throws Resources.NotFoundException("missing")

        val scope = resolver.forPackage(ownerPkg)
        scope.resolve(id).shouldBeNull()
        scope.resolve(id).shouldBeNull()
        scope.resolve(id).shouldBeNull()

        // Without containsKey-based caching, the resolver would retry each time.
        verify(exactly = 1) { appResources.getResourceTypeName(id) }
    }

    @Test
    fun `framework id with pkgId 0x01 renders as android prefix even when pkg name differs`() {
        // Defensive: if getResourcePackageName returns something unexpected for a framework id,
        // the pkgId shortcut should still produce @android:.
        val id = 0x01010009
        every { frameworkResources.getResourceTypeName(id) } returns "attr"
        every { frameworkResources.getResourceEntryName(id) } returns "protectionLevel"
        every { frameworkResources.getResourcePackageName(id) } returns "some.oem.pkg"

        resolver.forPackage(ownerPkg).resolve(id) shouldBe "@android:attr/protectionLevel"
    }
}
