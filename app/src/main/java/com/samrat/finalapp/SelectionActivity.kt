package com.samrat.finalapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.samrat.finalapp.databinding.ActivitySelectionBinding
import java.io.InputStream

class SelectionActivity : AppCompatActivity(), Detector.DetectorListener, NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivitySelectionBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var detector: Detector
    private var selectedImageBitmap: Bitmap? = null
    private var detectedClasses = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        detector = Detector(this, "model.tflite", "labels.txt", this)
        detector.setup()

        setSupportActionBar(binding.toolbar)
        drawerLayout = binding.drawerLayout
        val navView = binding.navView
        val toggle = ActionBarDrawerToggle(this, drawerLayout, binding.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navView.setNavigationItemSelectedListener(this)

        binding.selectButton.setOnClickListener {
            selectImageFromGallery()
        }

        binding.cameraButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivityForResult(intent, CAMERA_REQUEST_CODE)
        }

        binding.predictButton.setOnClickListener {
            selectedImageBitmap?.let {
                // Do not clear here; let detection append to existing results
                runModelInference(it)
            } ?: Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
        }

        binding.resultsButton.setOnClickListener {
            Log.d("SelectionActivity", "Sending to ResultsActivity: $detectedClasses")
            if (detectedClasses.isEmpty()) {
                Toast.makeText(this, "No results to display", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, ResultsActivity::class.java)
                intent.putStringArrayListExtra("detectedClasses", ArrayList(detectedClasses))
                startActivity(intent)
            }
        }
    }

    private val selectImageFromGallery = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            displaySelectedImage(it)
        }
    }

    private fun selectImageFromGallery() {
        selectImageFromGallery.launch("image/*")
    }

    private fun displaySelectedImage(uri: Uri) {
        try {
            val imageStream: InputStream? = contentResolver.openInputStream(uri)
            selectedImageBitmap = BitmapFactory.decodeStream(imageStream)
            binding.demoImage.setImageBitmap(selectedImageBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun runModelInference(bitmap: Bitmap) {
        detector.detect(bitmap)
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            displayResults(boundingBoxes)
        }
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            Toast.makeText(this, "No objects detected", Toast.LENGTH_SHORT).show()
            Log.d("SelectionActivity", "Detection returned no results")
        }
    }

    private fun displayResults(results: List<BoundingBox>) {
        val bitmap = binding.demoImage.drawable.toBitmap().copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        val textPaint = Paint().apply {
            color = Color.RED
            textSize = 50f
            style = Paint.Style.FILL
        }

        results.forEach { rect ->
            val rectF = RectF(
                rect.x1 * bitmap.width,
                rect.y1 * bitmap.height,
                rect.x2 * bitmap.width,
                rect.y2 * bitmap.height
            )
            canvas.drawRect(rectF, paint)
            canvas.drawText(rect.clsName, rectF.left, rectF.top, textPaint)
            detectedClasses.add(rect.clsName)
            Log.d("SelectionActivity", "Detected class: ${rect.clsName}, Total: $detectedClasses")
        }

        binding.demoImage.setImageBitmap(bitmap)
        updateResultsBadge()
    }

    private fun updateResultsBadge() {
        if (detectedClasses.isNotEmpty()) {
            binding.notificationBadge.text = detectedClasses.size.toString()
            binding.notificationBadge.visibility = View.VISIBLE
        } else {
            binding.notificationBadge.visibility = View.GONE
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_developers -> {
                startActivity(Intent(this, DevelopersActivity::class.java))
            }
            R.id.nav_disease_severity -> {
                startActivity(Intent(this, DiseaseSeverityActivity::class.java))
            }
        }
        drawerLayout.closeDrawers()
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.getStringArrayListExtra("detectedClasses")?.let { classes ->
                detectedClasses.addAll(classes) // Append, don’t clear
                Log.d("SelectionActivity", "Received from MainActivity: $detectedClasses")
                updateResultsBadge()
            }
        }
    }

    companion object {
        private const val CAMERA_REQUEST_CODE = 1
    }
}