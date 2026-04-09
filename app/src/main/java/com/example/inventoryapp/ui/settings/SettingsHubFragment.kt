package com.example.inventoryapp.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.FragmentSettingsHubBinding

class SettingsHubFragment : Fragment() {

    private var _binding: FragmentSettingsHubBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsHubBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pokaż tylko kartę ustawień drukarki — pozostałe ukryj
        binding.cardPrinterSettings.setOnClickListener {
            findNavController().navigate(R.id.printerSettingsFragment)
        }

        binding.cardCompanies.visibility = View.GONE
        binding.cardCompanies.isClickable = false
        binding.cardCompanies.isFocusable = false

        binding.cardContractorPoints.visibility = View.GONE
        binding.cardContractorPoints.isClickable = false
        binding.cardContractorPoints.isFocusable = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
