package gg.aquatic.comet.emitter.optimization.Mengshe

/**
 * @param tpDCost We first reduce position update cost, which may or may not align with display updates, thus we have no way to know if we will actually pay the extra cost for needing to send a metadata packet. Let's just... guess
 */
data class Costs(
    val tpCost: Int,
    val tpDCost: Int,
    val updateCost: Int,
) {
    companion object {
        const val BASE_METADATA_COST = 3
        const val VEC3_COST = 14
        const val QUATERNION_COST = 18
        const val COMPONENT_COST = 32
        const val BYTE_COST = 3
        const val INT_COST = 6
        
        val DEFAULT = Costs(
            tpCost = 66,
            tpDCost = BASE_METADATA_COST + INT_COST,
            updateCost = BASE_METADATA_COST + VEC3_COST * 2 + QUATERNION_COST + 2 * INT_COST + COMPONENT_COST + BYTE_COST,
        )
    }
}