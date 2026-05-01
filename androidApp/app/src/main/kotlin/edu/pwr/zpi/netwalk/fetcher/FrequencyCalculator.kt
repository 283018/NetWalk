package edu.pwr.zpi.netwalk.fetcher

object FrequencyCalculator {
    /**
     Dane pasm LTE: F_DL_Low, N_Offs_DL, F_UL_Low, N_Offs_UL
     */
    private data class LteBandParams(
        val fDlLow: Double,
        val nOffsDl: Int,
        val fUlLow: Double,
        val nOffsUl: Int,
    )

    //  Bierzemy pasma używane w Polsce
    private val lteBandMap = mapOf(
        "1" to LteBandParams(2110.0, 0, 1920.0, 18000),
        "3" to LteBandParams(1805.0, 1200, 1710.0, 19200),
        "7" to LteBandParams(2620.0, 2750, 2500.0, 20750),
        "8" to LteBandParams(942.5, 3450, 897.5, 21450),
        "20" to LteBandParams(791.0, 6150, 832.0, 24150),
        "38" to LteBandParams(2570.0, 37750, 2570.0, 37750), // TDD
        "40" to LteBandParams(2300.0, 38650, 2300.0, 38650), // TDD
    )

    /**
     Oblicza częstotliwości downlink i uplink LTE na podstawie EARFCN i numeru pasma.
     Wzór: F = F_low + 0.1 * (N - N_Offs)
     */
    fun calculateLteMhz(
        earfcn: Int,
        band: String,
    ): Pair<Double, Double>? {
        val params = lteBandMap[band] ?: return null

        val dlFreq = params.fDlLow + 0.1 * (earfcn - params.nOffsDl)

        val ulEarfcn = earfcn + (params.nOffsUl - params.nOffsDl)
        val ulFreq = params.fUlLow + 0.1 * (ulEarfcn - params.nOffsUl)

        return Pair(dlFreq, ulFreq)
    }

    /**
     TODO - finish 5G calculation
     Oblicza częstotliwości downlink i uplink 5G na podstawie NR-ARFCN i numeru pasma.
     */

    fun calculateNrMhz(nrarfcn: Int): Pair<Double, Double>? = null
}
