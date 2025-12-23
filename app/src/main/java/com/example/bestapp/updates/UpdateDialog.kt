package com.example.bestapp.updates

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import com.example.bestapp.R

/**
 * Диалог обновления приложения
 */
object UpdateDialog {

    /**
     * Показать диалог доступного обновления
     */
    fun showUpdateAvailableDialog(
        context: Context,
        updateInfo: UpdateInfo,
        onUpdate: () -> Unit,
        onLater: (() -> Unit)? = null
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Доступно обновление")
        builder.setMessage(
            """
            Новая версия: ${updateInfo.newVersion}
            Текущая версия: ${updateInfo.currentVersion}
            
            Что нового:
            ${updateInfo.releaseNotes}
            """.trimIndent()
        )

        builder.setPositiveButton("Обновить") { dialog, _ ->
            onUpdate()
            dialog.dismiss()
        }

        if (!updateInfo.forceUpdate) {
            // Если не принудительное обновление, показываем кнопку "Позже"
            builder.setNegativeButton("Позже") { dialog, _ ->
                onLater?.invoke()
                dialog.dismiss()
            }
            builder.setCancelable(true)
        } else {
            // Принудительное обновление - нельзя закрыть диалог
            builder.setCancelable(false)
        }

        builder.show()
    }

    /**
     * Показать диалог с прогрессом загрузки
     */
    fun showDownloadProgressDialog(
        context: Context,
        onCancel: (() -> Unit)? = null
    ): AlertDialog {
        val dialogView = LayoutInflater.from(context).inflate(
            android.R.layout.simple_list_item_1, // Временно используем стандартный layout
            null
        )

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Загрузка обновления")
        builder.setView(dialogView)
        builder.setCancelable(false)

        if (onCancel != null) {
            builder.setNegativeButton("Отмена") { dialog, _ ->
                onCancel()
                dialog.dismiss()
            }
        }

        return builder.create()
    }

    /**
     * Показать диалог ошибки
     */
    fun showErrorDialog(
        context: Context,
        message: String,
        onRetry: (() -> Unit)? = null
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Ошибка обновления")
        builder.setMessage(message)

        if (onRetry != null) {
            builder.setPositiveButton("Повторить") { dialog, _ ->
                onRetry()
                dialog.dismiss()
            }
        }

        builder.setNegativeButton("Закрыть") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    /**
     * Показать диалог "Приложение обновлено"
     */
    fun showUpToDateDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Все актуально")
        builder.setMessage("Вы используете последнюю версию приложения")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }
}
