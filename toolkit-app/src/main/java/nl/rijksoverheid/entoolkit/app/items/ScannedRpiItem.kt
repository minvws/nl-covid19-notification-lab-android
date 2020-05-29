/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.entoolkit.app.items

import com.xwray.groupie.Item
import nl.rijksoverheid.entoolkit.app.R
import nl.rijksoverheid.entoolkit.app.ScannerAdvertiserService
import nl.rijksoverheid.entoolkit.app.databinding.ItemScannedRpiBinding

class ScannedRpiItem(private val scannedRpi: ScannerAdvertiserService.ScannedRpi) :
    BaseDatabindingItem<ItemScannedRpiBinding>() {
    override fun bind(viewBinding: ItemScannedRpiBinding, position: Int) {
        viewBinding.rpi = scannedRpi
    }

    override fun getLayout(): Int = R.layout.item_scanned_rpi

    override fun isSameAs(other: Item<*>): Boolean {
        return other is ScannedRpiItem && other.scannedRpi.rpiHex == scannedRpi.rpiHex
    }

    override fun hasSameContentAs(other: Item<*>): Boolean {
        return other is ScannedRpiItem && other.scannedRpi.rpiHex == scannedRpi.rpiHex && other.scannedRpi.rssi == scannedRpi.rssi
    }
}
