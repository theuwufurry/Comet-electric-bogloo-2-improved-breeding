package gg.aquatic.comet.api.emitter.action

abstract class AbstractAction {

    abstract val subActions: List<SubAction>
    abstract fun execute(context: ActionContext)

}

interface SubAction {
    fun execute(context: ActionContext)
}