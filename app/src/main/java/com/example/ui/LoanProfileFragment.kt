package com.example.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.R
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

class LoanProfileFragment : Fragment() {

    private var adView: AdView? = null
    private var btnSubmitLoan: Button? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_loan_profile, container, false)

        val etFullName = view.findViewById<EditText>(R.id.etFullName)
        val etMobile = view.findViewById<EditText>(R.id.etMobile)
        val etUdyamNo = view.findViewById<EditText>(R.id.etUdyamNo)
        val spLoanType = view.findViewById<Spinner>(R.id.spLoanType)
        btnSubmitLoan = view.findViewById(R.id.btnSubmitLoan)
        adView = view.findViewById(R.id.adView)

        // 1. Setup Spinner Options
        val loanTypes = arrayOf(
            "PM SVANidhi Loan (स्ट्रीट वेंडर लोन)",
            "Mudra Loan (शिशु, किशोर, तरुण)",
            "MSME Business Loan (बिजनेस लोन)",
            "PMEGP योजना (सब्सिडी लोन)"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, loanTypes)
        spLoanType.adapter = adapter

        // 2. Load AdMob Banner with Lifecycle Protection
        adView?.let { ad ->
            ad.adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    // Safe error fallback
                }
            }
            ad.loadAd(AdRequest.Builder().build())
        }

        // 3. Submit Action with validation
        btnSubmitLoan?.setOnClickListener {
            val name = etFullName.text.toString().trim()
            val mobile = etMobile.text.toString().trim()
            val udyam = etUdyamNo.text.toString().trim()
            val selectedLoan = spLoanType.selectedItem.toString()

            if (name.isEmpty()) {
                etFullName.error = "कृपया नाम दर्ज करें"
                etFullName.requestFocus()
                return@setOnClickListener
            }

            if (mobile.length != 10) {
                etMobile.error = "10 अंकों का मोबाइल नंबर दर्ज करें"
                etMobile.requestFocus()
                return@setOnClickListener
            }

            btnSubmitLoan?.isEnabled = false
            Toast.makeText(
                requireContext(),
                "धन्यवाद $name! $selectedLoan के लिए आवेदन दर्ज किया गया।",
                Toast.LENGTH_LONG
            ).show()

            btnSubmitLoan?.isEnabled = true
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        adView?.resume()
    }

    override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    override fun onDestroyView() {
        adView?.destroy()
        adView = null
        btnSubmitLoan = null
        super.onDestroyView()
    }
}
