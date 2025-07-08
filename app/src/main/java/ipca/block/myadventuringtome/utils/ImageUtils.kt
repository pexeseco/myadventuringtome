
package ipca.block.myadventuringtome.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

object LocalImageUtils {

    /**
     * Save image to app's internal storage
     * @param context Application context
     * @param imageUri The image URI from gallery
     * @param folder Folder name (e.g., "characters" or "notes")
     * @return Local file path or null if failed
     */
    fun saveImageLocally(context: Context, imageUri: Uri, folder: String): String? {
        return try {
            // Create folder if it doesn't exist
            val storageDir = File(context.filesDir, "images/$folder")
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }

            // Generate unique filename
            val fileName = "img_${UUID.randomUUID()}.jpg"
            val imageFile = File(storageDir, fileName)

            // Copy and compress image
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)

                // Compress bitmap to reduce size
                val compressedBitmap = compressBitmap(bitmap, 800, 800)

                FileOutputStream(imageFile).use { outputStream ->
                    compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                }

                compressedBitmap.recycle()
                bitmap.recycle()
            }

            imageFile.absolutePath
        } catch (e: Exception) {
            println("Error saving image locally: ${e.message}")
            null
        }
    }

    /**
     * Delete local image file
     * @param imagePath The full local file path
     */
    fun deleteLocalImage(imagePath: String): Boolean {
        return try {
            if (imagePath.isNotBlank()) {
                val file = File(imagePath)
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            println("Error deleting local image: ${e.message}")
            false
        }
    }

    /**
     * Compress bitmap to specified dimensions
     */
    private fun compressBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val aspectRatio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (aspectRatio > 1) {
            newWidth = maxWidth
            newHeight = (maxWidth / aspectRatio).toInt()
        } else {
            newHeight = maxHeight
            newWidth = (maxHeight * aspectRatio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Check if local image file exists
     */
    fun imageExists(imagePath: String): Boolean {
        return try {
            imagePath.isNotBlank() && File(imagePath).exists()
        } catch (e: Exception) {
            false
        }
    }
}