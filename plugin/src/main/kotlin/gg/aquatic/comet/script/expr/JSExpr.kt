package gg.aquatic.comet.script.expr

import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.macro.Macro
import javax.script.Compilable
import javax.script.CompiledScript

class JSExpr<T>(
    val compiled: CompiledScript
) : Expr<T> {
    override fun eval(): Result<T> {
        return try {
            Result.success(compiled.eval() as T)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun <T> String.constructExpr(
            engine: Compilable,
            macros: Map<String, Macro>?,
            tryAsSimpleString: Boolean = false
        ): Result<JSExpr<T>> {
            val compiled = engine.compile(this, macros, tryAsSimpleString)
            return compiled.fold(
                { Result.success(JSExpr(it)) },
                { Result.failure(it) }
            )
        }
    }
}