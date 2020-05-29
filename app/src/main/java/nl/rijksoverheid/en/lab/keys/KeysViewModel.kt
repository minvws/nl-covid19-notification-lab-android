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
import androidx.lifecycle.viewModelScope
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.lab.NotificationsRepository

class KeysViewModel(private val repository: NotificationsRepository) : ViewModel() {

    val lastResults: LiveData<List<NotificationsRepository.ExposureInfo>> =
        repository.getExposureInformation().asLiveData(context = viewModelScope.coroutineContext)

    fun importKey(tek: TemporaryExposureKey) {
        viewModelScope.launch {
            repository.importTemporaryExposureKey(tek)
        }
    }
}
