/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab.keys.items

import android.annotation.SuppressLint
import android.text.format.DateFormat
import android.view.View
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import nl.rijksoverheid.en.lab.R
import nl.rijksoverheid.en.lab.databinding.ItemTestResultBinding
import nl.rijksoverheid.en.lab.storage.model.TestResult
import java.text.SimpleDateFormat
import java.util.Locale

class TestResultItem(private val testResult: TestResult) : BindableItem<ItemTestResultBinding>() {
    @SuppressLint("SetTextI18n")
    override fun bind(viewBinding: ItemTestResultBinding, position: Int) {
        val formattedDate = SimpleDateFormat(
            DateFormat.getBestDateTimePattern(
                Locale.getDefault(), "dd-MM-yyyy HH:mm"
            ),
            Locale.getDefault()
        ).format(testResult.timestamp)
        viewBinding.title.text = "${testResult.testId} - ${testResult.scannedDeviceId}"
        viewBinding.dateTime.text = formattedDate
        viewBinding.tekId.text = testResult.scannedTek.trim()
        val scans = testResult.exposureWindows.sumBy { it.scanInstances.size }
        val scanInstances = testResult.exposureWindows.flatMap { it.scanInstances }
        val min = scanInstances.minByOrNull { it.minAttenuationDb }?.minAttenuationDb ?: 0
        val typical = scanInstances.map { it.typicalAttenuationDb }.average().toInt()
        val duration = scanInstances.sumBy { it.secondsSinceLastScan }
        viewBinding.scanSummary.text =
            if (scans == 0) viewBinding.root.context.getString(R.string.test_result_no_scans) else viewBinding.root.context.getString(
                R.string.scan_instance_result,
                min,
                typical,
                duration
            )
    }

    override fun getLayout(): Int = R.layout.item_test_result
    override fun initializeViewBinding(view: View): ItemTestResultBinding =
        ItemTestResultBinding.bind(view)

    override fun isSameAs(other: Item<*>): Boolean {
        return other is TestResultItem && other.testResult.id == testResult.id
    }

    override fun hasSameContentAs(other: Item<*>): Boolean {
        return other is TestResultItem && other.testResult == testResult
    }
}
