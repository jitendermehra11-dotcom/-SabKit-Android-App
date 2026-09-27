package com.example.model

data class ZipEntryItem(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val compressedSize: Long,
    val extension: String
)

data class ZipArchive(
    val fileName: String,
    val entries: List<ZipEntryItem>,
    val totalSize: Long,
    val fileCount: Int
)

data class CodeFileView(
    val entryPath: String,
    val fileName: String,
    val content: String,
    val sizeBytes: Long,
    val linesCount: Int
)
