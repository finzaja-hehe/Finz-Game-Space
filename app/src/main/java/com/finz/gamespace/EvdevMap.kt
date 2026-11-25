package com.finz.gamespace

object EvdevMap {

    private val m = HashMap<Int, Int>()

    init {
        val letters = intArrayOf(
            30, 48, 46, 32, 18, 33, 34, 35, 23, 36, 37, 38, 50,
            49, 24, 25, 16, 19, 31, 20, 22, 47, 17, 45, 21, 44
        )
        for (i in letters.indices) m[letters[i]] = 29 + i
        for (i in 0..8) m[2 + i] = 8 + i
        m[11] = 7
        for (i in 0..9) m[59 + i] = 131 + i
        m[87] = 141
        m[88] = 142
        m[57] = 62
        m[28] = 66
        m[1] = 111
        m[15] = 61
        m[14] = 67
        m[42] = 59
        m[54] = 60
        m[29] = 113
        m[97] = 114
        m[56] = 57
        m[100] = 58
        m[58] = 115
        m[103] = 19
        m[108] = 20
        m[105] = 21
        m[106] = 22
        m[12] = 69
        m[13] = 70
        m[26] = 71
        m[27] = 72
        m[43] = 73
        m[39] = 74
        m[40] = 75
        m[41] = 68
        m[51] = 55
        m[52] = 56
        m[53] = 76
        m[272] = 1001
        m[273] = 1002
        m[274] = 1003
        m[275] = 1004
        m[276] = 1005
    }

    fun toAndroid(code: Int): Int = m[code] ?: 0
}
