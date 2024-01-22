package eu.kanade.domain.extension.interactor

import android.content.pm.PackageInfo
import androidx.core.content.pm.PackageInfoCompat
import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.domain.UnsortedPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TrustExtension(
    private val preferences: SourcePreferences,
) {
    val unsortedPreferences = Injekt.get<UnsortedPreferences>()

    fun isTrusted(pkgInfo: PackageInfo, signatureHash: String): Boolean {
        if (unsortedPreferences.superSecretSetting().get() > 2) return true
        val key = "${pkgInfo.packageName}:${PackageInfoCompat.getLongVersionCode(pkgInfo)}:$signatureHash"
        return key in preferences.trustedExtensions().get()
    }

    fun trust(pkgName: String, versionCode: Long, signatureHash: String) {
        preferences.trustedExtensions().getAndSet { exts ->
            // Remove previously trusted versions
            val removed = exts.filterNot { it.startsWith("$pkgName:") }.toMutableSet()

            removed.also {
                it += "$pkgName:$versionCode:$signatureHash"
            }
        }
    }

    fun revokeAll() {
        preferences.trustedExtensions().delete()
    }
}
