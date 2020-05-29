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
import nl.rijksoverheid.entoolkit.app.databinding.ItemAdvertiserStatusBinding

class AdvertiserItem(private val status: ScannerAdvertiserService.AdvertisingState.Started) :
    BaseDatabindingItem<ItemAdvertiserStatusBinding>() {
    override fun bind(viewBinding: ItemAdvertiserStatusBinding, position: Int) {
        viewBinding.status = status
    }

    override fun getLayout(): Int = R.layout.item_advertiser_status

    override fun isSameAs(other: Item<*>): Boolean {
        return other is AdvertiserItem
    }

    override fun hasSameContentAs(other: Item<*>): Boolean {
        return other is AdvertiserItem && other.status == status
    }
}
