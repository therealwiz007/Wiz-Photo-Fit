package com.wizlabs.photofit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf

/** Every screen of the app. The route string keys predictive-back peeks and transitions. */
sealed interface Screen {
    val route: String
    data object Home : Screen { override val route = "home" }
    data object ExamList : Screen { override val route = "exams" }
    data class ExamDetail(val presetId: String) : Screen { override val route = "exam:$presetId" }
    data class Editor(val presetId: String, val part: Int) : Screen { override val route = "edit:$presetId:$part" }
    data object Result : Screen { override val route = "result" }
    data object IdList : Screen { override val route = "ids" }
    data object Compress : Screen { override val route = "compress" }
    data object Signature : Screen { override val route = "signature" }
    data object Joiner : Screen { override val route = "joiner" }
    data object Saved : Screen { override val route = "saved" }
    data object About : Screen { override val route = "about" }
    data class Builder(val editId: String? = null, val copyFrom: String? = null) : Screen { override val route = "builder:$editId:$copyFrom" }
    data class Import(val code: String? = null) : Screen { override val route = "import" }
}

/** A tiny back stack: push to go deeper, pop to go back. Depth drives the slide direction. */
@Stable
class Nav {
    val stack = mutableStateListOf<Screen>(Screen.Home)
    var forward = true
        private set
    val top: Screen get() = stack.last()
    val canPop: Boolean get() = stack.size > 1

    fun push(s: Screen) { forward = true; stack.add(s) }
    fun pop() { if (canPop) { forward = false; stack.removeAt(stack.lastIndex) } }
    fun popTo(s: Screen) { forward = false; while (stack.size > 1 && stack.last() != s) stack.removeAt(stack.lastIndex) }
    /** Replace the top screen (e.g. Editor -> Result) so back skips the replaced one. */
    fun replace(s: Screen) { forward = true; stack[stack.lastIndex] = s }
}
