package ru.motohelp.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.location.LocationManager
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import ru.motohelp.app.databinding.ActivityMainBinding
import ru.motohelp.app.model.location.LocationProviderChangedReceiver
import ru.motohelp.app.ui.fragments.MapScreenFragment
import ru.motohelp.app.ui.fragments.RecyclerViewFragment
import ru.motohelp.app.ui.fragments.SettingsFragment
import ru.motohelp.app.utils.isDarkTheme
import ru.motohelp.app.utils.showCustomToast


class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            startSignInIntent()
        }
        val br: BroadcastReceiver = LocationProviderChangedReceiver()
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        registerReceiver(br, filter)

        navigationMenu()
    }

    private fun startSignInIntent() {
        fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
            if (result.resultCode == RESULT_OK) {
//                showFragment(MapScreenFragment.newInstance())
                supportFragmentManager.beginTransaction()
                    .replace(R.id.placeHolder, MapScreenFragment.newInstance())
                    .commit()

//                navigationMenu()

            } else {
                Toast(this).showCustomToast(
                    resources.getString(R.string.login_failed),
                    2,
                    this
                )
            }
        }

        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.AnonymousBuilder().build()
        )
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setLogo(R.mipmap.ic_launcher)
            .setAlwaysShowSignInMethodScreen(false)
            .setLockOrientation(true)
            .build()

        val signInLauncher = registerForActivityResult(
            FirebaseAuthUIActivityResultContract()
        ) { res ->
            onSignInResult(res)
        }

        signInLauncher.launch(signInIntent)
    }

    private fun navigationMenu() {
        binding.botNavig.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_bar_return_to_map -> {
//                    showFragment(MapScreenFragment.newInstance())
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.placeHolder, MapScreenFragment.newInstance())
                        .commit()
                }
                R.id.nav_bar_fab_my_setting_button -> {
//                    showFragment(SettingsFragment())
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.placeHolder,SettingsFragment())
                        .commit()
                }
                R.id.nav_bar_accident_list_fragment_button -> {
//                    showFragment(RecyclerViewFragment())
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.placeHolder, RecyclerViewFragment())
                        .commit()
                }

            }
            true
        }
        binding.botNavig.setOnItemReselectedListener {
            //
        }
    }
}
