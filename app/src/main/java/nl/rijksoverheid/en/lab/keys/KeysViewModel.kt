/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab.keys

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.lab.NotificationsRepository
import nl.rijksoverheid.en.lab.exposurenotification.StatusResult

class KeysViewModel(private val repository: NotificationsRepository, val deviceName: String) :
    ViewModel() {

    val lastResults: LiveData<NotificationsRepository.TestResults> =
        repository.getTestResults().asLiveData(context = viewModelScope.coroutineContext)

    val scanEnabled: LiveData<Boolean> =
        liveData { emit(repository.getStatus() == StatusResult.Enabled) }

    fun importKey(tek: TemporaryExposureKey, sourceDeviceId: String, testId: String) {
        repository.clearExposureInformation()
        repository.setSourceAndTestId(sourceDeviceId, testId)
        viewModelScope.launch {
            repository.importTemporaryExposureKey(tek)
        }
    }
}
