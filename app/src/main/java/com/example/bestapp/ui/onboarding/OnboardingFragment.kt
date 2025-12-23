package com.example.bestapp.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.bestapp.R
import com.example.bestapp.data.PreferencesManager
import com.google.android.material.button.MaterialButton

class OnboardingFragment : Fragment() {

    private var currentPage = 0

    private val titles by lazy {
        listOf(
            getString(R.string.onboarding_title_1),
            getString(R.string.onboarding_title_2),
            getString(R.string.onboarding_title_3)
        )
    }

    private val descriptions by lazy {
        listOf(
            getString(R.string.onboarding_desc_1),
            getString(R.string.onboarding_desc_2),
            getString(R.string.onboarding_desc_3)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_onboarding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val image = view.findViewById<ImageView>(R.id.image)
        val title = view.findViewById<TextView>(R.id.title)
        val description = view.findViewById<TextView>(R.id.description)
        val pageIndicator = view.findViewById<TextView>(R.id.page_indicator)
        val btnSkip = view.findViewById<MaterialButton>(R.id.btn_skip)
        val btnNext = view.findViewById<MaterialButton>(R.id.btn_next)

        fun updatePage() {
            title.text = titles[currentPage]
            description.text = descriptions[currentPage]
            pageIndicator.text = "${currentPage + 1}/${titles.size}"

            if (currentPage == titles.lastIndex) {
                btnNext.setText(R.string.onboarding_finish)
                btnSkip.visibility = View.INVISIBLE
            } else {
                btnNext.setText(R.string.onboarding_next)
                btnSkip.visibility = View.VISIBLE
            }
        }

        fun finishOnboarding() {
            val prefs = PreferencesManager.getInstance(requireContext())
            prefs.setOnboardingShown(true)
            findNavController().navigate(R.id.action_onboarding_to_citySelection)
        }

        btnSkip.setOnClickListener {
            finishOnboarding()
        }

        btnNext.setOnClickListener {
            if (currentPage < titles.lastIndex) {
                currentPage++
                updatePage()
            } else {
                finishOnboarding()
            }
        }

        updatePage()
    }
}







