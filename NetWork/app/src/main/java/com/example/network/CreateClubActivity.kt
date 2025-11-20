package com.example.network

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.network.database.DatabaseRepository
import com.example.network.utils.SessionManager
import com.google.android.gms.maps.model.LatLng
import java.io.File

class CreateClubActivity : AppCompatActivity() {

    private lateinit var repository: DatabaseRepository
    private lateinit var sessionManager: SessionManager

    private var selectedLatLng: LatLng? = null
    private lateinit var previewRow: LinearLayout

    private val selectedMedia = mutableListOf<Pair<String, String>>()

    private var tempFile: File? = null
    private var tempUri: Uri? = null

    companion object {
        const val REQUEST_IMAGE_PICK = 1
        const val REQUEST_IMAGE_CAPTURE = 2
        const val REQUEST_VIDEO_PICK = 3
        const val REQUEST_VIDEO_CAPTURE = 4
        const val REQUEST_LOCATION_SELECT = 5000

        const val CAMERA_PERMISSION_REQUEST = 900
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_club)

        repository = DatabaseRepository(this)
        sessionManager = SessionManager(this)
        previewRow = findViewById(R.id.mediaPreviewRow)

        // Sport spinner
        val sports = listOf("Basketball", "Soccer", "Tennis", "Volleyball", "Running", "Badminton")
        findViewById<Spinner>(R.id.sportTypeSpinner).adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, sports).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        // Location
        findViewById<Button>(R.id.chooseLocationBtn).setOnClickListener {
            startActivityForResult(Intent(this, SelectLocationActivity::class.java), REQUEST_LOCATION_SELECT)
        }

        // Gallery select
        findViewById<Button>(R.id.uploadImageBtn).setOnClickListener { openGalleryImage() }
        findViewById<Button>(R.id.uploadVideoBtn).setOnClickListener { openGalleryVideo() }

        // Capture operations (check camera permission!)
        findViewById<Button>(R.id.captureImageBtn).setOnClickListener {
            if (hasCameraPermission()) captureImage() else requestCameraPermission()
        }

        findViewById<Button>(R.id.captureVideoBtn).setOnClickListener {
            if (hasCameraPermission()) captureVideo() else requestCameraPermission()
        }

        // Create club
        findViewById<Button>(R.id.createClubBtn).setOnClickListener { createClub() }
        findViewById<Button>(R.id.cancelBtn).setOnClickListener { finish() }
    }

    // ---------------------------
    // CAMERA PERMISSIONS
    // ---------------------------
    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this, "Camera permission granted. Tap again.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ---------------------------
    // RESULTS
    // ---------------------------
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) return
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_LOCATION_SELECT -> {
                val lat = data?.getDoubleExtra("lat", 0.0) ?: return
                val lng = data.getDoubleExtra("lng", 0.0)
                selectedLatLng = LatLng(lat, lng)
                Toast.makeText(this, "Location selected!", Toast.LENGTH_SHORT).show()
            }

            REQUEST_IMAGE_PICK -> {
                val uri = data?.data ?: return
                val path = copyToInternal(uri)
                selectedMedia.add("photo" to path)
                addPreview("photo", path)
            }

            REQUEST_VIDEO_PICK -> {
                val uri = data?.data ?: return
                val path = copyToInternal(uri)
                selectedMedia.add("video" to path)
                addPreview("video", path)
            }

            REQUEST_IMAGE_CAPTURE -> {
                val path = tempFile?.absolutePath ?: return
                selectedMedia.add("photo" to path)
                addPreview("photo", path)
            }

            REQUEST_VIDEO_CAPTURE -> {
                val path = tempFile?.absolutePath ?: return
                selectedMedia.add("video" to path)
                addPreview("video", path)
            }
        }
    }

    // ---------------------------
    // MEDIA PICKERS
    // ---------------------------
    private fun openGalleryImage() {
        startActivityForResult(
            Intent(Intent.ACTION_PICK).apply { type = "image/*" },
            REQUEST_IMAGE_PICK
        )
    }

    private fun openGalleryVideo() {
        startActivityForResult(
            Intent(Intent.ACTION_PICK).apply { type = "video/*" },
            REQUEST_VIDEO_PICK
        )
    }

    // ---------------------------
    // CAPTURE PHOTO/VIDEO
    // ---------------------------
    private fun captureImage() {
        tempFile = createTempFile("IMG_", ".jpg")
        tempUri = FileProvider.getUriForFile(this, "${packageName}.provider", tempFile!!)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, tempUri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    private fun captureVideo() {
        tempFile = createTempFile("VID_", ".mp4")
        tempUri = FileProvider.getUriForFile(this, "${packageName}.provider", tempFile!!)
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, tempUri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        startActivityForResult(intent, REQUEST_VIDEO_CAPTURE)
    }

    private fun createTempFile(prefix: String, suffix: String): File {
        val dir = getExternalFilesDir("club_media")
        return File.createTempFile(prefix, suffix, dir)
    }

    private fun copyToInternal(uri: Uri): String {
        val input = contentResolver.openInputStream(uri) ?: return ""
        val dir = getExternalFilesDir("club_media")
        val file = File(dir, "MEDIA_${System.currentTimeMillis()}")
        file.outputStream().use { output -> input.copyTo(output) }
        return file.absolutePath
    }

    // ---------------------------
    // UI PREVIEW
    // ---------------------------
    private fun addPreview(type: String, path: String) {
        val img = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                setMargins(10, 0, 10, 0)
            }

            if (type == "video") {
                setImageResource(android.R.drawable.ic_media_play)
                scaleType = ImageView.ScaleType.CENTER
            } else {
                try {
                    val bmp = BitmapFactory.decodeFile(path)
                    setImageBitmap(bmp)
                } catch (e: Exception) {
                    setImageResource(android.R.drawable.ic_menu_report_image)
                }
            }
        }
        previewRow.addView(img)
    }

    // ---------------------------
    // CREATE CLUB
    // ---------------------------
    private fun createClub() {
        val name = findViewById<EditText>(R.id.clubNameField).text.toString()
        val desc = findViewById<EditText>(R.id.clubDescriptionField).text.toString()
        val sport = findViewById<Spinner>(R.id.sportTypeSpinner).selectedItem.toString()

        if (name.isBlank() || selectedLatLng == null) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val ownerId = sessionManager.getUserId()

        val clubId = repository.createClub(
            name, desc, sport,
            selectedLatLng!!.latitude,
            selectedLatLng!!.longitude,
            ownerId
        )

        selectedMedia.forEach { (type, path) ->
            repository.saveMedia(ownerId, clubId.toInt(), type, path)
        }

        Toast.makeText(this, "Club created!", Toast.LENGTH_SHORT).show()
        finish()
    }
}
