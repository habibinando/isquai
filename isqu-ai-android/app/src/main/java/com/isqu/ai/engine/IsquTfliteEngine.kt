package com.isqu.ai.engine

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object VulkanNativeBridge {
    init {
        System.loadLibrary("isqu_vulkan_bridge")
    }
    external fun probeVulkanGpuDevice(): String
}

data class LoadedModelMeta(
    val displayName: String,
    val sourceOrigin: String,
    val delegateActive: String,
    val vulkanGpuSummary: String
)

class IsquTfliteEngine(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var activeMeta: LoadedModelMeta? = null

    companion object {
        const val MODEL_NAME = "IsQu-50M-Reasoning"
        const val KAGGLE_REF = "hanando/isqu"
        const val VOCAB_SIZE = 8192
        const val MAX_SEQ_LEN = 256
        const val D_MODEL = 512
        const val N_LAYERS = 14
        const val N_HEADS = 8
    }

    suspend fun loadBundledIsquModel(assetName: String = "isqu_50m_fp16.tflite"): LoadedModelMeta =
        withContext(Dispatchers.IO) {
            val afd = context.assets.openFd(assetName)
            FileInputStream(afd.fileDescriptor).channel.use { channel ->
                val mapped = channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
                initInterpreter(mapped, "$MODEL_NAME ($KAGGLE_REF)", "assets://$assetName")
            }
        }

    suspend fun importCustomModel(uri: Uri, displayName: String): LoadedModelMeta =
        withContext(Dispatchers.IO) {
            val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                ?: throw IllegalStateException("Tidak dapat membuka berkas model kustom.")
            pfd.use { descriptor ->
                FileInputStream(descriptor.fileDescriptor).channel.use { channel ->
                    val mapped = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
                    initInterpreter(mapped, displayName, uri.toString())
                }
            }
        }

    private fun initInterpreter(
        modelBuffer: MappedByteBuffer,
        displayName: String,
        sourceOrigin: String
    ): LoadedModelMeta {
        close()
        val vulkanInfo = runCatching { VulkanNativeBridge.probeVulkanGpuDevice() }
            .getOrDefault("VulkanFallback")
        val compatList = CompatibilityList()
        val options = Interpreter.Options()
        var delegateName = "XNNPACK CPU (4 Threads)"

        if (compatList.isDelegateSupportedOnThisDevice) {
            val gpuOptions = compatList.bestOptionsForThisDevice.apply {
                isPrecisionLossAllowed = true // FP16 acceleration
            }
            gpuDelegate = GpuDelegate(gpuOptions)
            options.addDelegate(gpuDelegate)
            delegateName = "TFLite GPU Delegate (FP16)"
        } else {
            options.setUseXNNPACK(true)
            options.setNumThreads(4)
        }

        interpreter = Interpreter(modelBuffer, options)
        return LoadedModelMeta(
            displayName = displayName,
            sourceOrigin = sourceOrigin,
            delegateActive = delegateName,
            vulkanGpuSummary = vulkanInfo
        ).also { activeMeta = it }
    }

    suspend fun runInference(inputTokens: IntArray): Pair<FloatArray, Long> =
        withContext(Dispatchers.Default) {
            val engine = interpreter ?: throw IllegalStateException("Model ISQU belum dimuat.")
            val startNs = System.nanoTime()

            val inputBuffer = ByteBuffer.allocateDirect(inputTokens.size * 4)
                .order(ByteOrder.nativeOrder())
            inputTokens.forEach { inputBuffer.putInt(it) }
            inputBuffer.rewind()

            val outputShape = engine.getOutputTensor(0).shape()
            val outputSize = outputShape.fold(1) { acc, dim -> acc * dim }
            val outputBuffer = Array(1) { FloatArray(outputSize) }

            engine.run(inputBuffer, outputBuffer)
            val latencyMs = (System.nanoTime() - startNs) / 1_000_000L
            outputBuffer[0] to latencyMs
        }

    fun close() {
        interpreter?.close()
        interpreter = null
        gpuDelegate?.close()
        gpuDelegate = null
    }
}
