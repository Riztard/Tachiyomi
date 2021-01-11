package eu.kanade.tachiyomi.ui.browse.migration.advanced.process

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import exh.util.DeferredField
import exh.util.executeOnIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlin.coroutines.CoroutineContext

class MigratingManga(
    private val db: DatabaseHelper,
    private val sourceManager: SourceManager,
    val mangaId: Long,
    parentContext: CoroutineContext
) {
    val searchResult = DeferredField<Long?>()

    // <MAX, PROGRESS>
    val progress = ConflatedBroadcastChannel(1 to 0)

    val migrationJob = parentContext + SupervisorJob() + Dispatchers.Default

    var migrationStatus: Int = MigrationStatus.RUNNING

    @Volatile
    private var manga: Manga? = null
    suspend fun manga(): Manga? {
        if (manga == null) manga = db.getManga(mangaId).executeOnIO()
        return manga
    }

    suspend fun mangaSource(): Source {
        return sourceManager.getOrStub(manga()?.source ?: -1)
    }

    fun toModal(): MigrationProcessItem {
        // Create the model object.
        return MigrationProcessItem(this)
    }
}

class MigrationStatus {
    companion object {
        const val RUNNING = 0
        const val MANGA_FOUND = 1
        const val MANGA_NOT_FOUND = 2
    }
}
