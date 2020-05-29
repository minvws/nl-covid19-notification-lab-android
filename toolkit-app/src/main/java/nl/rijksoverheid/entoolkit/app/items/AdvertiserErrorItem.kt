/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.entoolkit.app.items

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import nl.rijksoverheid.entoolkit.app.R
import nl.rijksoverheid.entoolkit.app.databinding.ItemAdvertiserErrorBinding

class AdvertiserErrorItem : BindableItem<ItemAdvertiserErrorBinding>() {
    override fun getLayout(): Int = R.layout.item_advertiser_error

    override fun bind(viewBinding: ItemAdvertiserErrorBinding, position: Int) {
        // nothing to bind
    }

    override fun initializeViewBinding(view: View): ItemAdvertiserErrorBinding {
        return ItemAdvertiserErrorBinding.bind(view)
    }
}
