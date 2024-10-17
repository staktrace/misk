package misk.jdbc

import misk.resources.ResourceLoader
import java.util.regex.Pattern
import kotlin.reflect.KClass

internal class DeclarativeSchemaMigrator(
  private val qualifier: KClass<out Annotation>,
  private val resourceLoader: ResourceLoader,
  private val dataSourceConfig: DataSourceConfig,
  private val dataSourceService: DataSourceService,
  private val connector: DataSourceConnector,
) : BaseSchemaMigrator(resourceLoader, dataSourceService, connector) {

  override fun validateMigrationFile(migrationFile: MigrationFile): Boolean {
    return !Pattern.compile(connector.config().migrations_resources_regex).matcher(migrationFile.filename).matches()
  }
  override fun applyAll(author: String): MigrationStatus {
    throw UnsupportedOperationException("not implemented")
  }

  override fun requireAll(): MigrationStatus {
    throw UnsupportedOperationException("not implemented")
  }
}