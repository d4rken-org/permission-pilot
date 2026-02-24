package eu.darken.myperm.apps.core.queries

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
abstract class QueriesModule {

    @Binds
    abstract fun manifestParser(impl: ApkParserManifestParser): ManifestParser
}
