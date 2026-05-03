package edu.pwr.zpi.netwalk.fetcher

/*  Zwraca informacje dotyczące częstotliwości (UL i DL) i duplex mode
    na podstawie pasma oraz kanału częstotliwości
*/
object NetworkConverter {
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
        1 to LteBandParams(2110.0, 0, 1920.0, 18000),
        2 to LteBandParams(1805.0, 1200, 1710.0, 19200),
        7 to LteBandParams(2620.0, 2750, 2500.0, 20750),
        8 to LteBandParams(942.5, 3450, 897.5, 21450),
        20 to LteBandParams(791.0, 6150, 832.0, 24150),
        38 to LteBandParams(2570.0, 37750, 2570.0, 37750), // TDD
        40 to LteBandParams(2300.0, 38650, 2300.0, 38650), // TDD
    )

    /**
     Oblicza częstotliwości downlink i uplink LTE na podstawie EARFCN i numeru pasma.
     Wzór: F = F_low + 0.1 * (N - N_Offs)
     */
    fun calculateLteMhz(
        earfcn: Int,
        band: Int?,
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

    fun getLTEDuplexMode(band: Int?): String =
        when (band) {
            // FDD
            in 1..28, 30, 31, in 65..88 -> "FDD"

            // TDD
            in 33..53, 103 -> "TDD"

            29, 32, 67, 68 -> "SDL"

            else -> "Unknown"
        }

    fun getNrDuplexMode(band: Int?): String =
        when (band) {
            // FDD
            in 1..26, 28, 30, in 65..74, in 80..84, 86, 89, 91, 92, 93, 94, 95 -> "FDD"

            // TDD
            in 34..41, 46, 47, 48, 50, 51, 53, 54,
            in 77..79, 90, 96, 101, 102, 104, 105, in 257..263, in 270..273,
            -> "TDD"

            29, 32, 75, 76 -> "SDL"
            80, 81, 82, 83, 84, 86, 95 -> "SUL"

            else -> "Unknown"
        }
}
