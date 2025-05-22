package uz.ibrohim.food.state

data class State(
    var step: Step = Step.NONE,
    var tempNumber: String? = null
)
