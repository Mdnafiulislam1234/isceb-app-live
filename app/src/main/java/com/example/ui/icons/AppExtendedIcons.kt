package com.example.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

// Lightweight, zero-bloat vector definitions replacing material-icons-extended (~18MB dex reduction).
// Each property is scoped inside com.example.ui.icons.* so core icons in material-icons-core stay unambiguous.

private var _adminPanelSettings: ImageVector? = null
val Icons.Filled.AdminPanelSettings: ImageVector
    get() = _adminPanelSettings ?: materialIcon(name = "Filled.AdminPanelSettings") {
        materialPath {
            moveTo(17.0f, 11.0f)
            curveToRelative(0.34f, 0.0f, 0.67f, 0.04f, 1.0f, 0.09f)
            verticalLineTo(6.27f)
            lineTo(10.5f, 3.0f)
            lineTo(3.0f, 6.27f)
            verticalLineToRelative(4.91f)
            curveToRelative(0.0f, 4.54f, 3.2f, 8.79f, 7.5f, 9.82f)
            curveToRelative(0.55f, -0.13f, 1.08f, -0.32f, 1.6f, -0.55f)
            curveTo(11.41f, 19.47f, 11.0f, 18.28f, 11.0f, 17.0f)
            curveTo(11.0f, 13.69f, 13.69f, 11.0f, 17.0f, 11.0f)
            close()
        }
        materialPath {
            moveTo(17.0f, 13.0f)
            curveToRelative(-2.21f, 0.0f, -4.0f, 1.79f, -4.0f, 4.0f)
            curveToRelative(0.0f, 2.21f, 1.79f, 4.0f, 4.0f, 4.0f)
            reflectiveCurveToRelative(4.0f, -1.79f, 4.0f, -4.0f)
            curveTo(21.0f, 14.79f, 19.21f, 13.0f, 17.0f, 13.0f)
            close()
            moveTo(17.0f, 14.38f)
            curveToRelative(0.62f, 0.0f, 1.12f, 0.51f, 1.12f, 1.12f)
            reflectiveCurveToRelative(-0.51f, 1.12f, -1.12f, 1.12f)
            reflectiveCurveToRelative(-1.12f, -0.51f, -1.12f, -1.12f)
            reflectiveCurveTo(16.38f, 14.38f, 17.0f, 14.38f)
            close()
            moveTo(17.0f, 19.75f)
            curveToRelative(-0.93f, 0.0f, -1.74f, -0.46f, -2.24f, -1.17f)
            curveToRelative(0.05f, -0.72f, 1.51f, -1.08f, 2.24f, -1.08f)
            reflectiveCurveToRelative(2.19f, 0.36f, 2.24f, 1.08f)
            curveTo(18.74f, 19.29f, 17.93f, 19.75f, 17.0f, 19.75f)
            close()
        }
    }.also { _adminPanelSettings = it }

private var _alarm: ImageVector? = null
val Icons.Filled.Alarm: ImageVector
    get() = _alarm ?: materialIcon(name = "Filled.Alarm") {
        materialPath {
            moveTo(22.0f, 5.72f)
            lineToRelative(-4.6f, -3.86f)
            lineToRelative(-1.29f, 1.53f)
            lineToRelative(4.6f, 3.86f)
            lineTo(22.0f, 5.72f)
            close()
            moveTo(7.88f, 3.39f)
            lineTo(6.6f, 1.86f)
            lineTo(2.0f, 5.71f)
            lineToRelative(1.29f, 1.53f)
            lineToRelative(4.59f, -3.85f)
            close()
            moveTo(12.5f, 8.0f)
            horizontalLineTo(11.0f)
            verticalLineToRelative(6.0f)
            lineToRelative(4.75f, 2.85f)
            lineToRelative(0.75f, -1.23f)
            lineToRelative(-4.0f, -2.37f)
            verticalLineTo(8.0f)
            close()
            moveTo(12.0f, 4.0f)
            curveToRelative(-4.97f, 0.0f, -9.0f, 4.03f, -9.0f, 9.0f)
            reflectiveCurveToRelative(4.02f, 9.0f, 9.0f, 9.0f)
            curveToRelative(4.97f, 0.0f, 9.0f, -4.03f, 9.0f, -9.0f)
            reflectiveCurveTo(16.97f, 4.0f, 12.0f, 4.0f)
            close()
            moveTo(12.0f, 20.0f)
            curveToRelative(-3.87f, 0.0f, -7.0f, -3.13f, -7.0f, -7.0f)
            reflectiveCurveToRelative(3.13f, -7.0f, 7.0f, -7.0f)
            reflectiveCurveToRelative(7.0f, 3.13f, 7.0f, 7.0f)
            reflectiveCurveTo(15.87f, 20.0f, 12.0f, 20.0f)
            close()
        }
    }.also { _alarm = it }

