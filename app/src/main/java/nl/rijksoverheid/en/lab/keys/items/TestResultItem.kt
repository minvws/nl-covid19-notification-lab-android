package nl.rijksoverheid.en.lab.keys.items

import android.annotation.SuppressLint
import android.view.View
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import nl.rijksoverheid.en.lab.R
import nl.rijksoverheid.en.lab.databinding.ItemTestResultBinding
import nl.rijksoverheid.en.lab.storage.model.TestResult

class TestResultItem(private val testResult: TestResult) : BindableItem<ItemTestResultBinding>() {
    @SuppressLint("SetTextI18n")
    override fun bind(viewBinding: ItemTestResultBinding, position: Int) {
        viewBinding.title.text = "${testResult.testId} - ${testResult.scannedDeviceId}"
        viewBinding.details.text = testResult.scannedTek
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