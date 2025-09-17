package jetbrains.buildServer.helmReport.jsonOutput.model

import jetbrains.buildServer.helmReport.HelmDiffSupport.Companion.updateBuildStatus
import jetbrains.buildServer.helmReport.jsonOutput.ParsingUtil

data class HelmPlanData(
    val fileName: String,
    val changes: List<HelmChange>,
    val jsonData: String
) {
    private val knownChangeTypes = mapOf(
        "ADD" to "add",
        "MODIFY" to "change",
        "REMOVE" to "destroy",
        "OWNERSHIP" to "ownership"
    )

    constructor(fileName: String, changes: List<HelmChange>) :
            this(fileName, changes, jsonStrFromChanges(changes))

    fun summary(): String {
        if (changes.isEmpty()) {
            return "No resource changes are detected"
        }
        val changesMap = changes.groupBy { it.change }
        val statusBuilder = StringBuilder()
        knownChangeTypes.forEach {
            if (!changesMap[it.key].isNullOrEmpty()) {
                statusBuilder.append(", ${changesMap[it.key]?.size} to ${it.value}")
            }
        }
        changesMap.filter { !knownChangeTypes.contains(it.key) }.forEach {
            statusBuilder.append(",${it.value.size} to ${it.key.toLowerCase()}")
        }
        return statusBuilder.substring(2)
    }

}

private fun jsonStrFromChanges(changes: List<HelmChange>) : String {
    val objectMapper = ParsingUtil.getObjectMapper()
    return objectMapper.writeValueAsString(changes)
}
