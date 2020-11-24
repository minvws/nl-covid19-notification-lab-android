/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab.keys.items

import android.content.Context
import com.xwray.groupie.Item
import com.xwray.groupie.Section
import nl.rijksoverheid.en.lab.storage.model.TestResult

class TestResultSection(private val context: Context) : Section() {
    var testResults: List<TestResult> = emptyList()
        set(value) {
            if (field != value) {
                field = value
                updateTestResults()
            }
        }

    private fun updateTestResults() {
        val items = mutableListOf<Item<*>>()
        for (i in testResults.indices) {
            items.add(TestResultItem(testResults[i]))
        }
        update(items)
    }
}
