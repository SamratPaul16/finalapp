package com.samrat.finalapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.samrat.finalapp.databinding.ActivityResultsBinding

class ResultsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultsBinding
    private lateinit var diseases: List<Disease>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load diseases
        diseases = JsonDataLoader.loadDiseases(this, "Chilli")
        Log.d("ResultsActivity", "Loaded diseases: ${diseases.map { it.name }}")

        // Get detected classes
        val detectedClasses = intent.getStringArrayListExtra("detectedClasses") ?: emptyList()
        Log.d("ResultsActivity", "Received detected classes: $detectedClasses")

        val layout = binding.diseaseOptionsLayout
        layout.removeAllViews()

        if (detectedClasses.isEmpty()) {
            Log.w("ResultsActivity", "No detected classes received. Showing message.")
            val noResultsTextView = TextView(this).apply {
                text = "No diseases detected."
                textSize = 18f
                setPadding(16, 16, 16, 16)
            }
            layout.addView(noResultsTextView)
            return
        }

        // Create buttons for detected diseases
        for (cls in detectedClasses) {
            Log.d("ResultsActivity", "Processing detected class: $cls")

            val disease = diseases.find { disease ->
                disease.name.contains(cls, ignoreCase = true) || cls.contains(disease.name, ignoreCase = true)
            }

            if (disease != null) {
                Log.d("ResultsActivity", "Matched disease: ${disease.name}")

                val button = Button(this).apply {
                    text = disease.name
                    textSize = 16f
                    setPadding(16, 16, 16, 16)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setOnClickListener {
                        Log.d("ResultsActivity", "Clicked on: ${disease.name}")
                        val intent = Intent(this@ResultsActivity, DiseaseDetailActivity::class.java)
                        intent.putExtra("disease", disease)
                        startActivity(intent)
                    }
                }

                layout.addView(button)
            } else {
                Log.w("ResultsActivity", "No disease found for class: $cls")
            }
        }
    }
}
