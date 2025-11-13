package com.example.network

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_club)

        repository = DatabaseRepository(this)
        sessionManager = SessionManager(this)
        previewRow = findViewById(R.id.mediaPreviewRow)

        val sports = listOf("Basketball", "Soccer", "Tennis", "Volleyball", "Running", "Badminton")
        val sportSpinner = findViewById<Spinner>(R.id.sportTypeSpinner)
        sportSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            sports
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        findViewById<Button>(R.id.chooseLocationBtn).setOnClickListener {
            startActivityForResult(Intent(this, SelectLocationActivity::class.java), REQUEST_LOCATION_SELECT)
        }

        findViewById<Button>(R.id.uploadImageBtn).setOnClickListener { openGalleryImage() }
        findViewById<Button>(R.id.captureImageBtn).setOnClickListener { captureImage() }
        findViewById<Button>(R.id.uploadVideoBtn).setOnClickListener { openGalleryVideo() }
        findViewById<Button>(R.id.captureVideoBtn).setOnClickListener { captureVideo() }

        findViewById<Button>(R.id.createClubBtn).setOnClickListener { createClub() }
        findViewById<Button>(R.id.cancelBtn).setOnClickListener { finish() }
    }

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
                val copiedPath = copyToInternal(uri)
                selectedMedia.add("photo" to copiedPath)
                addPreview("photo", copiedPath)
            }

            REQUEST_VIDEO_PICK -> {
                val uri = data?.data ?: return
                val copiedPath = copyToInternal(uri)
                selectedMedia.add("video" to copiedPath)
                addPreview("video", copiedPath)
            }


            REQUEST_IMAGE_CAPTURE -> {
                val filePath = tempFile?.absolutePath ?: return
                selectedMedia.add("photo" to filePath)
                addPreview("photo", filePath)
            }

            REQUEST_VIDEO_CAPTURE -> {
                val filePath = tempFile?.absolutePath ?: return
                selectedMedia.add("video" to filePath)
                addPreview("video", filePath)
            }
        }
    }

    private fun openGalleryImage() {
        startActivityForResult(Intent(Intent.ACTION_PICK).apply { type = "image/*" }, REQUEST_IMAGE_PICK)
    }

    private fun openGalleryVideo() {
        startActivityForResult(Intent(Intent.ACTION_PICK).apply { type = "video/*" }, REQUEST_VIDEO_PICK)
    }

    private fun captureImage() {
        tempFile = createTempFile("IMG_", ".jpg")
        tempUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", tempFile!!)
        startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, tempUri)
        }, REQUEST_IMAGE_CAPTURE)
    }

    private fun captureVideo() {
        tempFile = createTempFile("VID_", ".mp4")
        tempUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", tempFile!!)
        startActivityForResult(Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, tempUri)
        }, REQUEST_VIDEO_CAPTURE)
    }

    private fun createTempFile(prefix: String, suffix: String): File {
        val dir = getExternalFilesDir("club_media")
        return File.createTempFile(prefix, suffix, dir)
    }
    private fun copyToInternal(uri: Uri): String {
        val input = contentResolver.openInputStream(uri) ?: return ""
        val storageDir = getExternalFilesDir("club_media")
        val file = File(storageDir, "IMG_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { output -> input.copyTo(output) }
        return file.absolutePath
    }


    private fun addPreview(type: String, uriString: String) {
        val img = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                setMargins(10, 0, 10, 0)
            }

            if (type == "video") {
                setImageResource(android.R.drawable.ic_media_play)
                scaleType = ImageView.ScaleType.CENTER
            } else {
                try {
                    val uri = Uri.parse(uriString)
                    val stream = contentResolver.openInputStream(uri)
                    val bmp = BitmapFactory.decodeStream(stream)
                    setImageBitmap(bmp)
                    stream?.close()
                } catch (e: Exception) {
                    setImageResource(android.R.drawable.ic_menu_report_image)
                }
            }
        }
        previewRow.addView(img)
    }

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
            name,
            desc,
            sport,
            selectedLatLng!!.latitude,
            selectedLatLng!!.longitude,
            ownerId
        )

        selectedMedia.forEach { (type, uriStr) ->
            repository.saveMedia(ownerId, clubId.toInt(), type, uriStr)
        }

        Toast.makeText(this, "Club created!", Toast.LENGTH_SHORT).show()
        finish()
    }
}
