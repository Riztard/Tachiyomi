package eu.kanade.tachiyomi.data.updater

import android.content.Context
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.util.lang.withIOContext
import exh.syDebugVersion
import uy.kohesive.injekt.injectLazy
import java.util.Date
import java.util.concurrent.TimeUnit

class AppUpdateChecker {

    private val networkService: NetworkHelper by injectLazy()
    private val preferences: PreferencesHelper by injectLazy()

    suspend fun checkForUpdate(context: Context, isUserPrompt: Boolean = false): AppUpdateResult {
        // Limit checks to once a day at most
        if (isUserPrompt.not() && Date().time < preferences.lastAppCheck().get() + TimeUnit.DAYS.toMillis(1)) {
            return AppUpdateResult.NoNewUpdate
        }

        return withIOContext {
            val result = networkService.client
                .newCall(GET("https://api.github.com/repos/$GITHUB_REPO/releases/latest"))
                .await()
                .parseAs<GithubRelease>()
                .let {
                    preferences.lastAppCheck().set(Date().time)

                    // Check if latest version is different from current version
                    if (/* SY --> */ isNewVersionSY(it.version) /* SY <-- */) {
                        AppUpdateResult.NewUpdate(it)
                    } else {
                        AppUpdateResult.NoNewUpdate
                    }
                }

            if (result is AppUpdateResult.NewUpdate) {
                AppUpdateNotifier(context).promptUpdate(result.release)
            }

            result
        }
    }

    // SY -->
    private fun isNewVersionSY(versionTag: String) = (versionTag != BuildConfig.VERSION_NAME && (syDebugVersion == "0")) || ((syDebugVersion != "0") && versionTag != syDebugVersion)
    // SY <--

    private fun isNewVersion(versionTag: String): Boolean {
        // Removes prefixes like "r" or "v"
        val newVersion = versionTag.replace("[^\\d.]".toRegex(), "")

        return if (BuildConfig.DEBUG) {
            // Preview builds: based on releases in "tachiyomiorg/tachiyomi-preview" repo
            // tagged as something like "r1234"
            newVersion.toInt() > BuildConfig.COMMIT_COUNT.toInt()
        } else {
            // Release builds: based on releases in "tachiyomiorg/tachiyomi" repo
            // tagged as something like "v0.1.2"
            newVersion != BuildConfig.VERSION_NAME
        }
    }
}

val GITHUB_REPO: String by lazy {
    // Sy -->
    if (syDebugVersion != "0") {
        "" // "jobobby04/TachiyomiSYPreview"
    } else {
        "" // ""jobobby04/tachiyomiSY"
    }
    // SY <--
}
