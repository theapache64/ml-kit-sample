package com.theapache64.mlkit.sample

import android.app.Activity
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.SparseIntArray
import android.view.Surface
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.common.FirebaseVisionPoint
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.SizeSelector
import com.otaliastudios.cameraview.SizeSelectors
import com.theapache64.twinkill.logger.info
import com.theapache64.twinkill.logger.mistake


class MainActivity : AppCompatActivity() {

    private var vRed: View? = null
    private val cameraManager by lazy {
        getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private val visionOptions by lazy {
        FirebaseVisionFaceDetectorOptions.Builder()
            .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
            .build()
    }

    val detector by lazy {
        FirebaseVision.getInstance()
            .getVisionFaceDetector(visionOptions)
    }
    private val ORIENTATIONS = SparseIntArray()

    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }

    /**
     * Get the angle by which an image must be rotated given the device's current
     * orientation.
     */
    @Throws(CameraAccessException::class)
    private fun getRotationCompensation(
        cameraId: String,
        activity: Activity
    ): Int {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        val deviceRotation = activity.windowManager.defaultDisplay.rotation
        var rotationCompensation = ORIENTATIONS.get(deviceRotation)

        // On most devices, the sensor orientation is 90 degrees, but for some
        // devices it is 270 degrees. For devices with a sensor orientation of
        // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.
        val cameraManager = activity.getSystemService(CAMERA_SERVICE) as CameraManager
        val sensorOrientation = cameraManager
            .getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360

        // Return the corresponding FirebaseVisionImageMetadata rotation value.
        val result: Int
        when (rotationCompensation) {
            0 -> result = FirebaseVisionImageMetadata.ROTATION_0
            90 -> result = FirebaseVisionImageMetadata.ROTATION_90
            180 -> result = FirebaseVisionImageMetadata.ROTATION_180
            270 -> result = FirebaseVisionImageMetadata.ROTATION_270
            else -> {
                result = FirebaseVisionImageMetadata.ROTATION_0
                info("Bad rotation value: $rotationCompensation")
            }
        }
        return result
    }

