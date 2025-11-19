package com.example.network

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.network.database.DatabaseRepository
import com.example.network.utils.SessionManager
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.widget.TextView
import android.app.Dialog
import android.view.LayoutInflater

class EditProfileActivity : AppCompatActivity() {

    private lateinit var repository: DatabaseRepository
    private lateinit var sessionManager: SessionManager

    private lateinit var profileImage: ImageView
    private lateinit var editName: TextInputEditText
    private lateinit var editBio: TextInputEditText
    private lateinit var editLocation: TextInputEditText
    private lateinit var saveBtn: Button
    private lateinit var cancelBtn: Button
    private lateinit var changePhotoBtn: com.google.android.material.floatingactionbutton.FloatingActionButton

    private lateinit var changePasswordBtn: Button

    private var userId: Int = -1
    private var selectedImagePath: String? = null
    private var tempCameraPhotoUri: Uri? = null

    // Activity result launchers
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleImageSelection(uri)
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            tempCameraPhotoUri?.let { uri ->
                handleImageSelection(uri)
            }
        }
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showImageSourceDialog()
        } else {
            Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        sessionManager = SessionManager(this)
        userId = sessionManager.getUserId()

        if (userId == -1) {
            Toast.makeText(this, "User session not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        repository = DatabaseRepository(this)
        bindViews()
        setupClickListeners()
        loadUserData()
    }

    private fun bindViews() {
        profileImage = findViewById(R.id.profileImage)
        editName = findViewById(R.id.editName)
        editBio = findViewById(R.id.editBio)
        editLocation = findViewById(R.id.editLocation)
        saveBtn = findViewById(R.id.saveBtn)
        cancelBtn = findViewById(R.id.cancelBtn)
        changePhotoBtn = findViewById(R.id.changePhotoBtn)
        changePasswordBtn = findViewById(R.id.changePasswordBtn)
    }

    private fun setupClickListeners() {
        saveBtn.setOnClickListener {
            saveProfile()
        }

        cancelBtn.setOnClickListener {
            finish()
        }

        changePhotoBtn.setOnClickListener {
            checkPermissionAndShowDialog()
        }

        profileImage.setOnClickListener {
            checkPermissionAndShowDialog()
        }

        changePasswordBtn.setOnClickListener {
            showChangePasswordDialog()
        }
    }

    private fun checkPermissionAndShowDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - Check for READ_MEDIA_IMAGES
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED -> {
                    showImageSourceDialog()
                }
                else -> {
                    storagePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }
        } else {
            // Below Android 13 - Check for READ_EXTERNAL_STORAGE
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    showImageSourceDialog()
                }
                else -> {
                    storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        AlertDialog.Builder(this)
            .setTitle("Change Profile Picture")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun openCamera() {
        // Check camera permission first
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        try {
            val photoFile = createImageFile()
            if (photoFile == null) {
                Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show()
                return
            }

            tempCameraPhotoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, tempCameraPhotoUri)

            // Grant permission to camera app
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

            cameraLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Camera error: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun createImageFile(): File? {
        return try {
            val storageDir = getExternalFilesDir("profile_images")
            storageDir?.mkdirs()
            File.createTempFile(
                "profile_${userId}_${System.currentTimeMillis()}",
                ".jpg",
                storageDir
            )
        } catch (e: IOException) {
            Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun handleImageSelection(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                // Save the bitmap to internal storage
                val savedPath = saveBitmapToInternalStorage(bitmap)
                if (savedPath != null) {
                    selectedImagePath = savedPath
                    profileImage.setImageBitmap(bitmap)
                    Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveBitmapToInternalStorage(bitmap: Bitmap): String? {
        return try {
            val directory = File(filesDir, "profile_images")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val filename = "profile_${userId}_${System.currentTimeMillis()}.jpg"
            val file = File(directory, filename)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun loadUserData() {
        Thread {
            val user = repository.getUserById(userId)
            val profileImagePath = repository.getUserProfileImage(userId)

            runOnUiThread {
                if (user != null) {
                    editName.setText(user.name)
                    editBio.setText(user.bio ?: "")
                    editLocation.setText(user.location ?: "")

                    // Load profile image
                    if (!profileImagePath.isNullOrEmpty()) {
                        val bitmap = BitmapFactory.decodeFile(profileImagePath)
                        if (bitmap != null) {
                            profileImage.setImageBitmap(bitmap)
                        } else {
                            profileImage.setImageResource(R.drawable.ic_profile_placeholder)
                        }
                    } else {
                        profileImage.setImageResource(R.drawable.ic_profile_placeholder)
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Failed to load user data",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }.start()
    }

    private fun showChangePasswordDialog() {
        val dialog = Dialog(this)
        // **NOTE**: You must create the layout resource file 'dialog_change_password' (Step 3)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
        dialog.setContentView(dialogView)

        val currentPasswordField = dialogView.findViewById<TextInputEditText>(R.id.dialogCurrentPassword)
        val newPasswordField = dialogView.findViewById<TextInputEditText>(R.id.dialogNewPassword)
        val confirmNewPasswordField = dialogView.findViewById<TextInputEditText>(R.id.dialogConfirmNewPassword)
        val saveNewPasswordBtn = dialogView.findViewById<Button>(R.id.dialogSavePasswordBtn)
        val cancelDialogBtn = dialogView.findViewById<Button>(R.id.dialogCancelBtn)

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        saveNewPasswordBtn.setOnClickListener {
            val currentPassword = currentPasswordField.text.toString()
            val newPassword = newPasswordField.text.toString()
            val confirmNewPassword = confirmNewPasswordField.text.toString()

            // Client-Side Validation (simplified, use better validation methods)
            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPassword.length < 6) {
                newPasswordField.error = "Must be 6+ chars"
                return@setOnClickListener
            }
            if (newPassword != confirmNewPassword) {
                confirmNewPasswordField.error = "Passwords do not match"
                return@setOnClickListener
            }
            if (currentPassword == newPassword) {
                newPasswordField.error = "New password must be different"
                return@setOnClickListener
            }

            saveNewPasswordBtn.isEnabled = false

            // Perform password change on a background thread
            Thread {
                // 1. Verify current password
                val isCurrentPasswordValid = repository.verifyCurrentPassword(userId, currentPassword)

                runOnUiThread {
                    if (isCurrentPasswordValid) {
                        // 2. Update password
                        Thread {
                            val updateSuccess = repository.updateUserPassword(userId, newPassword)

                            runOnUiThread {
                                saveNewPasswordBtn.isEnabled = true
                                dialog.dismiss()

                                if (updateSuccess) {
                                    Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(this, "Failed to update password. Try again.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }.start()
                    } else {
                        // Current password incorrect
                        saveNewPasswordBtn.isEnabled = true
                        currentPasswordField.error = "Incorrect current password"
                        Toast.makeText(this, "Incorrect current password", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }

        cancelDialogBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveProfile() {
        val name = editName.text.toString().trim()
        val bio = editBio.text.toString().trim()
        val location = editLocation.text.toString().trim()

        // Validation
        if (name.isEmpty()) {
            editName.error = "Name cannot be empty"
            editName.requestFocus()
            return
        }

        // Disable button to prevent multiple clicks
        saveBtn.isEnabled = false

        Thread {
            val success = repository.updateUserProfile(
                userId = userId,
                name = name,
                bio = bio.ifEmpty { null },
                location = location.ifEmpty { null },
                preferences = null
            )

            // Update profile image if a new one was selected
            val imageUpdateSuccess = if (selectedImagePath != null) {
                repository.updateUserProfileImage(userId, selectedImagePath!!)
            } else {
                true
            }

            runOnUiThread {
                saveBtn.isEnabled = true

                if (success && imageUpdateSuccess) {
                    Toast.makeText(
                        this,
                        "Profile updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish() // Return to ProfileActivity
                } else {
                    Toast.makeText(
                        this,
                        "Failed to update profile",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    override fun onDestroy() {
        if (::repository.isInitialized) {
            repository.close()
        }
        super.onDestroy()
    }
}