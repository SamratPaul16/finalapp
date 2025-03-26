package com.samrat.finalapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.samrat.finalapp.databinding.ActivityDiseaseSeverityBinding
import java.util.concurrent.Executors
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.pow

class DiseaseSeverityActivity : AppCompatActivity(), Detector.DetectorListener {

    private lateinit var binding: ActivityDiseaseSeverityBinding
    private val selectedImages = mutableListOf<String>()  // Store selected image paths
    private lateinit var detector: Detector  // TensorFlow Lite detector
    private val boundingBoxesPerImage = mutableListOf<List<BoundingBox>>() // Store bounding boxes for all images

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using ViewBinding
        binding = ActivityDiseaseSeverityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the TensorFlow Lite detector and pass 'this' as the listener
        detector = Detector(baseContext, Constants.MODEL_PATH, Constants.LABELS_PATH, this)
        detector.setup()

        // Set up select button click event
        binding.selectButton.setOnClickListener {
            selectImages()
        }

        // Set up calculate button click event
        binding.calculateButton.setOnClickListener {
            if (selectedImages.size < 10) {
                Toast.makeText(this, "Select at least 10 images", Toast.LENGTH_SHORT).show()
            } else {
                // Show Lottie animation and start classification
                showLoading(true)
                classifyImagesInBackground(selectedImages)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            // Show the Lottie animation
            binding.animationView.visibility = View.VISIBLE
            binding.animationView.playAnimation()  // Start animation
        } else {
            // Hide the Lottie animation
            binding.animationView.visibility = View.GONE
            binding.animationView.cancelAnimation()  // Stop animation
        }
    }

    private fun selectImages() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            selectedImages.clear()
            val clipData = data?.clipData

            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    val imageUri = clipData.getItemAt(i).uri
                    selectedImages.add(imageUri.toString())
                }
            } else {
                data?.data?.let { imageUri ->
                    selectedImages.add(imageUri.toString())
                }
            }

            Toast.makeText(this, "${selectedImages.size} images selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun classifyImagesInBackground(images: List<String>) {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            boundingBoxesPerImage.clear()  // Clear previous results

            images.forEach { imagePath ->
                val bitmap = getBitmapFromPath(imagePath)
                detector.detect(bitmap)  // This will call onDetect
            }

            runOnUiThread {
                processResults()  // After inference is done, process the results
                showLoading(false)  // Hide the animation
            }
        }
    }

    override fun onEmptyDetect() {
        // Handle empty detection case if needed
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        boundingBoxesPerImage.add(boundingBoxes)  // Store the results in memory
    }

    private fun getBitmapFromPath(path: String): Bitmap {
        val uri = Uri.parse(path)
        val inputStream = contentResolver.openInputStream(uri)
        try {
            return BitmapFactory.decodeStream(inputStream)
        } finally {
            inputStream?.close()
        }
    }

    private fun formatDecimal(value: Double): String {
        val df = DecimalFormat("#.###")
        df.roundingMode = RoundingMode.CEILING
        return df.format(value)
    }

    private fun processResults() {
        val numGroups = boundingBoxesPerImage.size / 10 + if (boundingBoxesPerImage.size % 10 != 0) 1 else 0
        val imageGroups = boundingBoxesPerImage.chunked(10)

        // Classify and calculate severity
        val classifications = imageGroups.map { group ->
            group.map { boundingBoxes -> classifyBoundingBoxes(boundingBoxes) }
        }

        val (avgMitesSeverity, avgVirusSeverity, avgTotalSeverity) = calculateSeverity(classifications, numGroups)

        binding.severityResult.text =
            "Severity:\n" +
                    "Mites: ${formatDecimal(avgMitesSeverity * 100)}%\n" +
                    "Virus: ${formatDecimal(avgVirusSeverity * 100)}%\n" +
                    "Total: ${formatDecimal(avgTotalSeverity * 100)}%"
    }

    private fun classifyBoundingBoxes(boundingBoxes: List<BoundingBox>): String {
        var mitesCount = 0
        var virusCount = 0
        var healthyCount = 0
        val totalCount = boundingBoxes.size

        // Count bounding boxes for each class
        boundingBoxes.forEach { box ->
            when (box.cls) {
                0 -> healthyCount++
                1 -> mitesCount++
                2 -> virusCount++
            }
        }

        // Mixed class: If all classes have the same number of bounding boxes
        if (healthyCount == mitesCount && mitesCount == virusCount) {
            return "Mixed"
        }

        // Otherwise, assign to the class with the highest count
        return when {
            healthyCount > mitesCount && healthyCount > virusCount -> "Healthy"
            mitesCount > healthyCount && mitesCount > virusCount -> "Mites"
            virusCount > healthyCount && virusCount > mitesCount -> "Virus"
            else -> "Mixed" // If there's no clear majority, assign to Mixed
        }
    }

    private fun calculateSeverity(
        groups: List<List<String>>,
        numGroups: Int
    ): Triple<Double, Double, Double> {
        var totalMitesSeverity = 0.0
        var totalVirusSeverity = 0.0
        var totalInfectedSeverity = 0.0

        val mitesSeverities = mutableListOf<Double>()
        val virusSeverities = mutableListOf<Double>()
        val totalSeverities = mutableListOf<Double>()  // For Total (Mites + Virus + Mixed)

        groups.forEachIndexed { i, group ->
            var xMites = 0
            var xVirus = 0
            var xMixed = 0  // For Total (Mites + Virus + Mixed)
            val n = group.size

            group.forEach { classification ->
                when (classification) {
                    "Mites" -> xMites++
                    "Virus" -> xVirus++
                    "Mixed" -> xMixed++  // Mixed counts toward both Mites and Virus
                }
            }

            // Print debug information for the current group
            println("Group $i: Mites=$xMites, Virus=$xVirus, Mixed=$xMixed, Total=$n")

            val mitesWithMixed = xMites + xMixed
            val virusWithMixed = xVirus + xMixed

            if (mitesWithMixed > 0) {
                val severityMites = 1 - (((n.toDouble() - mitesWithMixed.toDouble()) / n.toDouble()).pow(1.0 / numGroups.toDouble()))
                mitesSeverities.add(severityMites)
                totalMitesSeverity += severityMites
            }

            if (virusWithMixed > 0) {
                val severityVirus = 1 - (((n.toDouble() - virusWithMixed.toDouble()) / n.toDouble()).pow(1.0 / numGroups.toDouble()))
                virusSeverities.add(severityVirus)
                totalVirusSeverity += severityVirus
            }

            val xInfected = xMites + xVirus + xMixed
            if (xInfected > 0) {
                val severityTotal = 1 - (((n.toDouble() - xInfected.toDouble()) / n.toDouble()).pow(1.0 / numGroups.toDouble()))
                totalSeverities.add(severityTotal)
                totalInfectedSeverity += severityTotal
            }
        }

        val avgMitesSeverity = if (mitesSeverities.isNotEmpty()) totalMitesSeverity / mitesSeverities.size else 0.0
        val avgVirusSeverity = if (virusSeverities.isNotEmpty()) totalVirusSeverity / virusSeverities.size else 0.0
        val avgTotalSeverity = if (totalSeverities.isNotEmpty()) totalInfectedSeverity / totalSeverities.size else 0.0

        return Triple(avgMitesSeverity, avgVirusSeverity, avgTotalSeverity)
    }
}
