/**
 * Created by ST on 8/2/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.data.enums

enum class TyFileType(val value: Int) {
    UNKNOWN(65535),
    FOLDER(1),
    ARCHIVE(2),
    AUDIO(3),
    IMAGE(4),
    MSEXCEL(5),
    MSPOWERPOINT(6),
    MSWORD(7),
    VIDEO(8),
    ACROBAT_PDF(9),
    TEXT(10),
    HTML(11),
    FOLDER_EMPTY(12),
    MP3(13),
    FLV(14),
    EXECUTE(15),
    MINDMAP(16),
    EDITING(17),
    SWF(18),
    CERT(19),
    CHART(20),
    DIAGRAM(21),
    TIFF(22),
    RTF(23),
    XML(24),
    CSV(25),
    MQ4(26),
    MQ5(27),
    MQ5EX(28),
    MQH(29),
    CHM(30),
    ICO(31),
    CALENDAR(32),
    TEAMYAR_TEXT(33),
    VISIO(34),
    TEAMYAR_REPORT(35),
    SVG(36),
    TEAMYAR_SITE_TEXT(37);

    companion object {
        private val map = values().associateBy(TyFileType::value)
        fun fromInt(type: Int): TyFileType = map[type] ?: UNKNOWN
    }
}
