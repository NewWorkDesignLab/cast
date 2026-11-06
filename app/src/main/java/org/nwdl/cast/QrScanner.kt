package org.nwdl.cast

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.RequiresPermission
import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.util.concurrent.Executor
import kotlin.math.abs

/** Looks for QR codes through the headset's passthrough camera while the app is in front. */
class QrScanner(private val context: Context, private val onCode: (String) -> Unit) {
    companion object {
        const val PERMISSION = "horizonos.permission.HEADSET_CAMERA"
        private val CAMERA_SOURCE = CameraCharacteristics.Key("com.meta.extra_metadata.camera_source", Int::class.java)
        private const val DECODE_INTERVAL_MS = 300L
    }

    private val main = Handler(Looper.getMainLooper())
    private var thread: HandlerThread? = null
    private var camera: CameraDevice? = null
    private var reader: ImageReader? = null
    private val decoder = QRCodeReader()
    private var lastDecode = 0L

    /** False if the camera cannot be opened now, e.g. while the app is not in front; try again later. */
    @RequiresPermission(anyOf = [PERMISSION, Manifest.permission.CAMERA])
    fun start(): Boolean {
        if (thread != null) return true
        return try {
            open()
            true
        } catch (e: Exception) {
            stop()
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun open() {
        val manager = context.getSystemService(CameraManager::class.java)
        // Passthrough cameras have camera_source 0; position 0 is the left one
        val id = manager.cameraIdList.firstOrNull { manager.getCameraCharacteristics(it).get(CAMERA_SOURCE) == 0 }
            ?: manager.cameraIdList.first()
        val sizes = manager.getCameraCharacteristics(id).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            .getOutputSizes(ImageFormat.YUV_420_888)
        val size = sizes.minBy { abs(it.width - 1280) }

        val thread = HandlerThread("qr").also { it.start(); thread = it }
        val handler = Handler(thread.looper)
        val reader = ImageReader.newInstance(size.width, size.height, ImageFormat.YUV_420_888, 2).also { reader = it }
        reader.setOnImageAvailableListener({ it.acquireLatestImage()?.use(::decode) }, handler)

        manager.openCamera(id, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) {
                camera = device
                val output = OutputConfiguration(reader.surface)
                device.createCaptureSession(SessionConfiguration(SessionConfiguration.SESSION_REGULAR, listOf(output), Executor { handler.post(it) },
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            val request = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply { addTarget(reader.surface) }.build()
                            runCatching { session.setRepeatingRequest(request, null, handler) }
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) = Unit
                    }))
            }

            override fun onDisconnected(device: CameraDevice) = device.close()
            override fun onError(device: CameraDevice, error: Int) = device.close()
        }, handler)
    }

    fun stop() {
        camera?.close()
        reader?.close()
        thread?.quitSafely()
        camera = null
        reader = null
        thread = null
    }

    // Only the luminance (Y) plane is needed for QR codes
    private fun decode(image: Image) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastDecode < DECODE_INTERVAL_MS) return
        lastDecode = now
        val plane = image.planes[0]
        val data = ByteArray(plane.rowStride * image.height)
        plane.buffer.get(data, 0, minOf(data.size, plane.buffer.remaining()))
        val source = PlanarYUVLuminanceSource(data, plane.rowStride, image.height, 0, 0, image.width, image.height, false)
        try {
            val text = decoder.decode(BinaryBitmap(HybridBinarizer(source))).text
            main.post { onCode(text) }
        } catch (_: ReaderException) {
            // no code in this frame
        } finally {
            decoder.reset()
        }
    }
}
