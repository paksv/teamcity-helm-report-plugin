package jetbrains.buildServer.helmReport.jsonOutput.model

import jetbrains.buildServer.helmReport.jsonOutput.ParsingUtil

data class HelmPlanData(
    val fileName: String,
    val changes: List<HelmChange>,
    val jsonData: String
) {
    constructor(fileName: String, changes: List<HelmChange>) :
            this(fileName, changes, jsonStrFromChanges(changes))
}

private fun jsonStrFromChanges(changes: List<HelmChange>) : String {
    val objectMapper = ParsingUtil.getObjectMapper()
    return objectMapper.writeValueAsString(changes)
}
