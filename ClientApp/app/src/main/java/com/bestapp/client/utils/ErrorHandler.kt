package com.bestapp.client.utils

import android.content.Context
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Централизованная обработка ошибок
 */
object ErrorHandler {

    /**
     * Преобразует исключение в понятное пользователю сообщение
     */
    fun getErrorMessage(throwable: Throwable, context: Context? = null): String {
        return when (throwable) {
            is HttpException -> getHttpErrorMessage(throwable)
            is SocketTimeoutException -> "Превышено время ожидания. Проверьте соединение с интернетом."
            is UnknownHostException -> "Не удается подключиться к серверу. Проверьте соединение с интернетом."
            is IOException -> "Ошибка сети. Проверьте подключение к интернету."
            else -> throwable.message ?: "Произошла неизвестная ошибка"
        }
    }

    /**
     * Обработка HTTP ошибок
     */
    private fun getHttpErrorMessage(exception: HttpException): String {
        return when (exception.code()) {
            400 -> "Неверный запрос. Проверьте введенные данные."
            401 -> "Необходима авторизация. Войдите в систему."
            403 -> "Доступ запрещен. У вас нет прав для этого действия."
            404 -> "Ресурс не найден. Попробуйте обновить данные."
            408 -> "Превышено время ожидания запроса."
            500 -> "Ошибка сервера. Попробуйте позже."
            502 -> "Сервер временно недоступен."
            503 -> "Сервис временно недоступен. Попробуйте позже."
            else -> "Ошибка HTTP ${exception.code()}: ${exception.message()}"
        }
    }

    /**
     * Проверка, является ли ошибка сетевой
     */
    fun isNetworkError(throwable: Throwable): Boolean {
        return throwable is IOException ||
               throwable is SocketTimeoutException ||
               throwable is UnknownHostException
    }

    /**
     * Проверка, требует ли ошибка повторной авторизации
     */
    fun requiresReauth(throwable: Throwable): Boolean {
        return throwable is HttpException && throwable.code() == 401
    }

    /**
     * Обработка ошибки с автоматическим логированием
     */
    fun handleError(
        throwable: Throwable,
        context: Context? = null,
        tag: String = "ErrorHandler",
        onError: ((String) -> Unit)? = null
    ): String {
        val message = getErrorMessage(throwable, context)
        
        // Логирование ошибки
        android.util.Log.e(tag, "Error occurred: ${throwable.message}", throwable)
        
        // Вызов callback если есть
        onError?.invoke(message)
        
        return message
    }
}

/**
 * Wrapper для Result с обработкой ошибок
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

/**
 * Extension функция для безопасного выполнения API вызовов
 */
suspend fun <T> safeApiCall(
    context: Context? = null,
    apiCall: suspend () -> T
): ApiResult<T> {
    return try {
        ApiResult.Success(apiCall())
    } catch (e: Exception) {
        val message = ErrorHandler.getErrorMessage(e, context)
        ApiResult.Error(message, e)
    }
}