    private fun getFrontFacingCameraId(): String {
        for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING)!!
            if (cOrientation == CameraCharacteristics.LENS_FACING_FRONT) {
                return cameraId
            }
        }
        throw IllegalArgumentException("Failed to find cameraId")
    }


    private fun getSize(): SizeSelector {
        val height = SizeSelectors.minHeight(360)
        val sizeSelector = SizeSelectors.smallest()
        return SizeSelectors.and(height, sizeSelector)
    }

    private val frontCameraId by lazy {
        getFrontFacingCameraId()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vRed = findViewById<View>(R.id.v_red)
        val cameraView = findViewById<CameraView>(R.id.camera)

        cameraView.setLifecycleOwner(this)
        cameraView.setPreviewStreamSize(getSize())
        cameraView.addFrameProcessor { frame ->

            val rotation =
                getRotationCompensation(frontCameraId, this@MainActivity)

            val metadata = FirebaseVisionImageMetadata.Builder()
                .setWidth(frame.size.width) // 480x360 is typically sufficient for
                .setHeight(frame.size.height) // image recognition
                .setFormat(frame.format)
                .setRotation(rotation)
                .build()

            val image = FirebaseVisionImage.fromByteArray(
                frame.data,
                metadata
            )

            detectFrom(image)
        }
    }


    private fun detectFrom(image: FirebaseVisionImage) {
        detector.detectInImage(image)
            .addOnSuccessListener { faces ->

                for (face in faces) {

                    val bounds = face.boundingBox
                    val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
                    val rotZ = face.headEulerAngleZ // Head is tilted sideways rotZ degrees

                    val upperLipTop = face.getContour(FirebaseVisionFaceContour.UPPER_LIP_TOP)
                    val lowerLipBottom = face.getContour(FirebaseVisionFaceContour.LOWER_LIP_BOTTOM)

                    val lipDistance = getDistance(upperLipTop, lowerLipBottom)
                    info("Lip distance $lipDistance")
                    if (lipDistance > 50) {

                        // Check eye diff
                        val leftEye = face.getContour(FirebaseVisionFaceContour.LEFT_EYE)
                        val leftEyeTop = leftEye.points[4]
                        val leftEyeBottom = leftEye.points[12]
                        val leftEyeDistance = getDistance(leftEyeTop, leftEyeBottom)


                        val rightEye = face.getContour(FirebaseVisionFaceContour.RIGHT_EYE)
                        val rightEyeTop = rightEye.points[4]
                        val rightEyeBottom = rightEye.points[12]
                        val rightEyeDistance = getDistance(rightEyeTop, rightEyeBottom)

                        info("Eye distance : $leftEyeDistance : $rightEyeDistance")

                        if (leftEyeDistance < 10 && rightEyeDistance < 10) {
                            info("Yawning...")
                            showRed()
                            sendMessage("Stop yawning!!")
                        } else {
                            hideRed()
                        }

                    } else {
                        hideRed()
                    }

                    // Check eye diff
                    val leftEye = face.getContour(FirebaseVisionFaceContour.LEFT_EYE)
                    val leftEyeTop = leftEye.points[4]
                    val leftEyeBottom = leftEye.points[12]
                    val leftEyeDistance = getDistance(leftEyeTop, leftEyeBottom)


                    val rightEye = face.getContour(FirebaseVisionFaceContour.RIGHT_EYE)
                    val rightEyeTop = rightEye.points[4]
                    val rightEyeBottom = rightEye.points[12]
                    val rightEyeDistance = getDistance(rightEyeTop, rightEyeBottom)

                    info("Eye distance : $leftEyeDistance : $rightEyeDistance")

                    if (leftEyeDistance <= 6 && rightEyeDistance <= 6) {
                        if (eyeClosedTime == -1L) {
                            info("Sleeping tracking started")
                            eyeClosedTime = System.currentTimeMillis()
                        }

                        if (eyeClosedTime != -1L) {
                            // tracking active
                            val diff = System.currentTimeMillis() - eyeClosedTime
                            if (diff > 2000) {
                                info("Sleeping...")
                                showRed()
                                sendMessage("Stop sleeping!!")
                            }
                        }


                    } else {
                        hideRed()
                        if (eyeClosedTime != -1L) {
                            info("Sleeping tracking stopped")
                            eyeClosedTime = -1
                        }
                    }

                }

            }
            .addOnFailureListener {
                mistake("Something went wrong! ${it.message}")
            }
    }

    private var eyeClosedTime: Long = -1

    private fun sendMessage(message: String) {
        /*MokoSupport.getInstance().sendOrder(
            ZWriteCommonMessageTask(
                object : MokoOrderTaskCallback {
                    override fun onOrderResult(response: OrderTaskResponse?) {
                    }

                    override fun onOrderTimeout(response: OrderTaskResponse?) {
                    }

                    override fun onOrderFinish() {
                    }
                },
                false,
                "ALERT!\n$message",
                true
            )
        )*/
    }

    private fun showRed() {
        runOnUiThread { vRed!!.visibility = View.VISIBLE }
    }

    private fun hideRed() {
        runOnUiThread {
            vRed!!.visibility = View.GONE
        }
    }


    private fun getDistance(
        y1: FirebaseVisionPoint,
        y2: FirebaseVisionPoint
    ): Float {
        return y2.y - y1.y
    }

    private fun getDistance(
        x: FirebaseVisionFaceContour,
        y: FirebaseVisionFaceContour
    ): Float {
        val x1 = x.points[4]
        val x2 = x.points[5]
        val y1 = y.points[4]
        val y2 = y.points[5]

        val x1n = x1.x + ((x2.x - x1.x) / 2)
        val x2n = y1.x + ((y2.x - y1.x) / 2)

        val xn = FirebaseVisionPoint(x1n, x1.y, null)
        val yn = FirebaseVisionPoint(x2n, y1.y, null)

        return yn.y - xn.y
    }


}
