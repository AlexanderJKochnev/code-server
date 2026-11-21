package com.example.photouploader.data.model

/**
 * Ответ сервера на POST /api/upload_image.
 * Сервер возвращает бинарное изображение (webp) + заголовки.
 * Здесь описаны только полезные поля из заголовков.
 */
data class UploadResponse(
    val fileName: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val originalFileSize: Long? = null,
    val thumbnailBytes: ByteArray? = null
) {
    // Для data-классов с ByteArray нужно переопределить equals/hashCode,
    // иначе сравнение будет по ссылке на массив.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as UploadResponse
        if (fileName != other.fileName) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (originalFileSize != other.originalFileSize) return false
        if (thumbnailBytes != null) {
            if (other.thumbnailBytes == null) return false
            if (!thumbnailBytes.contentEquals(other.thumbnailBytes)) return false
        } else if (other.thumbnailBytes != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = fileName?.hashCode() ?: 0
        result = 31 * result + (width ?: 0)
        result = 31 * result + (height ?: 0)
        result = 31 * result + (originalFileSize?.hashCode() ?: 0)
        result = 31 * result + (thumbnailBytes?.contentHashCode() ?: 0)
        return result
    }
}