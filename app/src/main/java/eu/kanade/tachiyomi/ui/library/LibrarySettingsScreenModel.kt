package eu.kanade.tachiyomi.ui.library

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.coroutineScope
import eu.kanade.core.prefs.asState
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.category.interactor.SetDisplayModeForCategory
import eu.kanade.domain.category.interactor.SetSortModeForCategory
import eu.kanade.domain.library.service.LibraryPreferences
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.util.preference.toggle
import eu.kanade.tachiyomi.widget.TriState
import tachiyomi.core.preference.Preference
import tachiyomi.core.preference.getAndSet
import tachiyomi.core.util.lang.launchIO
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibrarySort
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LibrarySettingsScreenModel(
    val preferences: BasePreferences = Injekt.get(),
    val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val setDisplayModeForCategory: SetDisplayModeForCategory = Injekt.get(),
    private val setSortModeForCategory: SetSortModeForCategory = Injekt.get(),
    trackManager: TrackManager = Injekt.get(),
) : ScreenModel {

    val trackServices = trackManager.services.filter { service -> service.isLogged }

    // SY -->
    val grouping by libraryPreferences.groupLibraryBy().asState(coroutineScope)

    // SY <--
    fun togglePreference(preference: (LibraryPreferences) -> Preference<Boolean>) {
        preference(libraryPreferences).toggle()
    }

    fun toggleFilter(preference: (LibraryPreferences) -> Preference<Int>) {
        preference(libraryPreferences).getAndSet {
            TriState.valueOf(it).next().value
        }
    }

    fun toggleTracker(id: Int) {
        toggleFilter { libraryPreferences.filterTracking(id) }
    }

    fun setDisplayMode(category: Category, mode: LibraryDisplayMode) {
        coroutineScope.launchIO {
            setDisplayModeForCategory.await(category, mode)
        }
    }

    fun setSort(category: Category, mode: LibrarySort.Type, direction: LibrarySort.Direction) {
        coroutineScope.launchIO {
            setSortModeForCategory.await(category, mode, direction)
        }
    }

    // SY -->
    fun setGrouping(grouping: Int) {
        coroutineScope.launchIO {
            libraryPreferences.groupLibraryBy().set(grouping)
        }
    }
    // SY <--
}