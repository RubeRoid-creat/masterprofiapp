package com.bestapp.client.ui.reports

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream
import android.util.Base64 as AndroidBase64

@Composable
fun SignatureCanvas(
    modifier: Modifier = Modifier,
    onSignatureChanged: (String?) -> Unit
) {
    val paths = remember { mutableStateListOf<PathState>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentPathState by remember { mutableStateOf<PathState?>(null) }
    
    val strokeWidth = 4f
    val strokeColor = Color.Black
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val path = Path()
                        path.moveTo(offset.x, offset.y)
                        currentPath = path
                        currentPathState = PathState(path, strokeColor, strokeWidth)
                    },
                    onDrag = { change, _ ->
                        currentPath?.lineTo(change.position.x, change.position.y)
                    },
                    onDragEnd = {
                        currentPath?.let { path ->
                            currentPathState?.let { pathState ->
                                paths.add(pathState)
                                currentPath = null
                                currentPathState = null
                                
                                // Генерируем Base64 подпись
                                onSignatureChanged(generateSignatureBase64(paths))
                            }
                        }
                    }
                )
            }
    ) {
        // Рисуем все пути
        paths.forEach { pathState ->
            drawPath(
                path = pathState.path,
                color = pathState.color,
                style = Stroke(
                    width = pathState.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
        
        // Рисуем текущий путь
        currentPath?.let { path ->
            currentPathState?.let { pathState ->
                drawPath(
                    path = path,
                    color = pathState.color,
                    style = Stroke(
                        width = pathState.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

private fun generateSignatureBase64(paths: List<PathState>): String? {
    if (paths.isEmpty()) return null
    
    return try {
        // Создаем Bitmap
        val bitmap = Bitmap.createBitmap(800, 300, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        
        // Рисуем белый фон
        canvas.drawColor(android.graphics.Color.WHITE)
        
        // Рисуем все пути
        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 4f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        
        paths.forEach { pathState ->
            val androidPath = pathState.path.asAndroidPath()
            canvas.drawPath(androidPath, paint)
        }
        
        // Конвертируем в Base64
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        AndroidBase64.encodeToString(byteArray, AndroidBase64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}

data class PathState(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)

fun clearSignature(paths: MutableList<PathState>, onSignatureChanged: (String?) -> Unit) {
    paths.clear()
    onSignatureChanged(null)
}

