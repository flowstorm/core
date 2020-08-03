package com.promethist.client.util

import com.promethist.util.LoggerDelegate
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class AEC {

    // Time domain Filters
    internal var dc0 = IIR()    // DC-level running average (IIR highpass)
    internal var dc1 = IIR()
    internal var hp0 = FIR_HP_300Hz()      // 300Hz cut-off Highpass
    internal var Fx = IIR1(PreWhiteTransferFreq)   // pre-whitening Filter for x
    internal var Fe = IIR1(PreWhiteTransferFreq)   // pre-whitening Filter for e
    internal var gain = MICGAIN                      // Mic signal amplify

    // Adrian soft decision DTD (Double Talk Detector)
    internal var dfast = M75dB_PCM
    internal var xfast = M75dB_PCM
    internal var dslow = M80dB_PCM
    internal var xslow = M80dB_PCM

    // Geigel DTD (Double Talk Detector)
    internal var max_max_x = 0.0                     // max(|x[0]|, .. |x[L-1]|)
    internal var hangover = 0
    internal var max_x = DoubleArray(NLMS_LEN / DTD_LEN)// optimize: less calculations for max()
    internal var dtdCnt = 0
    internal var dtdNdx = 0

    // NLMS-pw
    internal var x = DoubleArray(NLMS_LEN + NLMS_EXT) // tap delayed loudspeaker signal
    internal var xf = DoubleArray(NLMS_LEN + NLMS_EXT)// pre-whitening tap delayed signal
    internal var w = DoubleArray(NLMS_LEN)          // tap weights
    internal var j = NLMS_EXT                           // optimize: less memory copies
    internal var dotp_xf_xf = 0.0                    // optimize: iterative dotp(x,x)
    internal var delta = 0.0                         // noise floor to stabilize NLMS
    /* Below this line there are no more design constants */

    /* Exponential Smoothing or IIR Infinite Impulse Response Filter */
    class IIR internal constructor() {
        internal var lowpassf: Double = 0.toDouble()

        init {
            lowpassf = 0.0
        }

        internal fun highpass(`in`: Double): Double {
            lowpassf += IIR.Companion.ALPHADC * (`in` - lowpassf)
            return `in` - lowpassf
        }

        companion object {
            internal val ALPHADC = 0.01  /* controls Transfer Frequency */
        }
    }

    class IIR6 {

        var lowpassf: DoubleArray
        var highpassf: DoubleArray

        init {
            lowpassf = DoubleArray(2 * IIR6.Companion.POL + 1)
            highpassf = DoubleArray(2 * IIR6.Companion.POL + 1)
            for (i in 0 until 2 * IIR6.Companion.POL + 1) {
                lowpassf[i] = 0.0
                highpassf[i] = 0.0
            }
        }

        fun highpass(`in`: Double): Double {
            /* Highpass = Signal - Lowpass. Lowpass = Exponential Smoothing */
            highpassf[0] = `in`
            for (i in 0 until 2 * IIR6.Companion.POL) {
                lowpassf[i + 1] += IIR6.Companion.AlphaHp * (highpassf[i] - lowpassf[i + 1])
                highpassf[i + 1] = highpassf[i] - lowpassf[i + 1]
            }
            return IIR6.Companion.Gain6 * highpassf[2 * IIR6.Companion.POL]
        }

        fun lowpass(`in`: Double): Double {
            /* Lowpass = Exponential Smoothing */
            lowpassf[0] = `in`
            for (i in 0 until 2 * IIR6.Companion.POL) {
                lowpassf[i + 1] += IIR6.Companion.AlphaLp * (lowpassf[i] - lowpassf[i + 1])
            }
            return lowpassf[2 * IIR6.Companion.POL]
        }

        companion object {
            val POL = 6      /* -6dB attenuation per octave per Pol */
            val AlphaHp = 0.075 /* controls Transfer Frequence */
            val AlphaLp = 0.15 /* controls Transfer Frequence */
            val Gain6 = 1.45  /* gain to undo filter attenuation */
        }
    }

    /* Recursive single pole IIR Infinite Impulse response High-pass filter
     *
     * Reference: The Scientist and Engineer's Guide to Digital Processing
     *
     *  output[N] = A0 * input[N] + A1 * input[N-1] + B1 * output[N-1]
     *
     *      X  = exp(-2.0 * pi * Fc)
     *      A0 = (1 + X) / 2
     *      A1 = -(1 + X) / 2
     *      B1 = X
     *      Fc = cutoff freq / sample rate
     */
    internal inner class IIR1(freq: Double) {
        var a0: Double = 0.toDouble()
        var a1: Double = 0.toDouble()
        var b1: Double = 0.toDouble()
        var last_in: Double = 0.toDouble()
        var last_out: Double = 0.toDouble()

        init {
            val x = Math.exp(-2.0 * Math.PI * freq / Rate)

            a0 = (1.0 + x) / 2.0
            a1 = -(1.0 + x) / 2.0
            b1 = x
            last_in = 0.0
            last_out = 0.0
        }

        fun highpass(`in`: Double): Double {
            val out = a0 * `in` + a1 * last_in + b1 * last_out
            last_in = `in`
            last_out = out
            return out
        }
    }

    /* Recursive two pole IIR Infinite Impulse Response filter
     * Coefficients calculated with
     * http://www.dsptutor.freeuk.com/IIRFilterDesign/IIRFiltDes102.html
     */
    internal inner class IIR2 {
        val a = doubleArrayOf(0.29289323, -0.58578646, 0.29289323)
        val b = doubleArrayOf(1.3007072E-16, 0.17157288)
        var x: DoubleArray
        var y: DoubleArray

        init {
            x = DoubleArray(2)
            y = DoubleArray(2)
        }

        fun highpass(`in`: Double): Double {
            // Butterworth IIR filter, Filter type: HP
            // Passband: 2000 - 4000.0 Hz, Order: 2
            val out = a[0] * `in` + a[1] * x[0] + a[2] * x[1] - b[0] * y[0] - b[1] * y[1]

            x[1] = x[0]
            x[0] = `in`
            y[1] = y[0]
            y[0] = out
            return out
        }
    }


    /* 17 taps FIR Finite Impulse Response filter
     * Coefficients calculated with
     * www.dsptutor.freeuk.com/KaiserFilterDesign/KaiserFilterDesign.html
     */
    internal inner class FIR_HP_300Hz {
        val a = doubleArrayOf(
                // Kaiser Window FIR Filter, Filter type: High pass
                // Passband: 300.0 - 4000.0 Hz, Order: 16
                // Transition band: 75.0 Hz, Stopband attenuation: 10.0 dB
                -0.034870606, -0.039650206, -0.044063766, -0.04800318, -0.051370874, -0.054082647, -0.056070227, -0.057283327, 0.8214126, -0.057283327, -0.056070227, -0.054082647, -0.051370874, -0.04800318, -0.044063766, -0.039650206, -0.034870606, 0.0)
        var z: DoubleArray

        init {
            z = DoubleArray(18)
        }

        fun highpass(`in`: Double): Double {
            System.arraycopy(z, 0, z, 1, 17)
            z[0] = `in`
            var sum0 = 0.0
            var sum1 = 0.0
            var j: Int

            j = 0
            while (j < 18) {
                // optimize: partial loop unrolling
                sum0 += a[j] * z[j]
                sum1 += a[j + 1] * z[j + 1]
                j += 2
            }
            return sum0 + sum1
        }
    }

    internal fun setambient(Min_xf: Double) {
        dotp_xf_xf -= delta
        delta = (NLMS_LEN - 1).toDouble() * Min_xf * Min_xf
        dotp_xf_xf += delta  // add new delta
    }

    internal fun setgain(gain: Double) {
        this.gain = gain
    }

    /* Normalized Least Mean Square Algorithm pre-whitening (NLMS-pw)
     * The LMS algorithm was developed by Bernard Widrow
     * book: Widrow/Stearns, Adaptive Signal Processing, Prentice-Hall, 1985
     * book: Haykin, Adaptive Filter Theory, 4. edition, Prentice Hall, 2002
     *
     * in mic: microphone sample (PCM as floating point value)
     * in spk: loudspeaker sample (PCM as floating point value)
     * in stepsize: NLMS adaptation variable
     * return: echo cancelled microphone sample
     */
    fun nlms_pw(mic: Double, spk: Double, stepsize: Double): Double {

        x[j] = spk
        xf[j] = Fx.highpass(spk)       // pre-whitening of x

        // calculate error value (mic signal - estimated mic signal from spk signal)
        var e = mic
        if (hangover > 0) {
            e -= dotp(w, 0, x, j, NLMS_LEN)
        }

        val ef = Fe.highpass(e)    // pre-whitening of e

        dotp_xf_xf += xf[j] * xf[j] - xf[j + NLMS_LEN - 1] * xf[j + NLMS_LEN - 1]

        if (stepsize > 0.0) {
            // calculate variable step size
            val mikro_ef = stepsize * ef / dotp_xf_xf

            // update tap weights (filter learning)
            var i = 0
            while (i < NLMS_LEN) {
                // optimize: partial loop unrolling
                w[i] += mikro_ef * xf[i + j]
                w[i + 1] += mikro_ef * xf[i + j + 1]
                i += 2
            }
        }

        if (--j < 0) {
            // optimize: decrease number of memory copies
            j = NLMS_EXT
            System.arraycopy(x, 0, x, j + 1, NLMS_LEN - 1)
            System.arraycopy(xf, 0, xf, j + 1, NLMS_LEN - 1)
        }

        // Saturation
        return if (e > MAXPCM) {
            MAXPCM
        } else if (e < -MAXPCM) {
            -MAXPCM
        } else {
            e
        }
    }

    /* Geigel Double-Talk Detector
     *
     * in d: microphone sample (PCM as doubleing point value)
     * in x: loudspeaker sample (PCM as doubleing point value)
     * return: false for no talking, true for talking
     */
    fun gdtd(d: Double, x: Double): Boolean {
        var x = x
        // optimized implementation of max(|x[0]|, |x[1]|, .., |x[L-1]|):
        // calculate max of block (DTD_LEN values)
        x = Math.abs(x)
        if (x > max_x[dtdNdx]) {
            max_x[dtdNdx] = x
            if (x > max_max_x) {
                max_max_x = x
            }
        }
        if (++dtdCnt >= DTD_LEN) {
            dtdCnt = 0
            // calculate max of max
            max_max_x = 0.0
            for (i in 0 until NLMS_LEN / DTD_LEN) {
                if (max_x[i] > max_max_x) {
                    max_max_x = max_x[i]
                }
            }
            // rotate Ndx
            if (++dtdNdx >= NLMS_LEN / DTD_LEN) dtdNdx = 0
            max_x[dtdNdx] = 0.0
        }

        // The Geigel DTD algorithm with Hangover timer Thold
        if (Math.abs(d) >= GeigelThreshold * max_max_x) {
            hangover = Thold
        }

        if (hangover > 0) --hangover

        return if (max_max_x < UpdateThreshold) {
            // avoid update with silence
            true
        } else {
            hangover > 0
        }
    }

    /*
     * Adrian soft decision DTD
     * (Dual Average Near-End to Far-End signal Ratio DTD)
     * This algorithm uses exponential smoothing with differnt
     * ageing parameters to get fast and slow near-end and far-end
     * signal averages. The ratio of NFRs term
     * (dfast / xfast) / (dslow / xslow) is used to compute the stepsize
     * A ratio value of 2.5 is mapped to stepsize 0, a ratio of 0 is
     * mapped to 1.0 with a limited linear function.
     */
    internal fun adtd(d: Double, x: Double): Double {
        val stepsize: Double

        // fast near-end and far-end average
        dfast += ALPHAFAST * (Math.abs(d) - dfast)
        xfast += ALPHAFAST * (Math.abs(x) - xfast)

        // slow near-end and far-end average
        dslow += ALPHASLOW * (Math.abs(d) - dslow)
        xslow += ALPHASLOW * (Math.abs(x) - xslow)

        if (xfast < M70dB_PCM) {
            return 0.0   // no Spk signal
        }

        if (dfast < M70dB_PCM) {
            return 0.0   // no Mic signal
        }

        // ratio of NFRs
        val ratio = dfast * xslow / (dslow * xfast)

        // begrenzte lineare Kennlinie
        val M = (STEPY2 - STEPY1) / (STEPX2 - STEPX1)
        if (ratio < STEPX1) {
            stepsize = STEPY1
        } else if (ratio > STEPX2) {
            stepsize = STEPY2
        } else {
            // Punktrichtungsform einer Geraden
            stepsize = M * (ratio - STEPX1) + STEPY1
        }

        return stepsize
    }


    // The xfast signal is used to charge the hangover timer to Thold.
    // When hangover expires (no Spk signal for some time) the vector w
    // is erased. This is Adrian implementation of Leaky NLMS.
    internal fun leaky() {
        if (xfast >= M70dB_PCM) {
            // vector w is valid for hangover Thold time
            hangover = Thold
        } else {
            if (hangover > 1) {
                --hangover
            } else if (1 == hangover) {
                --hangover
                // My Leaky NLMS is to erase vector w when hangover expires
                w = DoubleArray(NLMS_LEN)
            }
        }
    }

    /* Acoustic Echo Cancellation and Suppression of one sample
     * in   s0: microphone signal with echo
     * in   s1: loudspeaker signal
     * return:  echo cancelled microphone signal
     */
    fun doAEC(y: Int, x: Int): Int {
        var s0 = y.toDouble()
        var s1 = x.toDouble()

        // Mic and Spk signal remove DC (IIR highpass filter)
        s0 = dc0.highpass(s0)
        s1 = dc1.highpass(s1)

        // Mic Highpass Filter - telephone users are used to 300Hz cut-off
        s0 = hp0.highpass(s0)

        // Amplify, for e.g. Soundcards with -6dB max. volume
        s0 *= gain

        // Double Talk Detector
        val stepsize = adtd(s0, s1)

        // Leaky (ageing of vector w)
        leaky()

        // Acoustic Echo Cancellation
        s0 = nlms_pw(s0, s1, stepsize)

        return s0.toInt()
    }

    /* Acoustic Echo Cancellation and Suppression of frame
     * in/out   ybuff: microphone signal with echo
     * in       xbuff: loudspeaker signal
     * return:  echo cancelled microphone signal
     */

    fun process(ybuff: ByteArray, xbuff: ByteArray): ByteArray {
        val size = if (ybuff.size > xbuff.size) xbuff.size else ybuff.size
        val ebuff = ByteArray(size)

/*
        ByteBuffer xbb = ByteBuffer.wrap(xbuff);
        ByteBuffer ybb = ByteBuffer.wrap(ybuff);
        ByteBuffer o = ByteBuffer.allocate(ybuff.length);
        for (int i = 0; i< ybuff.length; i+=2){
            int y = ybb.getShort(i);
            int x = xbb.getShort(i);
            int e = doAEC(y,x);
            o.putShort(i,(short)e);
        }
        return o.array();
        */

        var i = 0
        while (i < size) {
            // If there's any way of doing this faster/better...

            val y = (ybuff[i].toInt() shl 8) + (ybuff[i + 1].toInt() and 0xFF)
            val x = (xbuff[i].toInt() shl 8) + (xbuff[i + 1].toInt() and 0xFF)
            val e = doAEC(y, x)
            ebuff[i] = (e shr 8).toByte()
            ebuff[i + 1] = (e and 0xff).toByte()
            i += 2
        }
        return ebuff
    }

    /**
     * writeSample
     *
     * @param buff byte[]
     */
    fun writeSample(buff: ByteArray, buff2: ByteArray, sno: Int) {
        val fname = "$sno.raw"
        try {
            val s = FileOutputStream(fname)
            s.write(buff)
            s.write(buff2)
            s.close()
        } catch (ex: IOException) {
            logger.warn(ex.message)
        }

    }

    companion object {
        /* dB Values */
        val M0dB = 1.00
        val M3dB = 0.71
        val M6dB = 0.50
        val M9dB = 0.35
        val M12dB = 0.25
        val M18dB = 0.125
        val M24dB = 0.063

        /* dB values for 16bit PCM */
        val M10dB_PCM = 10362.0
        val M20dB_PCM = 3277.0
        val M25dB_PCM = 1843.0
        val M30dB_PCM = 1026.0
        val M35dB_PCM = 583.0
        val M40dB_PCM = 328.0
        val M45dB_PCM = 184.0
        val M50dB_PCM = 104.0
        val M55dB_PCM = 58.0
        val M60dB_PCM = 33.0
        val M65dB_PCM = 18.0
        val M70dB_PCM = 10.0
        val M75dB_PCM = 6.0
        val M80dB_PCM = 3.0
        val M85dB_PCM = 2.0
        val M90dB_PCM = 1.0

        val MAXPCM = 32767.0

        /* Design constants (Change to fine tune the algorithms */

        /* NLMS filter length in taps (samples). A longer filter length gives
     * better Echo Cancellation, but maybe slower convergence speed and
     * needs more CPU power (Order of NLMS is linear) */
        val NLMS_LEN = 100 * 8

        /* minimum energy in xf. Range: M70dB_PCM to M50dB_PCM. Should be equal
     * to microphone ambient Noise level */
        val NoiseFloor = M55dB_PCM

        /* Initial MIC Gain, 1 = direct */
        val MICGAIN = 1.0

        /* Leaky hangover in taps.
     */
        val Thold = 60 * 8

        val PreWhiteTransferFreq = 2000.0
        val Rate = 8000.0

        // Adrian soft decision DTD
        // left point. X is ratio, Y is stepsize
        val STEPX1 = 1.0
        val STEPY1 = 1.0
        // right point. STEPX2=2.0 is good double talk, 3.0 is good single talk.
        val STEPX2 = 2.5
        val STEPY2 = 0.0
        val ALPHAFAST = 1.0 / 100.0
        val ALPHASLOW = 1.0 / 20000.0

        /* Ageing multiplier for LMS memory vector w */
        val Leaky = 0.9999

        /* Double Talk Detector Speaker/Microphone Threshold. Range <=1
     * Large value (M0dB) is good for Single-Talk Echo cancellation,
     * small value (M12dB) is good for Doulbe-Talk AEC */
        val GeigelThreshold = M6dB
        val UpdateThreshold = M50dB_PCM

        /* for Non Linear Processor. Range >0 to 1. Large value (M0dB) is good
     * for Double-Talk, small value (M12dB) is good for Single-Talk */
        val NLPAttenuation = M12dB


        private val logger by LoggerDelegate()

        /* Vector Dot Product */
        fun dotp(a: DoubleArray, oa: Int, b: DoubleArray, ob: Int, l: Int): Double {
            var sum0 = 0.0
            var sum1 = 0.0

            var i = 0
            while (i < l) {
                // optimize: partial loop unrolling
                sum0 += a[i + oa] * b[i + ob]
                sum1 += a[i + oa + 1] * b[i + ob + 1]
                i += 2
            }
            return sum0 + sum1
        }

        internal val NLMS_EXT = 10 * 8     // Extention in taps to optimize mem copies
        internal val DTD_LEN = 16          // block size in taps to optimize DTD calculation

        @JvmStatic
        fun main(argv: Array<String>) {
            val aec = AEC()
            var i = 0
            val head = ByteArray(24)
            val buffx = ByteArray(320)
            val buffy = ByteArray(320)
            var buffe: ByteArray
            ///Log.setLevel(Log.PROL);
            try {
                val fx = FileInputStream(argv[0])
                val fy = FileInputStream(argv[1])
                val fe = FileOutputStream(argv[2])
                // Skip over au header, and pass it to output.
                fx.read(head)
                fy.read(head)
                fe.write(head)

                val then = System.currentTimeMillis()
                while (fx.read(buffx) == 320) {
                    fy.read(buffy)
                    buffe = aec.process(buffy, buffx)
                    fe.write(buffe)
                    i++
                }
                val now = System.currentTimeMillis()
                logger.warn("AEC took " + (now - then) + " for " + i + " samples.")

                fx.close()
                fy.close()
                fe.close()

            } catch (ex: IOException) {
                logger.error(ex.message, ex)
            }

        }
    }
}


