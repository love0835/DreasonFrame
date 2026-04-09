package com.dreason.frame

import android.app.Application
import com.dreason.frame.core.preferences.AppPreferences
import com.dreason.frame.core.repository.RuleRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DreasonApp : Application() {

    @Inject lateinit var ruleRepository: RuleRepository
    @Inject lateinit var preferences: AppPreferences

    override fun onCreate() {
        super.onCreate()

        // Import built-in rules on first launch
        CoroutineScope(Dispatchers.IO).launch {
            val imported = preferences.rulesImported.first()
            if (!imported) {
                ruleRepository.importBuiltInRules()
                preferences.setRulesImported(true)
            }
        }
    }
}
