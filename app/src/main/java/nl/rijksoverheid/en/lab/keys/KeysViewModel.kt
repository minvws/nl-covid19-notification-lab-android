/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab.keys

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.lab.ImportTemporaryExposureKeysResult
import nl.rijksoverheid.en.lab.NotificationsRepository
import nl.rijksoverheid.en.lab.exposurenotification.StatusResult
import nl.rijksoverheid.en.lab.lifecyle.Event
import java.io.File

class KeysViewModel(private val repository: NotificationsRepository, val deviceName: String) :
    ViewModel() {

    val lastResults = repository.getTestResults().asLiveData(viewModelScope.coroutineContext)
    val importResult: LiveData<Event<ImportTemporaryExposureKeysResult>> = MutableLiveData()
    val exportFile: LiveData<Event<File>> = MutableLiveData()

    val scanEnabled: LiveData<Boolean> =
        liveData { emit(repository.getStatus() == StatusResult.Enabled) }

    fun importKey(tek: TemporaryExposureKey, sourceDeviceId: String, testId: String) {
        repository.setSourceAndTestId(sourceDeviceId, testId)
        viewModelScope.launch {
            (importResult as MutableLiveData).value =
                Event(repository.importTemporaryExposureKey(tek))
        }
    }

    fun exportResults() {
        viewModelScope.launch {
            val file = repository.exportResults()
            (exportFile as MutableLiveData).value = Event(file)
        }
    }

    fun clearResults() {
        viewModelScope.launch {
            repository.clearResults()
        }
    }
}
