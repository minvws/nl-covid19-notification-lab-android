/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lab

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModelProvider
import nl.rijksoverheid.en.lab.databinding.ActivityMainBinding
import nl.rijksoverheid.en.lab.keys.KeysFragment
import nl.rijksoverheid.en.lab.status.NotificationsStatusFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_status -> supportFragmentManager.commitNow {
                    replace(R.id.fragment, NotificationsStatusFragment())
                }
                R.id.nav_keys -> supportFragmentManager.commitNow {
                    replace(
                        R.id.fragment,
                        KeysFragment()
                    )
                }
            }
            true
        }

        binding.bottomNav.setOnNavigationItemReselectedListener { /* nothing */ }
        if (savedInstanceState == null) {
            supportFragmentManager.commitNow {
                add(
                    R.id.fragment,
                    NotificationsStatusFragment()
                )
            }
        }
    }

    override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
        return LabViewModelFactory(applicationContext)
    }
}
