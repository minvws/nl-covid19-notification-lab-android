/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.entoolkit.app.items

import androidx.annotation.StringRes
import nl.rijksoverheid.entoolkit.app.R
import nl.rijksoverheid.entoolkit.app.databinding.ItemHeaderBinding

class HeaderItem(@StringRes private val text: Int) : BaseDatabindingItem<ItemHeaderBinding>() {
    override fun bind(viewBinding: ItemHeaderBinding, position: Int) {
        viewBinding.text = text
    }

    override fun getLayout(): Int = R.layout.item_header
}
