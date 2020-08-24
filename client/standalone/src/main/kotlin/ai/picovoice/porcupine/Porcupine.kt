/*
    Copyright 2018 Picovoice Inc.
    You may not use this file except in compliance with the license. A copy of the license is
    located in the "LICENSE" file accompanying this source.
    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
    express or implied. See the License for the specific language governing permissions and
    limitations under the License.
*/
package ai.picovoice.porcupine

/**
 * Android binding for Picovoice's wake word detection engine (Porcupine).
 */
class Porcupine {
    companion object {
        init {
            System.loadLibrary("pv_porcupine")
        }
    }

    private var `object`: Long = 0

    /**
     * Constructor.
     *
     * @param modelFilePath   Absolute path to file containing model parameters.
     * @param keywordFilePath Absolute path to keyword model file.
     * @param sensitivity     Sensitivity parameter. A higher sensitivity value lowers miss rate at
     * the cost of increased false alarm rate. A sensitivity value should be
     * within [0, 1].
     * @throws PorcupineException if there is an error while initializing Porcupine.
     */
    constructor(modelFilePath: String, keywordFilePath: String, sensitivity: Float) {
        `object` = try {
            init(modelFilePath, arrayOf(keywordFilePath), floatArrayOf(sensitivity))
        } catch (e: Exception) {
            throw PorcupineException(e)
        }
    }

    /**
     * Constructor.
     *
     * @param modelFilePath    Absolute path to file containing model parameters.
     * @param keywordFilePaths Array of absolute paths to keyword files.
     * @param sensitivities    Array of sensitivity parameters.
     * @throws PorcupineException if there is an error while initializing Porcupine.
     */
    constructor(
            modelFilePath: String,
            keywordFilePaths: Array<String>,
            sensitivities: FloatArray) {
        `object` = try {
            init(modelFilePath, keywordFilePaths, sensitivities)
        } catch (e: Exception) {
            throw PorcupineException(e)
        }
    }

    /**
     * Number of audio samples per frame expected by C library.
     *
     * @return acceptable number of audio samples per frame.
     */
    val frameLength: Int
        external get

    /**
     * Audio sample rate accepted by Porcupine.
     *
     * @return sample rate acceptable by Porcupine.
     */
    val sampleRate: Int
        external get

    /**
     * Monitors incoming audio stream for keywords.
     *
     * @param pcm An array of consecutive audio samples. The number of samples per frame can be
     * attained by calling [.getFrameLength]. The incoming audio needs to have a
     * sample rate equal to [.getSampleRate] and be 16-bit linearly-encoded.
     * Porcupine operates on single-channel audio.
     * @return Index of detected keyword. Indexing is 0-based and according to ordering of keyword
     * files passed to constructor. When no keyword is detected it returns -1.
     * @throws PorcupineException if there is an error while processing the audio sample.
     */
    @Throws(PorcupineException::class)
    fun process(pcm: ShortArray): Int {
        return try {
            process(`object`, pcm)
        } catch (e: Exception) {
            throw PorcupineException(e)
        }
    }

    /**
     * Releases resources acquired.
     */
    fun delete() {
        delete(`object`)
    }

    private external fun init(modelFilePath: String, keywordFilePaths: Array<String>, sensitivities: FloatArray): Long
    private external fun delete(`object`: Long)
    private external fun process(`object`: Long, pcm: ShortArray): Int
}