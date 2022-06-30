package eu.kanade.tachiyomi.util

import android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.DEVICE_BATTERY_NOT_LOW
import eu.kanade.tachiyomi.data.preference.DEVICE_CHARGING
import eu.kanade.tachiyomi.data.preference.DEVICE_ONLY_ON_WIFI
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.manga.chapter.ChapterItem
import eu.kanade.tachiyomi.util.chapter.getChapterSort
import eu.kanade.tachiyomi.util.system.batteryManager
import eu.kanade.tachiyomi.util.system.isConnectedToWifi
import java.util.Date

fun Manga.isLocal() = source == LocalSource.ID

/**
 * Call before updating [Manga.thumbnail_url] to ensure old cover can be cleared from cache
 */
fun Manga.prepUpdateCover(coverCache: CoverCache, remoteManga: SManga, refreshSameUrl: Boolean) {
    // Never refresh covers if the new url is null, as the current url has possibly become invalid
    val newUrl = remoteManga.thumbnail_url ?: return

    // Never refresh covers if the url is empty to avoid "losing" existing covers
    if (newUrl.isEmpty()) return

    if (!refreshSameUrl && thumbnail_url == newUrl) return

    when {
        isLocal() -> {
            cover_last_modified = Date().time
        }
        hasCustomCover(coverCache) -> {
            coverCache.deleteFromCache(this, false)
        }
        else -> {
            cover_last_modified = Date().time
            coverCache.deleteFromCache(this, false)
        }
    }
}

fun Manga.hasCustomCover(coverCache: CoverCache): Boolean {
    return coverCache.getCustomCoverFile(id).exists()
}

fun Manga.removeCovers(coverCache: CoverCache) {
    if (isLocal()) return

    cover_last_modified = Date().time
    coverCache.deleteFromCache(this, true)
}

fun Manga.updateCoverLastModified(db: DatabaseHelper) {
    cover_last_modified = Date().time
    db.updateMangaCoverLastModified(this).executeAsBlocking()
}

fun Manga.shouldDownloadNewChapters(db: DatabaseHelper, prefs: PreferencesHelper): Boolean {
    if (!favorite) return false

    // Boolean to determine if user wants to automatically download new chapters.
    val downloadNewChapter = prefs.downloadNewChapters().get()
    if (downloadNewChapter == 0) return false

    val restrictions = prefs.downloadNewDeviceRestriction().get()
    if (DEVICE_ONLY_ON_WIFI in restrictions && !prefs.context.isConnectedToWifi()) return false
    val batteryManager = prefs.context.batteryManager
    if (DEVICE_CHARGING in restrictions && !batteryManager.isCharging) return false
    val batteryLevel = batteryManager.getIntProperty(BATTERY_PROPERTY_CAPACITY)
    if (DEVICE_BATTERY_NOT_LOW in restrictions && batteryLevel <= 15) return false

    val includedCategories = prefs.downloadNewChapterCategories().get().map { it.toInt() }
    val excludedCategories = prefs.downloadNewChapterCategoriesExclude().get().map { it.toInt() }

    // Default: Download from all categories
    if (includedCategories.isEmpty() && excludedCategories.isEmpty()) return true

    // Get all categories, else default category (0)
    val categoriesForManga =
        db.getCategoriesForManga(this).executeAsBlocking()
            .mapNotNull { it.id }
            .takeUnless { it.isEmpty() } ?: listOf(0)

    // In excluded category
    if (categoriesForManga.any { it in excludedCategories }) return false

    // Included category not selected
    if (includedCategories.isEmpty()) return true

    // In included category
    return categoriesForManga.any { it in includedCategories }
}

/**
 * Filter the chapters to download among the new chapters of a manga
 */
fun Manga.getChaptersToDownload(
    newChapters: List<Chapter>,
    mangaChapters: List<ChapterItem>,
    preferences: PreferencesHelper,
): List<Chapter> {
    val skipWhenUnreadChapters = preferences.downloadNewSkipUnread().get()
    val downloadNewChaptersLimit = preferences.downloadNewChapters().get()

    val newChaptersIds = newChapters.map { it.id }
    val unreadChapters = mangaChapters.filter { !it.read && it.id !in newChaptersIds }

    if (skipWhenUnreadChapters && unreadChapters.isNotEmpty()) return emptyList()

    return if (downloadNewChaptersLimit != -1) {
        val downloadedUnreadChaptersCount = unreadChapters.count { it.isDownloaded }

        newChapters.sortedWith(getChapterSort(this, false))
            .take((downloadNewChaptersLimit - downloadedUnreadChaptersCount).coerceAtLeast(0))
    } else newChapters
}
