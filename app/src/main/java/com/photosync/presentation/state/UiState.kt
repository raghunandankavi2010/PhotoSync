package com.photosync.presentation.state

/**
 * Base UI state sealed class for MVI pattern
 */
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

/**
 * Extension to check if state is success
 */
fun <T> UiState<T>.isSuccess(): Boolean = this is UiState.Success

/**
 * Extension to check if state is loading
 */
fun <T> UiState<T>.isLoading(): Boolean = this is UiState.Loading

/**
 * Extension to check if state is error
 */
fun <T> UiState<T>.isError(): Boolean = this is UiState.Error

/**
 * Extension to get success data or null
 */
fun <T> UiState<T>.getOrNull(): T? = (this as? UiState.Success)?.data

/**
 * Extension to get error message or null
 */
fun <T> UiState<T>.errorOrNull(): String? = (this as? UiState.Error)?.message
