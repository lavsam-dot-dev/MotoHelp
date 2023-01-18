package ru.motohelp.app.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import coil.ImageLoader
import coil.request.ImageRequest
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import ru.motohelp.app.R
import ru.motohelp.app.data.AppConstants.ANONIMUSUSERNAME


class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_fragment, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user = FirebaseAuth.getInstance().currentUser
        val pref = findPreference<Preference>("user")

        user.let {

            if (user?.isAnonymous!!) {
                pref?.title = ANONIMUSUSERNAME
            } else {
                pref?.title = user.displayName
                pref?.summary = user.email
            }

        }

        val imageLoader = ImageLoader(requireContext())
        val request = ImageRequest.Builder(requireContext())
            .data(user?.photoUrl)
            .target { drawable ->
                pref?.icon = drawable
            }
            .build()
        imageLoader.enqueue(request)

        val dropDownPreference = findPreference<DropDownPreference>("MapType")
        dropDownPreference?.setOnPreferenceChangeListener { preference, newValue ->
            preference.summary = newValue.toString()
            true
        }
        dropDownPreference?.summary = dropDownPreference?.value


    }


    override fun onPreferenceTreeClick(preference: Preference): Boolean {

        when (preference.key) {
            "signOut" -> {
                AuthUI.getInstance()
                    .signOut(requireContext())
                    .addOnCompleteListener {
                        val pref = findPreference<Preference>("user")
                        pref?.title = "Sign In"
                        pref?.icon = null
                        pref?.summary = null
                    }
                requireActivity().finish()
                startActivity(requireActivity().intent)
            }

            "user" -> {
                if (FirebaseAuth.getInstance().currentUser == null) {
                    requireActivity().finish()
                    startActivity(requireActivity().intent)
                }
            }
        }

        return super.onPreferenceTreeClick(preference)
    }


}
