package exh.metadata.metadata

import android.content.Context
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.SManga
import exh.metadata.metadata.base.RaisedSearchMetadata
import kotlinx.serialization.Serializable
import tachiyomi.source.model.MangaInfo

@Serializable
class HBrowseSearchMetadata : RaisedSearchMetadata() {
    var hbId: Long? = null

    var hbUrl: String? = null

    var thumbnail: String? = null

    var title: String? by titleDelegate(TITLE_TYPE_MAIN)

    // Length in pages
    var length: Int? = null

    override fun createMangaInfo(manga: MangaInfo): MangaInfo {
        val key = hbUrl

        val title = title

        // Guess thumbnail URL if manga does not have thumbnail URL
        val cover = if (manga.cover.isBlank()) {
            guessThumbnailUrl(hbId.toString())
        } else null

        val artist = tags.ofNamespace(ARTIST_NAMESPACE).joinToString { it.name }

        val genres = tagsToGenreList()

        val description = "meta"

        return manga.copy(
            key = key ?: manga.key,
            title = title ?: manga.title,
            cover = cover ?: manga.cover,
            artist = artist,
            genres = genres,
            description = description
        )
    }

    override fun copyTo(manga: SManga) {
        hbUrl?.let {
            manga.url = it
        }

        title?.let {
            manga.title = it
        }

        // Guess thumbnail URL if manga does not have thumbnail URL
        if (manga.thumbnail_url.isNullOrBlank()) {
            manga.thumbnail_url = guessThumbnailUrl(hbId.toString())
        }

        manga.artist = tags.ofNamespace(ARTIST_NAMESPACE).joinToString { it.name }

        manga.genre = tagsToGenreString()

        manga.description = "meta"
    }

    override fun getExtraInfoPairs(context: Context): List<Pair<String, String>> {
        val pairs = mutableListOf<Pair<String, String>>()
        hbId?.let { pairs += context.getString(R.string.id) to it.toString() }
        hbUrl?.let { pairs += context.getString(R.string.url) to it }
        thumbnail?.let { pairs += context.getString(R.string.thumbnail_url) to it }
        title?.let { pairs += context.getString(R.string.title) to it }
        length?.let { pairs += context.getString(R.string.page_count) to it.toString() }
        return pairs
    }

    companion object {
        const val BASE_URL = "https://www.hbrowse.com"

        private const val TITLE_TYPE_MAIN = 0

        const val TAG_TYPE_DEFAULT = 0
        const val ARTIST_NAMESPACE = "artist"

        fun guessThumbnailUrl(hbid: String): String {
            return "$BASE_URL/thumbnails/${hbid}_1.jpg#guessed"
        }
    }
}