private var _arrowDownward: ImageVector? = null
val Icons.Filled.ArrowDownward: ImageVector
    get() = _arrowDownward ?: materialIcon(name = "Filled.ArrowDownward") {
        materialPath {
            moveTo(20.0f, 12.0f)
            lineToRelative(-1.41f, -1.41f)
            lineTo(13.0f, 16.17f)
            verticalLineTo(4.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineToRelative(12.17f)
            lineToRelative(-5.58f, -5.59f)
            lineTo(4.0f, 12.0f)
            lineToRelative(8.0f, 8.0f)
            lineToRelative(8.0f, -8.0f)
            close()
        }
    }.also { _arrowDownward = it }

private var _arrowUpward: ImageVector? = null
val Icons.Filled.ArrowUpward: ImageVector
    get() = _arrowUpward ?: materialIcon(name = "Filled.ArrowUpward") {
        materialPath {
            moveTo(4.0f, 12.0f)
            lineToRelative(1.41f, 1.41f)
            lineTo(11.0f, 7.83f)
            verticalLineTo(20.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(7.83f)
            lineToRelative(5.58f, 5.59f)
            lineTo(20.0f, 12.0f)
            lineToRelative(-8.0f, -8.0f)
            lineToRelative(-8.0f, 8.0f)
            close()
        }
    }.also { _arrowUpward = it }

private var _article: ImageVector? = null
val Icons.Filled.Article: ImageVector
    get() = _article ?: materialIcon(name = "Filled.Article") {
        materialPath {
            moveTo(19.0f, 3.0f)
            horizontalLineTo(5.0f)
            curveTo(3.9f, 3.0f, 3.0f, 3.9f, 3.0f, 5.0f)
            verticalLineToRelative(14.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(14.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineTo(5.0f)
            curveTo(21.0f, 3.9f, 20.1f, 3.0f, 19.0f, 3.0f)
            close()
            moveTo(14.0f, 17.0f)
            horizontalLineTo(7.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(7.0f)
            verticalLineTo(17.0f)
            close()
            moveTo(17.0f, 13.0f)
            horizontalLineTo(7.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(10.0f)
            verticalLineTo(13.0f)
            close()
            moveTo(17.0f, 9.0f)
            horizontalLineTo(7.0f)
            verticalLineTo(7.0f)
            horizontalLineToRelative(10.0f)
            verticalLineTo(9.0f)
            close()
        }
    }.also { _article = it }

private var _assignment: ImageVector? = null
val Icons.Filled.Assignment: ImageVector
    get() = _assignment ?: materialIcon(name = "Filled.Assignment") {
        materialPath {
            moveTo(19.0f, 3.0f)
            horizontalLineToRelative(-4.18f)
            curveTo(14.4f, 1.84f, 13.3f, 1.0f, 12.0f, 1.0f)
            curveToRelative(-1.3f, 0.0f, -2.4f, 0.84f, -2.82f, 2.0f)
            horizontalLineTo(5.0f)
            curveTo(3.9f, 3.0f, 3.0f, 3.9f, 3.0f, 5.0f)
            verticalLineToRelative(14.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(14.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineTo(5.0f)
            curveTo(21.0f, 3.9f, 20.1f, 3.0f, 19.0f, 3.0f)
            close()
            moveTo(12.0f, 3.0f)
            curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
            reflectiveCurveToRelative(-0.45f, 1.0f, -1.0f, 1.0f)
            reflectiveCurveToRelative(-1.0f, -0.45f, -1.0f, -1.0f)
            reflectiveCurveTo(11.45f, 3.0f, 12.0f, 3.0f)
            close()
            moveTo(14.0f, 17.0f)
            horizontalLineTo(7.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(7.0f)
            verticalLineTo(17.0f)
            close()
            moveTo(17.0f, 13.0f)
            horizontalLineTo(7.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(10.0f)
            verticalLineTo(13.0f)
            close()
            moveTo(17.0f, 9.0f)
            horizontalLineTo(7.0f)
            verticalLineTo(7.0f)
            horizontalLineToRelative(10.0f)
            verticalLineTo(9.0f)
            close()
        }
    }.also { _assignment = it }

private var _attachFile: ImageVector? = null
val Icons.Filled.AttachFile: ImageVector
    get() = _attachFile ?: materialIcon(name = "Filled.AttachFile") {
        materialPath {
            moveTo(16.5f, 6.0f)
            verticalLineToRelative(11.5f)
            curveToRelative(0.0f, 2.21f, -1.79f, 4.0f, -4.0f, 4.0f)
            reflectiveCurveToRelative(-4.0f, -1.79f, -4.0f, -4.0f)
            verticalLineTo(5.0f)
            curveToRelative(0.0f, -1.38f, 1.12f, -2.5f, 2.5f, -2.5f)
            reflectiveCurveToRelative(2.5f, 1.12f, 2.5f, 2.5f)
            verticalLineToRelative(10.5f)
            curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
            reflectiveCurveToRelative(-1.0f, -0.45f, -1.0f, -1.0f)
            verticalLineTo(6.0f)
            horizontalLineTo(10.0f)
            verticalLineToRelative(9.5f)
            curveToRelative(0.0f, 1.38f, 1.12f, 2.5f, 2.5f, 2.5f)
            reflectiveCurveToRelative(2.5f, -1.12f, 2.5f, -2.5f)
            verticalLineTo(5.0f)
            curveToRelative(0.0f, -2.21f, -1.79f, -4.0f, -4.0f, -4.0f)
            reflectiveCurveTo(7.0f, 2.79f, 7.0f, 5.0f)
            verticalLineToRelative(12.5f)
            curveToRelative(0.0f, 3.04f, 2.46f, 5.5f, 5.5f, 5.5f)
            reflectiveCurveToRelative(5.5f, -2.46f, 5.5f, -5.5f)
            verticalLineTo(6.0f)
            horizontalLineTo(16.5f)
            close()
        }
    }.also { _attachFile = it }

private var _audiotrack: ImageVector? = null
val Icons.Filled.Audiotrack: ImageVector
    get() = _audiotrack ?: materialIcon(name = "Filled.Audiotrack") {
        materialPath {
            moveTo(12.0f, 3.0f)
            verticalLineToRelative(9.28f)
            curveToRelative(-0.47f, -0.17f, -0.97f, -0.28f, -1.5f, -0.28f)
            curveTo(8.01f, 12.0f, 6.0f, 14.01f, 6.0f, 16.5f)
            reflectiveCurveTo(8.01f, 21.0f, 10.5f, 21.0f)
            curveToRelative(2.31f, 0.0f, 4.2f, -1.75f, 4.45f, -4.0f)
            horizontalLineTo(15.0f)
            verticalLineTo(6.0f)
            horizontalLineToRelative(4.0f)
            verticalLineTo(3.0f)
            horizontalLineTo(12.0f)
            close()
        }
    }.also { _audiotrack = it }

private var _badge: ImageVector? = null
val Icons.Filled.Badge: ImageVector
    get() = _badge ?: materialIcon(name = "Filled.Badge") {
        materialPath {
            moveTo(20.0f, 7.0f)
            horizontalLineToRelative(-5.0f)
            verticalLineTo(4.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            horizontalLineToRelative(-2.0f)
            curveTo(9.9f, 2.0f, 9.0f, 2.9f, 9.0f, 4.0f)
            verticalLineToRelative(3.0f)
            horizontalLineTo(4.0f)
            curveTo(2.9f, 7.0f, 2.0f, 7.9f, 2.0f, 9.0f)
            verticalLineToRelative(11.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(16.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineTo(9.0f)
            curveTo(22.0f, 7.9f, 21.1f, 7.0f, 20.0f, 7.0f)
            close()
            moveTo(9.0f, 12.0f)
            curveToRelative(0.83f, 0.0f, 1.5f, 0.67f, 1.5f, 1.5f)
            reflectiveCurveTo(9.83f, 15.0f, 9.0f, 15.0f)
            reflectiveCurveToRelative(-1.5f, -0.67f, -1.5f, -1.5f)
            reflectiveCurveTo(8.17f, 12.0f, 9.0f, 12.0f)
            close()
            moveTo(12.0f, 18.0f)
            horizontalLineTo(6.0f)
            verticalLineToRelative(-0.75f)
            curveToRelative(0.0f, -1.0f, 2.0f, -1.5f, 3.0f, -1.5f)
            reflectiveCurveToRelative(3.0f, 0.5f, 3.0f, 1.5f)
            verticalLineTo(18.0f)
            close()
            moveTo(13.0f, 9.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineTo(4.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(9.0f)
            close()
            moveTo(18.0f, 16.5f)
            horizontalLineToRelative(-4.0f)
            verticalLineTo(15.0f)
            horizontalLineToRelative(4.0f)
            verticalLineTo(16.5f)
            close()
            moveTo(18.0f, 13.5f)
            horizontalLineToRelative(-4.0f)
            verticalLineTo(12.0f)
            horizontalLineToRelative(4.0f)
            verticalLineTo(13.5f)
            close()
        }
    }.also { _badge = it }

private var _block: ImageVector? = null
val Icons.Filled.Block: ImageVector
    get() = _block ?: materialIcon(name = "Filled.Block") {
        materialPath {
            moveTo(12.0f, 2.0f)
            curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
            reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
            reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
            reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
            close()
            moveTo(4.0f, 12.0f)
            curveToRelative(0.0f, -4.42f, 3.58f, -8.0f, 8.0f, -8.0f)
            curveToRelative(1.85f, 0.0f, 3.55f, 0.63f, 4.9f, 1.69f)
            lineTo(5.69f, 16.9f)
            curveTo(4.63f, 15.55f, 4.0f, 13.85f, 4.0f, 12.0f)
            close()
            moveTo(12.0f, 20.0f)
            curveToRelative(-1.85f, 0.0f, -3.55f, -0.63f, -4.9f, -1.69f)
            lineTo(18.31f, 7.1f)
            curveTo(19.37f, 8.45f, 20.0f, 10.15f, 20.0f, 12.0f)
            curveTo(20.0f, 16.42f, 16.42f, 20.0f, 12.0f, 20.0f)
            close()
        }
    }.also { _block = it }

private var _bolt: ImageVector? = null
val Icons.Filled.Bolt: ImageVector
    get() = _bolt ?: materialIcon(name = "Filled.Bolt") {
        materialPath {
            moveTo(11.0f, 21.0f)
            horizontalLineToRelative(-1.0f)
            lineToRelative(1.0f, -7.0f)
            horizontalLineTo(7.5f)
            curveToRelative(-0.88f, 0.0f, -0.33f, -0.75f, -0.31f, -0.78f)
            curveToRelative(1.29f, -2.28f, 3.24f, -5.73f, 5.81f, -10.22f)
            horizontalLineToRelative(1.0f)
            lineToRelative(-1.0f, 7.0f)
            horizontalLineToRelative(3.51f)
            curveToRelative(0.4f, 0.0f, 0.62f, 0.19f, 0.4f, 0.66f)
            curveTo(12.97f, 17.55f, 11.0f, 21.0f, 11.0f, 21.0f)
            close()
        }
    }.also { _bolt = it }

private var _bookmarkFilled: ImageVector? = null
val Icons.Filled.Bookmark: ImageVector
    get() = _bookmarkFilled ?: materialIcon(name = "Filled.Bookmark") {
        materialPath {
            moveTo(17.0f, 3.0f)
            horizontalLineTo(7.0f)
            curveTo(5.9f, 3.0f, 5.01f, 3.9f, 5.01f, 5.0f)
            lineTo(5.0f, 21.0f)
            lineToRelative(7.0f, -3.0f)
            lineToRelative(7.0f, 3.0f)
            verticalLineTo(5.0f)
            curveTo(19.0f, 3.9f, 18.1f, 3.0f, 17.0f, 3.0f)
            close()
        }
    }.also { _bookmarkFilled = it }

private var _bookmarkOutlined: ImageVector? = null
val Icons.Outlined.Bookmark: ImageVector
    get() = _bookmarkOutlined ?: materialIcon(name = "Outlined.Bookmark") {
        materialPath {
            moveTo(17.0f, 3.0f)
            horizontalLineTo(7.0f)
            curveTo(5.9f, 3.0f, 5.0f, 3.9f, 5.0f, 5.0f)
            verticalLineToRelative(16.0f)
            lineToRelative(7.0f, -3.0f)
            lineToRelative(7.0f, 3.0f)
            verticalLineTo(5.0f)
            curveTo(19.0f, 3.9f, 18.1f, 3.0f, 17.0f, 3.0f)
            close()
            moveTo(17.0f, 18.0f)
            lineToRelative(-5.0f, -2.18f)
            lineTo(7.0f, 18.0f)
            verticalLineTo(5.0f)
            horizontalLineToRelative(10.0f)
            verticalLineTo(18.0f)
            close()
        }
    }.also { _bookmarkOutlined = it }

private var _bookmarkBorderOutlined: ImageVector? = null
val Icons.Outlined.BookmarkBorder: ImageVector
    get() = _bookmarkBorderOutlined ?: Icons.Outlined.Bookmark.also { _bookmarkBorderOutlined = it }

private var _build: ImageVector? = null
val Icons.Filled.Build: ImageVector
    get() = _build ?: materialIcon(name = "Filled.Build") {
        materialPath {
            moveTo(22.7f, 19.0f)
            lineToRelative(-9.1f, -9.1f)
            curveToRelative(0.9f, -2.3f, 0.4f, -5.0f, -1.5f, -6.9f)
            curveToRelative(-2.0f, -2.0f, -5.0f, -2.4f, -7.4f, -1.3f)
            lineTo(9.0f, 6.0f)
            lineTo(6.0f, 9.0f)
            lineTo(1.6f, 4.7f)
            curveTo(0.4f, 7.1f, 0.9f, 10.1f, 2.9f, 12.1f)
            curveToRelative(1.9f, 1.9f, 4.6f, 2.4f, 6.9f, 1.5f)
            lineToRelative(9.1f, 9.1f)
            curveToRelative(0.4f, 0.4f, 1.0f, 0.4f, 1.4f, 0.0f)
            lineToRelative(2.3f, -2.3f)
            curveTo(23.1f, 20.0f, 23.1f, 19.3f, 22.7f, 19.0f)
            close()
        }
    }.also { _build = it }

private var _business: ImageVector? = null
val Icons.Filled.Business: ImageVector
    get() = _business ?: materialIcon(name = "Filled.Business") {
        materialPath {
            moveTo(12.0f, 7.0f)
            verticalLineTo(3.0f)
            horizontalLineTo(2.0f)
            verticalLineToRelative(18.0f)
            horizontalLineToRelative(20.0f)
            verticalLineTo(7.0f)
            horizontalLineTo(12.0f)
            close()
            moveTo(6.0f, 19.0f)
            horizontalLineTo(4.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(19.0f)
            close()
            moveTo(6.0f, 15.0f)
            horizontalLineTo(4.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(15.0f)
            close()
            moveTo(6.0f, 11.0f)
            horizontalLineTo(4.0f)
            verticalLineTo(9.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(11.0f)
            close()
            moveTo(6.0f, 7.0f)
            horizontalLineTo(4.0f)
            verticalLineTo(5.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(7.0f)
            close()
            moveTo(10.0f, 19.0f)
            horizontalLineTo(8.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(19.0f)
            close()
            moveTo(10.0f, 15.0f)
            horizontalLineTo(8.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(15.0f)
            close()
            moveTo(10.0f, 11.0f)
            horizontalLineTo(8.0f)
            verticalLineTo(9.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(11.0f)
            close()
            moveTo(10.0f, 7.0f)
            horizontalLineTo(8.0f)
            verticalLineTo(5.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(7.0f)
            close()
            moveTo(20.0f, 19.0f)
            horizontalLineToRelative(-8.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineTo(9.0f)
            horizontalLineToRelative(8.0f)
            verticalLineTo(19.0f)
            close()
            moveTo(18.0f, 11.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(11.0f)
            close()
            moveTo(18.0f, 15.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(15.0f)
            close()
        }
    }.also { _business = it }

private var _campaign: ImageVector? = null
val Icons.Filled.Campaign: ImageVector
    get() = _campaign ?: materialIcon(name = "Filled.Campaign") {
        materialPath {
            moveTo(18.0f, 11.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineTo(18.0f)
            close()
            moveTo(16.0f, 17.61f)
            curveToRelative(0.96f, 0.71f, 2.21f, 1.65f, 3.2f, 2.39f)
            curveToRelative(0.4f, -0.53f, 0.8f, -1.07f, 1.2f, -1.6f)
            curveToRelative(-0.99f, -0.74f, -2.24f, -1.68f, -3.2f, -2.4f)
            curveTo(16.8f, 16.54f, 16.4f, 17.08f, 16.0f, 17.61f)
            close()
            moveTo(20.4f, 5.6f)
            curveTo(20.0f, 5.07f, 19.6f, 4.53f, 19.2f, 4.0f)
            curveToRelative(-0.99f, 0.74f, -2.24f, 1.68f, -3.2f, 2.4f)
            curveToRelative(0.4f, 0.53f, 0.8f, 1.07f, 1.2f, 1.6f)
            curveTo(18.19f, 7.25f, 19.44f, 6.31f, 20.4f, 5.6f)
            close()
            moveTo(4.0f, 9.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(2.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(1.0f)
            verticalLineToRelative(4.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(-4.0f)
            horizontalLineToRelative(1.0f)
            lineToRelative(5.0f, 3.0f)
            verticalLineTo(6.0f)
            lineTo(8.0f, 9.0f)
            horizontalLineTo(4.0f)
            close()
            moveTo(15.5f, 12.0f)
            curveToRelative(0.0f, -1.33f, -0.58f, -2.53f, -1.5f, -3.35f)
            verticalLineToRelative(6.69f)
            curveTo(14.92f, 14.53f, 15.5f, 13.33f, 15.5f, 12.0f)
            close()
        }
    }.also { _campaign = it }

private var _checkCircleOutline: ImageVector? = null
val Icons.Outlined.CheckCircleOutline: ImageVector
    get() = _checkCircleOutline ?: materialIcon(name = "Outlined.CheckCircleOutline") {
        materialPath {
            moveTo(16.59f, 7.58f)
            lineTo(10.0f, 14.17f)
            lineToRelative(-3.59f, -3.58f)
            lineTo(5.0f, 12.0f)
            lineToRelative(5.0f, 5.0f)
            lineToRelative(8.0f, -8.0f)
            lineTo(16.59f, 7.58f)
            close()
            moveTo(12.0f, 2.0f)
            curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
            reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
            reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
            reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
            close()
            moveTo(12.0f, 20.0f)
            curveToRelative(-4.42f, 0.0f, -8.0f, -3.58f, -8.0f, -8.0f)
            reflectiveCurveToRelative(3.58f, -8.0f, 8.0f, -8.0f)
            reflectiveCurveToRelative(8.0f, 3.58f, 8.0f, 8.0f)
            reflectiveCurveTo(16.42f, 20.0f, 12.0f, 20.0f)
            close()
        }
    }.also { _checkCircleOutline = it }

private var _chevronLeft: ImageVector? = null
val Icons.Filled.ChevronLeft: ImageVector
    get() = _chevronLeft ?: materialIcon(name = "Filled.ChevronLeft") {
        materialPath {
            moveTo(15.41f, 7.41f)
            lineTo(14.0f, 6.0f)
            lineToRelative(-6.0f, 6.0f)
            lineToRelative(6.0f, 6.0f)
            lineToRelative(1.41f, -1.41f)
            lineTo(10.83f, 12.0f)
            close()
        }
    }.also { _chevronLeft = it }

private var _chevronRight: ImageVector? = null
val Icons.Filled.ChevronRight: ImageVector
    get() = _chevronRight ?: materialIcon(name = "Filled.ChevronRight") {
        materialPath {
            moveTo(10.0f, 6.0f)
            lineTo(8.59f, 7.41f)
            lineTo(13.17f, 12.0f)
            lineToRelative(-4.58f, 4.59f)
            lineTo(10.0f, 18.0f)
            lineToRelative(6.0f, -6.0f)
            close()
        }
    }.also { _chevronRight = it }

private var _cloud: ImageVector? = null
val Icons.Filled.Cloud: ImageVector
    get() = _cloud ?: materialIcon(name = "Filled.Cloud") {
        materialPath {
            moveTo(19.35f, 10.04f)
            curveTo(18.67f, 6.59f, 15.64f, 4.0f, 12.0f, 4.0f)
            curveTo(9.11f, 4.0f, 6.6f, 5.64f, 5.35f, 8.04f)
            curveTo(2.34f, 8.36f, 0.0f, 10.91f, 0.0f, 14.0f)
            curveToRelative(0.0f, 3.31f, 2.69f, 6.0f, 6.0f, 6.0f)
            horizontalLineToRelative(13.0f)
            curveToRelative(2.76f, 0.0f, 5.0f, -2.24f, 5.0f, -5.0f)
            curveTo(24.0f, 12.36f, 21.95f, 10.22f, 19.35f, 10.04f)
            close()
        }
    }.also { _cloud = it }

private var _cloudDone: ImageVector? = null
val Icons.Filled.CloudDone: ImageVector
    get() = _cloudDone ?: materialIcon(name = "Filled.CloudDone") {
        materialPath {
            moveTo(19.35f, 10.04f)
            curveTo(18.67f, 6.59f, 15.64f, 4.0f, 12.0f, 4.0f)
            curveTo(9.11f, 4.0f, 6.6f, 5.64f, 5.35f, 8.04f)
            curveTo(2.34f, 8.36f, 0.0f, 10.91f, 0.0f, 14.0f)
            curveToRelative(0.0f, 3.31f, 2.69f, 6.0f, 6.0f, 6.0f)
            horizontalLineToRelative(13.0f)
            curveToRelative(2.76f, 0.0f, 5.0f, -2.24f, 5.0f, -5.0f)
            curveTo(24.0f, 12.36f, 21.95f, 10.22f, 19.35f, 10.04f)
            close()
            moveTo(10.0f, 17.0f)
            lineToRelative(-3.5f, -3.5f)
            lineToRelative(1.41f, -1.41f)
            lineTo(10.0f, 14.17f)
            lineTo(15.18f, 9.0f)
            lineToRelative(1.41f, 1.41f)
            lineTo(10.0f, 17.0f)
            close()
        }
    }.also { _cloudDone = it }

private var _cloudOff: ImageVector? = null
val Icons.Filled.CloudOff: ImageVector
    get() = _cloudOff ?: materialIcon(name = "Filled.CloudOff") {
        materialPath {
            moveTo(19.35f, 10.04f)
            curveTo(18.67f, 6.59f, 15.64f, 4.0f, 12.0f, 4.0f)
            curveToRelative(-1.48f, 0.0f, -2.85f, 0.43f, -4.01f, 1.17f)
            lineToRelative(1.46f, 1.46f)
            curveTo(10.21f, 6.23f, 11.08f, 6.0f, 12.0f, 6.0f)
            curveToRelative(3.04f, 0.0f, 5.5f, 2.46f, 5.5f, 5.5f)
            verticalLineToRelative(0.5f)
            horizontalLineTo(19.0f)
            curveToRelative(1.66f, 0.0f, 3.0f, 1.34f, 3.0f, 3.0f)
            curveToRelative(0.0f, 1.13f, -0.64f, 2.11f, -1.56f, 2.62f)
            lineToRelative(1.45f, 1.45f)
            curveTo(23.16f, 18.16f, 24.0f, 16.68f, 24.0f, 15.0f)
            curveTo(24.0f, 12.36f, 21.95f, 10.22f, 19.35f, 10.04f)
            close()
            moveTo(3.0f, 5.27f)
            lineToRelative(2.75f, 2.74f)
            curveTo(2.56f, 8.15f, 0.0f, 10.77f, 0.0f, 14.0f)
            curveToRelative(0.0f, 3.31f, 2.69f, 6.0f, 6.0f, 6.0f)
            horizontalLineToRelative(11.73f)
            lineToRelative(2.0f, 2.0f)
            lineTo(21.0f, 20.73f)
            lineTo(4.27f, 4.0f)
            lineTo(3.0f, 5.27f)
            close()
            moveTo(7.73f, 10.0f)
            lineToRelative(8.0f, 8.0f)
            horizontalLineTo(6.0f)
            curveToRelative(-2.21f, 0.0f, -4.0f, -1.79f, -4.0f, -4.0f)
            reflectiveCurveToRelative(1.79f, -4.0f, 4.0f, -4.0f)
            horizontalLineTo(7.73f)
            close()
        }
    }.also { _cloudOff = it }

private var _cloudSync: ImageVector? = null
val Icons.Filled.CloudSync: ImageVector
    get() = _cloudSync ?: materialIcon(name = "Filled.CloudSync") {
        materialPath {
            moveTo(19.35f, 10.04f)
            curveTo(18.67f, 6.59f, 15.64f, 4.0f, 12.0f, 4.0f)
            curveTo(9.11f, 4.0f, 6.6f, 5.64f, 5.35f, 8.04f)
            curveTo(2.34f, 8.36f, 0.0f, 10.91f, 0.0f, 14.0f)
            curveToRelative(0.0f, 3.31f, 2.69f, 6.0f, 6.0f, 6.0f)
            horizontalLineToRelative(13.0f)
            curveToRelative(2.76f, 0.0f, 5.0f, -2.24f, 5.0f, -5.0f)
            curveTo(24.0f, 12.36f, 21.95f, 10.22f, 19.35f, 10.04f)
            close()
            moveTo(12.0f, 18.0f)
            curveToRelative(-2.21f, 0.0f, -4.0f, -1.79f, -4.0f, -4.0f)
            horizontalLineToRelative(2.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            curveToRelative(0.55f, 0.0f, 1.05f, -0.22f, 1.41f, -0.59f)
            lineTo(12.0f, 14.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(4.0f)
            lineToRelative(-1.17f, -1.17f)
            curveTo(14.11f, 17.55f, 13.11f, 18.0f, 12.0f, 18.0f)
            close()
            moveTo(14.0f, 14.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            curveToRelative(-0.55f, 0.0f, -1.05f, 0.22f, -1.41f, 0.59f)
            lineTo(12.0f, 14.0f)
            horizontalLineTo(8.0f)
            verticalLineToRelative(-4.0f)
            lineToRelative(1.17f, 1.17f)
            curveTo(9.89f, 10.45f, 10.89f, 10.0f, 12.0f, 10.0f)
            curveToRelative(2.21f, 0.0f, 4.0f, 1.79f, 4.0f, 4.0f)
            horizontalLineTo(14.0f)
            close()
        }
    }.also { _cloudSync = it }

private var _cloudUpload: ImageVector? = null
val Icons.Filled.CloudUpload: ImageVector
    get() = _cloudUpload ?: materialIcon(name = "Filled.CloudUpload") {
        materialPath {
            moveTo(19.35f, 10.04f)
            curveTo(18.67f, 6.59f, 15.64f, 4.0f, 12.0f, 4.0f)
            curveTo(9.11f, 4.0f, 6.6f, 5.64f, 5.35f, 8.04f)
            curveTo(2.34f, 8.36f, 0.0f, 10.91f, 0.0f, 14.0f)
            curveToRelative(0.0f, 3.31f, 2.69f, 6.0f, 6.0f, 6.0f)
            horizontalLineToRelative(13.0f)
            curveToRelative(2.76f, 0.0f, 5.0f, -2.24f, 5.0f, -5.0f)
            curveTo(24.0f, 12.36f, 21.95f, 10.22f, 19.35f, 10.04f)
            close()
            moveTo(14.0f, 13.0f)
            verticalLineToRelative(4.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineToRelative(-4.0f)
            horizontalLineTo(7.0f)
            lineToRelative(5.0f, -5.0f)
            lineToRelative(5.0f, 5.0f)
            horizontalLineTo(14.0f)
            close()
        }
    }.also { _cloudUpload = it }

private var _contactMail: ImageVector? = null
val Icons.Filled.ContactMail: ImageVector
    get() = _contactMail ?: materialIcon(name = "Filled.ContactMail") {
        materialPath {
            moveTo(21.0f, 8.0f)
            verticalLineTo(7.0f)
            lineToRelative(-3.0f, 2.0f)
            lineToRelative(-3.0f, -2.0f)
            verticalLineToRelative(1.0f)
            lineToRelative(3.0f, 2.0f)
            lineToRelative(3.0f, -2.0f)
            close()
            moveTo(22.0f, 3.0f)
            horizontalLineTo(2.0f)
            curveTo(0.9f, 3.0f, 0.0f, 3.9f, 0.0f, 5.0f)
            verticalLineToRelative(14.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(20.0f)
            curveToRelative(1.1f, 0.0f, 1.99f, -0.9f, 1.99f, -2.0f)
            lineTo(24.0f, 5.0f)
            curveTo(24.0f, 3.9f, 23.1f, 3.0f, 22.0f, 3.0f)
            close()
            moveTo(8.0f, 6.0f)
            curveToRelative(1.66f, 0.0f, 3.0f, 1.34f, 3.0f, 3.0f)
            reflectiveCurveToRelative(-1.34f, 3.0f, -3.0f, 3.0f)
            reflectiveCurveToRelative(-3.0f, -1.34f, -3.0f, -3.0f)
            reflectiveCurveTo(6.34f, 6.0f, 8.0f, 6.0f)
            close()
            moveTo(14.0f, 18.0f)
            horizontalLineTo(2.0f)
            verticalLineToRelative(-1.0f)
            curveToRelative(0.0f, -2.0f, 4.0f, -3.1f, 6.0f, -3.1f)
            reflectiveCurveToRelative(6.0f, 1.1f, 6.0f, 3.1f)
            verticalLineTo(18.0f)
            close()
            moveTo(22.0f, 12.0f)
            horizontalLineToRelative(-8.0f)
            verticalLineTo(6.0f)
            horizontalLineToRelative(8.0f)
            verticalLineTo(12.0f)
            close()
        }
    }.also { _contactMail = it }

private var _contentCopy: ImageVector? = null
val Icons.Filled.ContentCopy: ImageVector
    get() = _contentCopy ?: materialIcon(name = "Filled.ContentCopy") {
        materialPath {
            moveTo(16.0f, 1.0f)
            horizontalLineTo(4.0f)
            curveTo(2.9f, 1.0f, 2.0f, 1.9f, 2.0f, 3.0f)
            verticalLineToRelative(14.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(3.0f)
            horizontalLineToRelative(12.0f)
            verticalLineTo(1.0f)
            close()
            moveTo(19.0f, 5.0f)
            horizontalLineTo(8.0f)
            curveTo(6.9f, 5.0f, 6.0f, 5.9f, 6.0f, 7.0f)
            verticalLineToRelative(14.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(11.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineTo(7.0f)
            curveTo(21.0f, 5.9f, 20.1f, 5.0f, 19.0f, 5.0f)
            close()
            moveTo(19.0f, 21.0f)
            horizontalLineTo(8.0f)
            verticalLineTo(7.0f)
            horizontalLineToRelative(11.0f)
            verticalLineTo(21.0f)
            close()
        }
    }.also { _contentCopy = it }

private var _description: ImageVector? = null
val Icons.Filled.Description: ImageVector
    get() = _description ?: materialIcon(name = "Filled.Description") {
        materialPath {
            moveTo(14.0f, 2.0f)
            horizontalLineTo(6.0f)
            curveTo(4.9f, 2.0f, 4.0f, 2.9f, 4.0f, 4.0f)
            verticalLineToRelative(16.0f)
            curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 1.99f, 2.0f)
            horizontalLineTo(18.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineTo(8.0f)
            lineTo(14.0f, 2.0f)
            close()
            moveTo(16.0f, 18.0f)
            horizontalLineTo(8.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(8.0f)
            verticalLineTo(18.0f)
            close()
            moveTo(16.0f, 14.0f)
            horizontalLineTo(8.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(8.0f)
            verticalLineTo(14.0f)
            close()
            moveTo(13.0f, 9.0f)
            verticalLineTo(3.5f)
            lineTo(18.5f, 9.0f)
            horizontalLineTo(13.0f)
            close()
        }
    }.also { _description = it }

private var _download: ImageVector? = null
val Icons.Filled.Download: ImageVector
    get() = _download ?: materialIcon(name = "Filled.Download") {
        materialPath {
            moveTo(5.0f, 20.0f)
            horizontalLineToRelative(14.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineTo(5.0f)
            verticalLineTo(20.0f)
            close()
            moveTo(19.0f, 9.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineTo(3.0f)
            horizontalLineTo(9.0f)
            verticalLineToRelative(6.0f)
            horizontalLineTo(5.0f)
            lineToRelative(7.0f, 7.0f)
            lineTo(19.0f, 9.0f)
            close()
        }
    }.also { _download = it }

private var _downloadDone: ImageVector? = null
val Icons.Filled.DownloadDone: ImageVector
    get() = _downloadDone ?: materialIcon(name = "Filled.DownloadDone") {
        materialPath {
            moveTo(5.0f, 18.0f)
            horizontalLineToRelative(14.0f)
            verticalLineToRelative(2.0f)
            horizontalLineTo(5.0f)
            verticalLineTo(18.0f)
            close()
            moveTo(9.6f, 15.3f)
            lineTo(5.0f, 10.7f)
            lineToRelative(2.0f, -1.9f)
            lineToRelative(2.6f, 2.6f)
            lineTo(17.0f, 4.0f)
            lineToRelative(2.0f, 2.0f)
            lineTo(9.6f, 15.3f)
            close()
        }
    }.also { _downloadDone = it }

private var _emojiEvents: ImageVector? = null
val Icons.Filled.EmojiEvents: ImageVector
    get() = _emojiEvents ?: materialIcon(name = "Filled.EmojiEvents") {
        materialPath {
            moveTo(19.0f, 5.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineTo(3.0f)
            horizontalLineTo(7.0f)
            verticalLineToRelative(2.0f)
            horizontalLineTo(5.0f)
            curveTo(3.9f, 5.0f, 3.0f, 5.9f, 3.0f, 7.0f)
            verticalLineToRelative(1.0f)
            curveToRelative(0.0f, 2.55f, 1.92f, 4.63f, 4.39f, 4.94f)
            curveToRelative(0.63f, 1.5f, 1.98f, 2.63f, 3.61f, 2.96f)
            verticalLineTo(19.0f)
            horizontalLineTo(7.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(10.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineToRelative(-3.1f)
            curveToRelative(1.63f, -0.33f, 2.98f, -1.46f, 3.61f, -2.96f)
            curveTo(19.08f, 12.63f, 21.0f, 10.55f, 21.0f, 8.0f)
            verticalLineTo(7.0f)
            curveTo(21.0f, 5.9f, 20.1f, 5.0f, 19.0f, 5.0f)
            close()
            moveTo(5.0f, 8.0f)
            verticalLineTo(7.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(3.82f)
            curveTo(5.84f, 10.4f, 5.0f, 9.3f, 5.0f, 8.0f)
            close()
            moveTo(19.0f, 8.0f)
            curveToRelative(0.0f, 1.3f, -0.84f, 2.4f, -2.0f, 2.82f)
            verticalLineTo(7.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(8.0f)
            close()
        }
    }.also { _emojiEvents = it }

private var _error: ImageVector? = null
val Icons.Filled.Error: ImageVector
    get() = _error ?: materialIcon(name = "Filled.Error") {
        materialPath {
            moveTo(12.0f, 2.0f)
            curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
            reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
            reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
            reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
            close()
            moveTo(13.0f, 17.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(17.0f)
            close()
            moveTo(13.0f, 13.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineTo(7.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(13.0f)
            close()
        }
    }.also { _error = it }

private var _errorOutline: ImageVector? = null
val Icons.Filled.ErrorOutline: ImageVector
    get() = _errorOutline ?: materialIcon(name = "Filled.ErrorOutline") {
        materialPath {
            moveTo(11.0f, 15.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineTo(15.0f)
            close()
            moveTo(11.0f, 7.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(6.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineTo(7.0f)
            close()
            moveTo(11.99f, 2.0f)
            curveTo(6.47f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
            reflectiveCurveToRelative(4.47f, 10.0f, 9.99f, 10.0f)
            curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f)
            reflectiveCurveTo(17.52f, 2.0f, 11.99f, 2.0f)
            close()
            moveTo(12.0f, 20.0f)
            curveToRelative(-4.42f, 0.0f, -8.0f, -3.58f, -8.0f, -8.0f)
            reflectiveCurveToRelative(3.58f, -8.0f, 8.0f, -8.0f)
            reflectiveCurveToRelative(8.0f, 3.58f, 8.0f, 8.0f)
            reflectiveCurveTo(16.42f, 20.0f, 12.0f, 20.0f)
            close()
        }
    }.also { _errorOutline = it }

private var _filterList: ImageVector? = null
val Icons.Filled.FilterList: ImageVector
    get() = _filterList ?: materialIcon(name = "Filled.FilterList") {
        materialPath {
            moveTo(10.0f, 18.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineTo(18.0f)
            close()
            moveTo(3.0f, 6.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(18.0f)
            verticalLineTo(6.0f)
            horizontalLineTo(3.0f)
            close()
            moveTo(6.0f, 13.0f)
            horizontalLineToRelative(12.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineTo(6.0f)
            verticalLineTo(13.0f)
            close()
        }
    }.also { _filterList = it }

private var _forward10: ImageVector? = null
val Icons.Filled.Forward10: ImageVector
    get() = _forward10 ?: materialIcon(name = "Filled.Forward10") {
        materialPath {
            moveTo(18.0f, 13.0f)
            curveToRelative(0.0f, 3.31f, -2.69f, 6.0f, -6.0f, 6.0f)
            reflectiveCurveToRelative(-6.0f, -2.69f, -6.0f, -6.0f)
            reflectiveCurveToRelative(2.69f, -6.0f, 6.0f, -6.0f)
            verticalLineToRelative(4.0f)
            lineToRelative(5.0f, -5.0f)
            lineToRelative(-5.0f, -5.0f)
            verticalLineToRelative(4.0f)
            curveToRelative(-4.42f, 0.0f, -8.0f, 3.58f, -8.0f, 8.0f)
            curveToRelative(0.0f, 4.42f, 3.58f, 8.0f, 8.0f, 8.0f)
            reflectiveCurveToRelative(8.0f, -3.58f, 8.0f, -8.0f)
            horizontalLineTo(18.0f)
            close()
        }
    }.also { _forward10 = it }

private var _gridViewFilled: ImageVector? = null
val Icons.Filled.GridView: ImageVector
    get() = _gridViewFilled ?: materialIcon(name = "Filled.GridView") {
        materialPath {
            moveTo(3.0f, 3.0f)
            verticalLineToRelative(8.0f)
            horizontalLineToRelative(8.0f)
            verticalLineTo(3.0f)
            horizontalLineTo(3.0f)
            close()
            moveTo(13.0f, 3.0f)
            verticalLineToRelative(8.0f)
            horizontalLineToRelative(8.0f)
            verticalLineTo(3.0f)
            horizontalLineTo(13.0f)
            close()
            moveTo(3.0f, 13.0f)
            verticalLineToRelative(8.0f)
            horizontalLineToRelative(8.0f)
            verticalLineToRelative(-8.0f)
            horizontalLineTo(3.0f)
            close()
            moveTo(13.0f, 13.0f)
            verticalLineToRelative(8.0f)
            horizontalLineToRelative(8.0f)
            verticalLineToRelative(-8.0f)
            horizontalLineTo(13.0f)
            close()
        }
    }.also { _gridViewFilled = it }

private var _gridViewOutlined: ImageVector? = null
val Icons.Outlined.GridView: ImageVector
    get() = _gridViewOutlined ?: materialIcon(name = "Outlined.GridView") {
        materialPath {
            moveTo(3.0f, 3.0f)
            verticalLineToRelative(8.0f)
            horizontalLineToRelative(8.0f)
            verticalLineTo(3.0f)
            horizontalLineTo(3.0f)
            close()
            moveTo(9.0f, 9.0f)
            horizontalLineTo(5.0f)
            verticalLineTo(5.0f)
            horizontalLineToRelative(4.0f)
            verticalLineTo(9.0f)
            close()
            moveTo(3.0f, 13.0f)
            verticalLineToRelative(8.0f)
            horizontalLineToRelative(8.0f)
            verticalLineToRelative(-8.0f)
            horizontalLineTo(3.0f)
            close()
            moveTo(9.0f, 19.0f)
            horizontalLineTo(5.0f)
            verticalLineToRelative(-4.0f)
            horizontalLineToRelative(4.0f)
            verticalLineTo(19.0f)
            close()
            moveTo(13.0f, 3.0f)
            verticalLineToRelative(8.0f)
            horizontalLineToRelative(8.0f)
            verticalLineTo(3.0f)
            horizontalLineTo(13.0f)
            close()
            moveTo(19.0f, 9.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineTo(5.0f)
            horizontalLineToRelative(4.0f)
            verticalLineTo(9.0f)
            close()
            moveTo(13.0f, 13.0f)
            verticalLineToRelative(8.0f)
            horizontalLineToRelative(8.0f)
            verticalLineToRelative(-8.0f)
            horizontalLineTo(13.0f)
            close()
            moveTo(19.0f, 19.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineToRelative(-4.0f)
            horizontalLineToRelative(4.0f)
            verticalLineTo(19.0f)
            close()
        }
    }.also { _gridViewOutlined = it }

private var _groupsFilled: ImageVector? = null
val Icons.Filled.Groups: ImageVector
    get() = _groupsFilled ?: materialIcon(name = "Filled.Groups") {
        materialPath {
            moveTo(12.0f, 12.75f)
            curveToRelative(1.63f, 0.0f, 3.07f, 0.39f, 4.24f, 0.9f)
            curveToRelative(1.08f, 0.48f, 1.76f, 1.56f, 1.76f, 2.73f)
            verticalLineTo(18.0f)
            horizontalLineTo(6.0f)
            verticalLineToRelative(-1.61f)
            curveToRelative(0.0f, -1.18f, 0.68f, -2.26f, 1.76f, -2.73f)
            curveTo(8.93f, 13.14f, 10.37f, 12.75f, 12.0f, 12.75f)
            close()
            moveTo(12.0f, 11.0f)
            curveToRelative(1.66f, 0.0f, 3.0f, -1.34f, 3.0f, -3.0f)
            reflectiveCurveToRelative(-1.34f, -3.0f, -3.0f, -3.0f)
            reflectiveCurveTo(9.0f, 6.34f, 9.0f, 8.0f)
            reflectiveCurveTo(10.34f, 11.0f, 12.0f, 11.0f)
            close()
            moveTo(4.0f, 13.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
            curveTo(2.0f, 12.1f, 2.9f, 13.0f, 4.0f, 13.0f)
            close()
            moveTo(20.0f, 13.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
            curveTo(18.0f, 12.1f, 18.9f, 13.0f, 20.0f, 13.0f)
            close()
        }
    }.also { _groupsFilled = it }

private var _groupsOutlined: ImageVector? = null
val Icons.Outlined.Groups: ImageVector
    get() = _groupsOutlined ?: Icons.Filled.Groups.also { _groupsOutlined = it }

private var _helpOutline: ImageVector? = null
val Icons.Filled.HelpOutline: ImageVector
    get() = _helpOutline ?: materialIcon(name = "Filled.HelpOutline") {
        materialPath {
            moveTo(11.0f, 18.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineTo(18.0f)
            close()
            moveTo(12.0f, 2.0f)
            curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
            reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
            reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
            reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
            close()
            moveTo(12.0f, 20.0f)
            curveToRelative(-4.41f, 0.0f, -8.0f, -3.59f, -8.0f, -8.0f)
            reflectiveCurveToRelative(3.59f, -8.0f, 8.0f, -8.0f)
            reflectiveCurveToRelative(8.0f, 3.59f, 8.0f, 8.0f)
            reflectiveCurveTo(16.41f, 20.0f, 12.0f, 20.0f)
            close()
            moveTo(12.0f, 6.0f)
            curveToRelative(-2.21f, 0.0f, -4.0f, 1.79f, -4.0f, 4.0f)
            horizontalLineToRelative(2.0f)
            curveToRelative(0.0f, -1.1f, 0.9f, -2.0f, 2.0f, -2.0f)
            reflectiveCurveToRelative(2.0f, 0.9f, 2.0f, 2.0f)
            curveToRelative(0.0f, 2.0f, -3.0f, 1.75f, -3.0f, 5.0f)
            horizontalLineToRelative(2.0f)
            curveToRelative(0.0f, -2.25f, 3.0f, -2.5f, 3.0f, -5.0f)
            curveTo(16.0f, 7.79f, 14.21f, 6.0f, 12.0f, 6.0f)
            close()
        }
    }.also { _helpOutline = it }

private var _image: ImageVector? = null
val Icons.Filled.Image: ImageVector
    get() = _image ?: materialIcon(name = "Filled.Image") {
        materialPath {
            moveTo(21.0f, 19.0f)
            verticalLineTo(5.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            horizontalLineTo(5.0f)
            curveTo(3.9f, 3.0f, 3.0f, 3.9f, 3.0f, 5.0f)
            verticalLineToRelative(14.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(14.0f)
            curveTo(20.1f, 21.0f, 21.0f, 20.1f, 21.0f, 19.0f)
            close()
            moveTo(8.5f, 13.5f)
            lineToRelative(2.5f, 3.01f)
            lineTo(14.5f, 12.0f)
            lineToRelative(4.5f, 6.0f)
            horizontalLineTo(5.0f)
            lineTo(8.5f, 13.5f)
            close()
        }
    }.also { _image = it }

private var _installMobile: ImageVector? = null
val Icons.Filled.InstallMobile: ImageVector
    get() = _installMobile ?: materialIcon(name = "Filled.InstallMobile") {
        materialPath {
            moveTo(17.0f, 18.0f)
            horizontalLineTo(7.0f)
            verticalLineTo(6.0f)
            horizontalLineToRelative(7.0f)
            verticalLineTo(1.0f)
            horizontalLineTo(7.0f)
            curveTo(5.9f, 1.0f, 5.0f, 1.9f, 5.0f, 3.0f)
            verticalLineToRelative(18.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(10.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineToRelative(-5.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineTo(18.0f)
            close()
            moveTo(18.0f, 14.0f)
            lineToRelative(5.0f, -5.0f)
            lineToRelative(-1.41f, -1.41f)
            lineTo(19.0f, 10.17f)
            verticalLineTo(3.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineToRelative(7.17f)
            lineToRelative(-2.59f, -2.58f)
            lineTo(13.0f, 9.0f)
            lineTo(18.0f, 14.0f)
            close()
        }
    }.also { _installMobile = it }

private var _key: ImageVector? = null
val Icons.Filled.Key: ImageVector
    get() = _key ?: materialIcon(name = "Filled.Key") {
        materialPath {
            moveTo(12.65f, 10.0f)
            curveTo(11.83f, 7.67f, 9.61f, 6.0f, 7.0f, 6.0f)
            curveToRelative(-3.31f, 0.0f, -6.0f, 2.69f, -6.0f, 6.0f)
            reflectiveCurveToRelative(2.69f, 6.0f, 6.0f, 6.0f)
            curveToRelative(2.61f, 0.0f, 4.83f, -1.67f, 5.65f, -4.0f)
            horizontalLineTo(17.0f)
            verticalLineToRelative(4.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(-4.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(-4.0f)
            horizontalLineTo(12.65f)
            close()
            moveTo(7.0f, 14.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f)
            reflectiveCurveToRelative(0.9f, -2.0f, 2.0f, -2.0f)
            reflectiveCurveToRelative(2.0f, 0.9f, 2.0f, 2.0f)
            reflectiveCurveTo(8.1f, 14.0f, 7.0f, 14.0f)
            close()
        }
    }.also { _key = it }

private var _language: ImageVector? = null
val Icons.Filled.Language: ImageVector
    get() = _language ?: materialIcon(name = "Filled.Language") {
        materialPath {
            moveTo(11.99f, 2.0f)
            curveTo(6.47f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
            reflectiveCurveToRelative(4.47f, 10.0f, 9.99f, 10.0f)
            curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f)
            reflectiveCurveTo(17.52f, 2.0f, 11.99f, 2.0f)
            close()
            moveTo(18.92f, 8.0f)
            horizontalLineToRelative(-2.95f)
            curveToRelative(-0.32f, -1.25f, -0.78f, -2.45f, -1.38f, -3.56f)
            curveTo(16.43f, 5.07f, 17.96f, 6.34f, 18.92f, 8.0f)
            close()
            moveTo(12.0f, 4.04f)
            curveToRelative(0.83f, 1.2f, 1.48f, 2.53f, 1.91f, 3.96f)
            horizontalLineToRelative(-3.82f)
            curveTo(10.52f, 6.57f, 11.17f, 5.24f, 12.0f, 4.04f)
            close()
            moveTo(4.26f, 14.0f)
            curveTo(4.1f, 13.36f, 4.0f, 12.69f, 4.0f, 12.0f)
            reflectiveCurveToRelative(0.1f, -1.36f, 0.26f, -2.0f)
            horizontalLineToRelative(3.38f)
            curveToRelative(-0.08f, 0.66f, -0.14f, 1.32f, -0.14f, 2.0f)
            curveToRelative(0.0f, 0.68f, 0.06f, 1.34f, 0.14f, 2.0f)
            horizontalLineTo(4.26f)
            close()
            moveTo(5.08f, 16.0f)
            horizontalLineToRelative(2.95f)
            curveToRelative(0.32f, 1.25f, 0.78f, 2.45f, 1.38f, 3.56f)
            curveTo(7.57f, 18.93f, 6.04f, 17.66f, 5.08f, 16.0f)
            close()
            moveTo(8.03f, 8.0f)
            horizontalLineTo(5.08f)
            curveToRelative(0.96f, -1.66f, 2.49f, -2.93f, 4.33f, -3.56f)
            curveTo(8.81f, 5.55f, 8.35f, 6.75f, 8.03f, 8.0f)
            close()
            moveTo(12.0f, 19.96f)
            curveToRelative(-0.83f, -1.2f, -1.48f, -2.53f, -1.91f, -3.96f)
            horizontalLineToRelative(3.82f)
            curveTo(13.48f, 17.43f, 12.83f, 18.76f, 12.0f, 19.96f)
            close()
            moveTo(14.34f, 14.0f)
            horizontalLineTo(9.66f)
            curveToRelative(-0.09f, -0.66f, -0.16f, -1.32f, -0.16f, -2.0f)
            curveToRelative(0.0f, -0.68f, 0.07f, -1.35f, 0.16f, -2.0f)
            horizontalLineToRelative(4.68f)
            curveToRelative(0.09f, 0.65f, 0.16f, 1.32f, 0.16f, 2.0f)
            curveTo(14.5f, 12.68f, 14.43f, 13.34f, 14.34f, 14.0f)
            close()
            moveTo(14.59f, 19.56f)
            curveToRelative(0.6f, -1.11f, 1.06f, -2.31f, 1.38f, -3.56f)
            horizontalLineToRelative(2.95f)
            curveTo(17.96f, 17.65f, 16.43f, 18.93f, 14.59f, 19.56f)
            close()
            moveTo(16.36f, 14.0f)
            curveToRelative(0.08f, -0.66f, 0.14f, -1.32f, 0.14f, -2.0f)
            curveToRelative(0.0f, -0.68f, -0.06f, -1.34f, -0.14f, -2.0f)
            horizontalLineToRelative(3.38f)
            curveToRelative(0.16f, 0.64f, 0.26f, 1.31f, 0.26f, 2.0f)
            reflectiveCurveToRelative(-0.1f, 1.36f, -0.26f, 2.0f)
            horizontalLineTo(16.36f)
            close()
        }
    }.also { _language = it }

private var _localFireDepartment: ImageVector? = null
val Icons.Filled.LocalFireDepartment: ImageVector
    get() = _localFireDepartment ?: materialIcon(name = "Filled.LocalFireDepartment") {
        materialPath {
            moveTo(12.0f, 12.9f)
            lineToRelative(-2.13f, 2.09f)
            curveTo(9.31f, 15.55f, 9.0f, 16.28f, 9.0f, 17.06f)
            curveTo(9.0f, 18.68f, 10.35f, 20.0f, 12.0f, 20.0f)
            reflectiveCurveToRelative(3.0f, -1.32f, 3.0f, -2.94f)
            curveToRelative(0.0f, -0.78f, -0.31f, -1.52f, -0.87f, -2.07f)
            lineTo(12.0f, 12.9f)
            close()
        }
        materialPath {
            moveTo(16.0f, 6.0f)
            lineToRelative(-0.44f, 0.55f)
            curveTo(14.38f, 8.02f, 12.0f, 7.19f, 12.0f, 5.3f)
            verticalLineTo(2.0f)
            curveToRelative(0.0f, 0.0f, -8.0f, 4.0f, -8.0f, 11.0f)
            curveToRelative(0.0f, 2.92f, 1.56f, 5.47f, 3.89f, 6.86f)
            curveTo(7.33f, 19.07f, 7.0f, 18.1f, 7.0f, 17.06f)
            curveToRelative(0.0f, -1.32f, 0.52f, -2.56f, 1.47f, -3.5f)
            lineTo(12.0f, 10.1f)
            lineToRelative(3.53f, 3.47f)
            curveToRelative(0.95f, 0.93f, 1.47f, 2.17f, 1.47f, 3.5f)
            curveToRelative(0.0f, 1.02f, -0.31f, 1.96f, -0.85f, 2.75f)
            curveToRelative(1.89f, -1.15f, 3.29f, -3.06f, 3.71f, -5.3f)
            curveTo(20.52f, 10.97f, 18.79f, 7.62f, 16.0f, 6.0f)
            close()
        }
    }.also { _localFireDepartment = it }

private var _locationOn: ImageVector? = null
val Icons.Filled.LocationOn: ImageVector
    get() = _locationOn ?: materialIcon(name = "Filled.LocationOn") {
        materialPath {
            moveTo(12.0f, 2.0f)
            curveTo(8.13f, 2.0f, 5.0f, 5.13f, 5.0f, 9.0f)
            curveToRelative(0.0f, 5.25f, 7.0f, 13.0f, 7.0f, 13.0f)
            reflectiveCurveToRelative(7.0f, -7.75f, 7.0f, -13.0f)
            curveTo(19.0f, 5.13f, 15.87f, 2.0f, 12.0f, 2.0f)
            close()
            moveTo(12.0f, 11.5f)
            curveToRelative(-1.38f, 0.0f, -2.5f, -1.12f, -2.5f, -2.5f)
            reflectiveCurveToRelative(1.12f, -2.5f, 2.5f, -2.5f)
            reflectiveCurveToRelative(2.5f, 1.12f, 2.5f, 2.5f)
            reflectiveCurveTo(13.38f, 11.5f, 12.0f, 11.5f)
            close()
        }
    }.also { _locationOn = it }

private var _openInBrowser: ImageVector? = null
val Icons.Filled.OpenInBrowser: ImageVector
    get() = _openInBrowser ?: materialIcon(name = "Filled.OpenInBrowser") {
        materialPath {
            moveTo(19.0f, 4.0f)
            horizontalLineTo(5.0f)
            curveTo(3.89f, 4.0f, 3.0f, 4.9f, 3.0f, 6.0f)
            verticalLineToRelative(12.0f)
            curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineTo(5.0f)
            verticalLineTo(8.0f)
            horizontalLineToRelative(14.0f)
            verticalLineToRelative(10.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(4.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineTo(6.0f)
            curveTo(21.0f, 4.9f, 20.1f, 4.0f, 19.0f, 4.0f)
            close()
            moveTo(12.0f, 10.0f)
            lineToRelative(-4.0f, 4.0f)
            horizontalLineToRelative(3.0f)
            verticalLineToRelative(6.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(-6.0f)
            horizontalLineToRelative(3.0f)
            lineTo(12.0f, 10.0f)
            close()
        }
    }.also { _openInBrowser = it }

private var _openInNew: ImageVector? = null
val Icons.Filled.OpenInNew: ImageVector
    get() = _openInNew ?: materialIcon(name = "Filled.OpenInNew") {
        materialPath {
            moveTo(19.0f, 19.0f)
            horizontalLineTo(5.0f)
            verticalLineTo(5.0f)
            horizontalLineToRelative(7.0f)
            verticalLineTo(3.0f)
            horizontalLineTo(5.0f)
            curveTo(3.89f, 3.0f, 3.0f, 3.9f, 3.0f, 5.0f)
            verticalLineToRelative(14.0f)
            curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(14.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineToRelative(-7.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineTo(19.0f)
            close()
            moveTo(14.0f, 3.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(3.59f)
            lineToRelative(-9.83f, 9.83f)
            lineToRelative(1.41f, 1.41f)
            lineTo(19.0f, 6.41f)
            verticalLineTo(10.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(3.0f)
            horizontalLineTo(14.0f)
            close()
        }
    }.also { _openInNew = it }

private var _pause: ImageVector? = null
val Icons.Filled.Pause: ImageVector
    get() = _pause ?: materialIcon(name = "Filled.Pause") {
        materialPath {
            moveTo(6.0f, 19.0f)
            horizontalLineToRelative(4.0f)
            verticalLineTo(5.0f)
            horizontalLineTo(6.0f)
            verticalLineTo(19.0f)
            close()
            moveTo(14.0f, 5.0f)
            verticalLineToRelative(14.0f)
            horizontalLineToRelative(4.0f)
            verticalLineTo(5.0f)
            horizontalLineTo(14.0f)
            close()
        }
    }.also { _pause = it }

private var _phoneAndroid: ImageVector? = null
val Icons.Filled.PhoneAndroid: ImageVector
    get() = _phoneAndroid ?: materialIcon(name = "Filled.PhoneAndroid") {
        materialPath {
            moveTo(16.0f, 1.0f)
            horizontalLineTo(8.0f)
            curveTo(6.34f, 1.0f, 5.0f, 2.34f, 5.0f, 4.0f)
            verticalLineToRelative(16.0f)
            curveToRelative(0.0f, 1.66f, 1.34f, 3.0f, 3.0f, 3.0f)
            horizontalLineToRelative(8.0f)
            curveToRelative(1.66f, 0.0f, 3.0f, -1.34f, 3.0f, -3.0f)
            verticalLineTo(4.0f)
            curveTo(19.0f, 2.34f, 17.66f, 1.0f, 16.0f, 1.0f)
            close()
            moveTo(14.0f, 21.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineToRelative(-1.0f)
            horizontalLineToRelative(4.0f)
            verticalLineTo(21.0f)
            close()
            moveTo(17.25f, 18.0f)
            horizontalLineTo(6.75f)
            verticalLineTo(4.0f)
            horizontalLineToRelative(10.5f)
            verticalLineTo(18.0f)
            close()
        }
    }.also { _phoneAndroid = it }

private var _photoCamera: ImageVector? = null
val Icons.Filled.PhotoCamera: ImageVector
    get() = _photoCamera ?: materialIcon(name = "Filled.PhotoCamera") {
        materialPath {
            moveTo(12.0f, 15.0f)
            curveToRelative(1.66f, 0.0f, 3.0f, -1.34f, 3.0f, -3.0f)
            reflectiveCurveToRelative(-1.34f, -3.0f, -3.0f, -3.0f)
            reflectiveCurveToRelative(-3.0f, 1.34f, -3.0f, 3.0f)
            reflectiveCurveTo(10.34f, 15.0f, 12.0f, 15.0f)
            close()
        }
        materialPath {
            moveTo(9.0f, 2.0f)
            lineTo(7.17f, 4.0f)
            horizontalLineTo(4.0f)
            curveTo(2.9f, 4.0f, 2.0f, 4.9f, 2.0f, 6.0f)
            verticalLineToRelative(12.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(16.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineTo(6.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            horizontalLineToRelative(-3.17f)
            lineTo(15.0f, 2.0f)
            horizontalLineTo(9.0f)
            close()
            moveTo(12.0f, 17.0f)
            curveToRelative(-2.76f, 0.0f, -5.0f, -2.24f, -5.0f, -5.0f)
            reflectiveCurveToRelative(2.24f, -5.0f, 5.0f, -5.0f)
            reflectiveCurveToRelative(5.0f, 2.24f, 5.0f, 5.0f)
            reflectiveCurveTo(14.76f, 17.0f, 12.0f, 17.0f)
            close()
        }
    }.also { _photoCamera = it }

private var _playCircle: ImageVector? = null
val Icons.Filled.PlayCircle: ImageVector
    get() = _playCircle ?: materialIcon(name = "Filled.PlayCircle") {
        materialPath {
            moveTo(12.0f, 2.0f)
            curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
            reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
            reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
            reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
            close()
            moveTo(9.5f, 16.5f)
            verticalLineToRelative(-9.0f)
            lineToRelative(7.0f, 4.5f)
            lineTo(9.5f, 16.5f)
            close()
        }
    }.also { _playCircle = it }

private var _print: ImageVector? = null
val Icons.Filled.Print: ImageVector
    get() = _print ?: materialIcon(name = "Filled.Print") {
        materialPath {
            moveTo(19.0f, 8.0f)
            horizontalLineTo(5.0f)
            curveTo(3.34f, 8.0f, 2.0f, 9.34f, 2.0f, 11.0f)
            verticalLineToRelative(6.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(4.0f)
            horizontalLineToRelative(12.0f)
            verticalLineToRelative(-4.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(-6.0f)
            curveTo(22.0f, 9.34f, 20.66f, 8.0f, 19.0f, 8.0f)
            close()
            moveTo(16.0f, 19.0f)
            horizontalLineTo(8.0f)
            verticalLineToRelative(-5.0f)
            horizontalLineToRelative(8.0f)
            verticalLineTo(19.0f)
            close()
            moveTo(19.0f, 12.0f)
            curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
            reflectiveCurveToRelative(0.45f, -1.0f, 1.0f, -1.0f)
            reflectiveCurveToRelative(1.0f, 0.45f, 1.0f, 1.0f)
            reflectiveCurveTo(19.55f, 12.0f, 19.0f, 12.0f)
            close()
            moveTo(18.0f, 3.0f)
            horizontalLineTo(6.0f)
            verticalLineToRelative(4.0f)
            horizontalLineToRelative(12.0f)
            verticalLineTo(3.0f)
            close()
        }
    }.also { _print = it }

private var _quiz: ImageVector? = null
val Icons.Filled.Quiz: ImageVector
    get() = _quiz ?: materialIcon(name = "Filled.Quiz") {
        materialPath {
            moveTo(4.0f, 6.0f)
            horizontalLineTo(2.0f)
            verticalLineToRelative(14.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(14.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineTo(4.0f)
            verticalLineTo(6.0f)
            close()
        }
        materialPath {
            moveTo(20.0f, 2.0f)
            horizontalLineTo(8.0f)
            curveTo(6.9f, 2.0f, 6.0f, 2.9f, 6.0f, 4.0f)
            verticalLineToRelative(12.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(12.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineTo(4.0f)
            curveTo(22.0f, 2.9f, 21.1f, 2.0f, 20.0f, 2.0f)
            close()
            moveTo(14.01f, 15.0f)
            curveToRelative(-0.59f, 0.0f, -1.05f, -0.47f, -1.05f, -1.05f)
            curveToRelative(0.0f, -0.59f, 0.47f, -1.04f, 1.05f, -1.04f)
            curveToRelative(0.59f, 0.0f, 1.05f, 0.45f, 1.05f, 1.04f)
            curveTo(15.06f, 14.53f, 14.6f, 15.0f, 14.01f, 15.0f)
            close()
            moveTo(16.51f, 8.83f)
            curveToRelative(-0.63f, 0.93f, -1.23f, 1.21f, -1.56f, 1.81f)
            curveToRelative(-0.13f, 0.24f, -0.18f, 0.4f, -0.18f, 1.18f)
            horizontalLineToRelative(-1.52f)
            curveToRelative(0.0f, -0.41f, -0.06f, -1.08f, 0.26f, -1.65f)
            curveToRelative(0.41f, -0.73f, 1.18f, -1.16f, 1.63f, -1.8f)
            curveToRelative(0.48f, -0.68f, 0.21f, -1.94f, -1.14f, -1.94f)
            curveToRelative(-0.88f, 0.0f, -1.32f, 0.67f, -1.5f, 1.23f)
            lineToRelative(-1.37f, -0.57f)
            curveTo(11.51f, 5.96f, 12.52f, 5.0f, 13.99f, 5.0f)
            curveToRelative(1.23f, 0.0f, 2.08f, 0.56f, 2.51f, 1.26f)
            curveTo(16.87f, 6.87f, 17.08f, 7.99f, 16.51f, 8.83f)
            close()
        }
    }.also { _quiz = it }

private var _replay10: ImageVector? = null
val Icons.Filled.Replay10: ImageVector
    get() = _replay10 ?: materialIcon(name = "Filled.Replay10") {
        materialPath {
            moveTo(11.99f, 5.0f)
            verticalLineTo(1.0f)
            lineToRelative(-5.0f, 5.0f)
            lineToRelative(5.0f, 5.0f)
            verticalLineTo(7.0f)
            curveToRelative(3.31f, 0.0f, 6.0f, 2.69f, 6.0f, 6.0f)
            reflectiveCurveToRelative(-2.69f, 6.0f, -6.0f, 6.0f)
            reflectiveCurveToRelative(-6.0f, -2.69f, -6.0f, -6.0f)
            horizontalLineTo(4.0f)
            curveToRelative(0.0f, 4.42f, 3.58f, 8.0f, 8.0f, 8.0f)
            reflectiveCurveToRelative(8.0f, -3.58f, 8.0f, -8.0f)
            curveTo(20.0f, 8.58f, 16.42f, 5.0f, 11.99f, 5.0f)
            close()
        }
    }.also { _replay10 = it }

private var _schedule: ImageVector? = null
val Icons.Filled.Schedule: ImageVector
    get() = _schedule ?: materialIcon(name = "Filled.Schedule") {
        materialPath {
            moveTo(11.99f, 2.0f)
            curveTo(6.47f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
            reflectiveCurveToRelative(4.47f, 10.0f, 9.99f, 10.0f)
            curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f)
            reflectiveCurveTo(17.52f, 2.0f, 11.99f, 2.0f)
            close()
            moveTo(12.0f, 20.0f)
            curveToRelative(-4.42f, 0.0f, -8.0f, -3.58f, -8.0f, -8.0f)
            reflectiveCurveToRelative(3.58f, -8.0f, 8.0f, -8.0f)
            reflectiveCurveToRelative(8.0f, 3.58f, 8.0f, 8.0f)
            reflectiveCurveTo(16.42f, 20.0f, 12.0f, 20.0f)
            close()
            moveTo(12.5f, 7.0f)
            horizontalLineTo(11.0f)
            verticalLineToRelative(6.0f)
            lineToRelative(5.25f, 3.15f)
            lineToRelative(0.75f, -1.23f)
            lineToRelative(-4.5f, -2.67f)
            verticalLineTo(7.0f)
            close()
        }
    }.also { _schedule = it }

private var _school: ImageVector? = null
val Icons.Filled.School: ImageVector
    get() = _school ?: materialIcon(name = "Filled.School") {
        materialPath {
            moveTo(5.0f, 13.18f)
            verticalLineToRelative(4.0f)
            lineTo(12.0f, 21.0f)
            lineToRelative(7.0f, -3.82f)
            verticalLineToRelative(-4.0f)
            lineTo(12.0f, 17.0f)
            lineTo(5.0f, 13.18f)
            close()
            moveTo(12.0f, 3.0f)
            lineTo(1.0f, 9.0f)
            lineToRelative(11.0f, 6.0f)
            lineToRelative(9.0f, -4.91f)
            verticalLineTo(17.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(9.0f)
            lineTo(12.0f, 3.0f)
            close()
        }
    }.also { _school = it }

private var _security: ImageVector? = null
val Icons.Filled.Security: ImageVector
    get() = _security ?: materialIcon(name = "Filled.Security") {
        materialPath {
            moveTo(12.0f, 1.0f)
            lineTo(3.0f, 5.0f)
            verticalLineToRelative(6.0f)
            curveToRelative(0.0f, 5.55f, 3.84f, 10.74f, 9.0f, 12.0f)
            curveToRelative(5.16f, -1.26f, 9.0f, -6.45f, 9.0f, -12.0f)
            verticalLineTo(5.0f)
            lineTo(12.0f, 1.0f)
            close()
            moveTo(12.0f, 11.99f)
            horizontalLineToRelative(7.0f)
            curveToRelative(-0.53f, 4.12f, -3.28f, 7.79f, -7.0f, 8.94f)
            verticalLineTo(12.0f)
            horizontalLineTo(5.0f)
            verticalLineTo(6.3f)
            lineToRelative(7.0f, -3.11f)
            verticalLineTo(11.99f)
            close()
        }
    }.also { _security = it }

private var _shield: ImageVector? = null
val Icons.Filled.Shield: ImageVector
    get() = _shield ?: materialIcon(name = "Filled.Shield") {
        materialPath {
            moveTo(12.0f, 1.0f)
            lineTo(3.0f, 5.0f)
            verticalLineToRelative(6.0f)
            curveToRelative(0.0f, 5.55f, 3.84f, 10.74f, 9.0f, 12.0f)
            curveToRelative(5.16f, -1.26f, 9.0f, -6.45f, 9.0f, -12.0f)
            verticalLineTo(5.0f)
            lineTo(12.0f, 1.0f)
            close()
        }
    }.also { _shield = it }

private var _swapHoriz: ImageVector? = null
val Icons.Filled.SwapHoriz: ImageVector
    get() = _swapHoriz ?: materialIcon(name = "Filled.SwapHoriz") {
        materialPath {
            moveTo(6.99f, 11.0f)
            lineTo(3.0f, 15.0f)
            lineToRelative(3.99f, 4.0f)
            verticalLineToRelative(-3.0f)
            horizontalLineTo(14.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineTo(6.99f)
            verticalLineTo(11.0f)
            close()
            moveTo(21.0f, 9.0f)
            lineToRelative(-3.99f, -4.0f)
            verticalLineToRelative(3.0f)
            horizontalLineTo(10.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(7.01f)
            verticalLineToRelative(3.0f)
            lineTo(21.0f, 9.0f)
            close()
        }
    }.also { _swapHoriz = it }

private var _switchAccount: ImageVector? = null
val Icons.Filled.SwitchAccount: ImageVector
    get() = _switchAccount ?: materialIcon(name = "Filled.SwitchAccount") {
        materialPath {
            moveTo(4.0f, 6.0f)
            horizontalLineTo(2.0f)
            verticalLineToRelative(14.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(14.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineTo(4.0f)
            verticalLineTo(6.0f)
            close()
            moveTo(20.0f, 2.0f)
            horizontalLineTo(8.0f)
            curveTo(6.9f, 2.0f, 6.0f, 2.9f, 6.0f, 4.0f)
            verticalLineToRelative(12.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(12.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineTo(4.0f)
            curveTo(22.0f, 2.9f, 21.1f, 2.0f, 20.0f, 2.0f)
            close()
            moveTo(14.0f, 5.0f)
            curveToRelative(1.66f, 0.0f, 3.0f, 1.34f, 3.0f, 3.0f)
            curveToRelative(0.0f, 1.66f, -1.34f, 3.0f, -3.0f, 3.0f)
            reflectiveCurveToRelative(-3.0f, -1.34f, -3.0f, -3.0f)
            curveTo(11.0f, 6.34f, 12.34f, 5.0f, 14.0f, 5.0f)
            close()
            moveTo(7.76f, 16.0f)
            curveToRelative(1.47f, -1.83f, 3.71f, -3.0f, 6.24f, -3.0f)
            reflectiveCurveToRelative(4.77f, 1.17f, 6.24f, 3.0f)
            horizontalLineTo(7.76f)
            close()
        }
    }.also { _switchAccount = it }

private var _systemUpdate: ImageVector? = null
val Icons.Filled.SystemUpdate: ImageVector
    get() = _systemUpdate ?: materialIcon(name = "Filled.SystemUpdate") {
        materialPath {
            moveTo(17.0f, 1.01f)
            lineTo(7.0f, 1.0f)
            curveTo(5.9f, 1.0f, 5.0f, 1.9f, 5.0f, 3.0f)
            verticalLineToRelative(18.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(10.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineTo(3.0f)
            curveTo(19.0f, 1.9f, 18.1f, 1.01f, 17.0f, 1.01f)
            close()
            moveTo(17.0f, 19.0f)
            horizontalLineTo(7.0f)
            verticalLineTo(5.0f)
            horizontalLineToRelative(10.0f)
            verticalLineTo(19.0f)
            close()
            moveTo(16.0f, 13.0f)
            horizontalLineToRelative(-3.0f)
            verticalLineTo(8.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineToRelative(5.0f)
            horizontalLineTo(8.0f)
            lineToRelative(4.0f, 4.0f)
            lineTo(16.0f, 13.0f)
            close()
        }
    }.also { _systemUpdate = it }

private var _timer: ImageVector? = null
val Icons.Filled.Timer: ImageVector
    get() = _timer ?: Icons.Filled.Alarm.also { _timer = it }

private var _verified: ImageVector? = null
val Icons.Filled.Verified: ImageVector
    get() = _verified ?: materialIcon(name = "Filled.Verified") {
        materialPath {
            moveTo(23.0f, 12.0f)
            lineToRelative(-2.44f, -2.79f)
            lineToRelative(0.34f, -3.69f)
            lineToRelative(-3.61f, -0.82f)
            lineTo(15.4f, 1.5f)
            lineTo(12.0f, 2.96f)
            lineTo(8.6f, 1.5f)
            lineTo(6.71f, 4.69f)
            lineTo(3.1f, 5.5f)
            lineTo(3.44f, 9.2f)
            lineTo(1.0f, 12.0f)
            lineToRelative(2.44f, 2.79f)
            lineToRelative(-0.34f, 3.7f)
            lineToRelative(3.61f, 0.82f)
            lineTo(8.6f, 22.5f)
            lineToRelative(3.4f, -1.47f)
            lineToRelative(3.4f, 1.46f)
            lineToRelative(1.89f, -3.19f)
            lineToRelative(3.61f, -0.82f)
            lineToRelative(-0.34f, -3.69f)
            lineTo(23.0f, 12.0f)
            close()
            moveTo(10.09f, 16.72f)
            lineToRelative(-3.8f, -3.81f)
            lineToRelative(1.48f, -1.48f)
            lineToRelative(2.32f, 2.33f)
            lineToRelative(5.85f, -5.87f)
            lineToRelative(1.48f, 1.48f)
            lineTo(10.09f, 16.72f)
            close()
        }
    }.also { _verified = it }

private var _videocam: ImageVector? = null
val Icons.Filled.Videocam: ImageVector
    get() = _videocam ?: materialIcon(name = "Filled.Videocam") {
        materialPath {
            moveTo(17.0f, 10.5f)
            verticalLineTo(7.0f)
            curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
            horizontalLineTo(4.0f)
            curveTo(3.45f, 6.0f, 3.0f, 6.45f, 3.0f, 7.0f)
            verticalLineToRelative(10.0f)
            curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
            horizontalLineToRelative(12.0f)
            curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
            verticalLineToRelative(-3.5f)
            lineToRelative(4.0f, 4.0f)
            verticalLineToRelative(-11.0f)
            lineTo(17.0f, 10.5f)
            close()
        }
    }.also { _videocam = it }

private var _visibility: ImageVector? = null
val Icons.Filled.Visibility: ImageVector
    get() = _visibility ?: materialIcon(name = "Filled.Visibility") {
        materialPath {
            moveTo(12.0f, 4.5f)
            curveTo(7.0f, 4.5f, 2.73f, 7.61f, 1.0f, 12.0f)
            curveToRelative(1.73f, 4.39f, 6.0f, 7.5f, 11.0f, 7.5f)
            reflectiveCurveToRelative(9.27f, -3.11f, 11.0f, -7.5f)
            curveTo(21.27f, 7.61f, 17.0f, 4.5f, 12.0f, 4.5f)
            close()
            moveTo(12.0f, 17.0f)
            curveToRelative(-2.76f, 0.0f, -5.0f, -2.24f, -5.0f, -5.0f)
            reflectiveCurveToRelative(2.24f, -5.0f, 5.0f, -5.0f)
            reflectiveCurveToRelative(5.0f, 2.24f, 5.0f, 5.0f)
            reflectiveCurveTo(14.76f, 17.0f, 12.0f, 17.0f)
            close()
            moveTo(12.0f, 9.0f)
            curveToRelative(-1.66f, 0.0f, -3.0f, 1.34f, -3.0f, 3.0f)
            reflectiveCurveToRelative(1.34f, 3.0f, 3.0f, 3.0f)
            reflectiveCurveToRelative(3.0f, -1.34f, 3.0f, -3.0f)
            reflectiveCurveTo(13.66f, 9.0f, 12.0f, 9.0f)
            close()
        }
    }.also { _visibility = it }

private var _visibilityOff: ImageVector? = null
val Icons.Filled.VisibilityOff: ImageVector
    get() = _visibilityOff ?: materialIcon(name = "Filled.VisibilityOff") {
        materialPath {
            moveTo(12.0f, 7.0f)
            curveToRelative(2.76f, 0.0f, 5.0f, 2.24f, 5.0f, 5.0f)
            curveToRelative(0.0f, 0.65f, -0.13f, 1.26f, -0.36f, 1.83f)
            lineToRelative(2.92f, 2.92f)
            curveToRelative(1.51f, -1.26f, 2.7f, -2.89f, 3.43f, -4.75f)
            curveToRelative(-1.73f, -4.39f, -6.0f, -7.5f, -11.0f, -7.5f)
            curveToRelative(-1.4f, 0.0f, -2.74f, 0.25f, -3.98f, 0.7f)
            lineToRelative(2.16f, 2.16f)
            curveTo(10.74f, 7.13f, 11.35f, 7.0f, 12.0f, 7.0f)
            close()
            moveTo(2.0f, 4.27f)
            lineToRelative(2.28f, 2.28f)
            lineToRelative(0.46f, 0.46f)
            curveTo(3.08f, 8.3f, 1.78f, 10.02f, 1.0f, 12.0f)
            curveToRelative(1.73f, 4.39f, 6.0f, 7.5f, 11.0f, 7.5f)
            curveToRelative(1.55f, 0.0f, 3.03f, -0.3f, 4.38f, -0.84f)
            lineToRelative(0.42f, 0.42f)
            lineTo(19.73f, 22.0f)
            lineTo(21.0f, 20.73f)
            lineTo(3.27f, 3.0f)
            lineTo(2.0f, 4.27f)
            close()
        }
    }.also { _visibilityOff = it }

private var _workOutline: ImageVector? = null
val Icons.Filled.WorkOutline: ImageVector
    get() = _workOutline ?: materialIcon(name = "Filled.WorkOutline") {
        materialPath {
            moveTo(14.0f, 6.0f)
            verticalLineTo(4.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineToRelative(2.0f)
            horizontalLineTo(14.0f)
            close()
            moveTo(4.0f, 8.0f)
            verticalLineToRelative(11.0f)
            horizontalLineToRelative(16.0f)
            verticalLineTo(8.0f)
            horizontalLineTo(4.0f)
            close()
            moveTo(20.0f, 6.0f)
            curveToRelative(1.11f, 0.0f, 2.0f, 0.89f, 2.0f, 2.0f)
            verticalLineToRelative(11.0f)
            curveToRelative(0.0f, 1.11f, -0.89f, 2.0f, -2.0f, 2.0f)
            horizontalLineTo(4.0f)
            curveToRelative(-1.11f, 0.0f, -2.0f, -0.89f, -2.0f, -2.0f)
            lineTo(2.01f, 8.0f)
            curveToRelative(0.0f, -1.11f, 0.88f, -2.0f, 1.99f, -2.0f)
            horizontalLineToRelative(4.0f)
            verticalLineTo(4.0f)
            curveToRelative(0.0f, -1.11f, 0.89f, -2.0f, 2.0f, -2.0f)
            horizontalLineToRelative(4.0f)
            curveToRelative(1.11f, 0.0f, 2.0f, 0.89f, 2.0f, 2.0f)
            verticalLineToRelative(2.0f)
            horizontalLineTo(20.0f)
            close()
        }
    }.also { _workOutline = it }
