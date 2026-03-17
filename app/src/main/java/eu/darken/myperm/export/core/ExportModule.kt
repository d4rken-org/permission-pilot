package eu.darken.myperm.export.core

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import eu.darken.myperm.export.core.formatter.CsvFormatter
import eu.darken.myperm.export.core.formatter.ExportFormatter
import eu.darken.myperm.export.core.formatter.JsonFormatter
import eu.darken.myperm.export.core.formatter.MarkdownFormatter

@Module
@InstallIn(SingletonComponent::class)
object ExportModule {

    @Provides
    fun provideFormatters(
        markdown: MarkdownFormatter,
        csv: CsvFormatter,
        json: JsonFormatter,
    ): Map<ExportFormat, ExportFormatter> = mapOf(
        ExportFormat.MARKDOWN to markdown,
        ExportFormat.CSV to csv,
        ExportFormat.JSON to json,
    )
}
