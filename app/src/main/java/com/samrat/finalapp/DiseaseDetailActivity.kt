package com.samrat.finalapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.samrat.finalapp.databinding.ActivityDiseaseDetailBinding

class DiseaseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDiseaseDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiseaseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val disease = intent.getParcelableExtra<Disease>("disease")

        disease?.let {
            binding.diseaseName.text = it.name
            binding.scientificName.text = it.scientificName
            binding.symptoms.text = it.symptoms.joinToString("\n")
            binding.management.text = it.management.joinToString("\n")
            binding.alsoFoundIn.text = it.alsoFoundIn.joinToString(", ")

            // Ensure correct format for image resource
            val imageName = if (it.imageResId.startsWith("disease_")) it.imageResId else "disease_${it.imageResId}"
            val imageResId = resources.getIdentifier(imageName, "drawable", packageName)

            Log.d("DiseaseDetailActivity", "Trying to load image: $imageName, Found ID: $imageResId")

            if (imageResId != 0) {
                binding.diseaseDetailImage.setImageResource(imageResId)
            } else {
                Log.w("DiseaseDetailActivity", "Image not found for: $imageName")
            }
        } ?: Log.e("DiseaseDetailActivity", "No disease data received!")
    }
}
