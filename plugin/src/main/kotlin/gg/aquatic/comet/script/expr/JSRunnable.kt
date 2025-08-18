package gg.aquatic.comet.script.expr

import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.macro.Macro
import javax.script.Compilable
import javax.script.CompiledScript

class JSRunnable(
    val compiled: CompiledScript
) : Runnable {
    override fun run() {
        compiled.eval()
    }

    companion object {
        fun String.constructRunnable(
            engine: Compilable,
            macros: Map<String, Macro>?,
            tryAsSimpleString: Boolean = false
        ): Result<JSRunnable> {
            val compiled = engine.compile(this, macros, tryAsSimpleString)
            return compiled.fold(
                { Result.success(JSRunnable(it)) },
                { Result.failure(it) }
            )
        }
    }
}
