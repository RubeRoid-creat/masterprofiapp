package com.example.bestapp.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.bestapp.R
import com.example.bestapp.data.PreferencesManager
import com.google.android.material.button.MaterialButton

class CitySelectionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_city_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_group_cities)
        val btnContinue = view.findViewById<MaterialButton>(R.id.btn_continue)

        btnContinue.setOnClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            if (selectedId == View.NO_ID) {
                Toast.makeText(requireContext(), R.string.city_select_error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedRadio = view.findViewById<RadioButton>(selectedId)
            val cityName = selectedRadio.text.toString()

            val prefs = PreferencesManager.getInstance(requireContext())
            prefs.setSelectedCity(cityName)

            findNavController().navigate(R.id.action_citySelection_to_login)
        }
    }
}







