package com.yenaly.han1meviewer.ui.fragment.settings

import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.util.TranslationCache

class OtherSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_home, rootKey)

        val cachePref = findPreference<Preference>("translation_cache")
        updateCacheSummary(cachePref)

        cachePref?.setOnPreferenceClickListener {
            TranslationCache.clear()
            updateCacheSummary(cachePref)
            Toast.makeText(
                requireContext(),
                R.string.pref_translation_cache_cleared,
                Toast.LENGTH_SHORT
            ).show()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        val cachePref = findPreference<Preference>("translation_cache")
        updateCacheSummary(cachePref)
    }

    private fun updateCacheSummary(pref: Preference?) {
        val usedMB = TranslationCache.sizeInMB()
        pref?.summary = getString(R.string.pref_translation_cache_summary, usedMB)
    }
}
