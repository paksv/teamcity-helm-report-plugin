package jetbrains.buildServer.helmReport.jsonOutput.model

import jetbrains.buildServer.helmReport.report.ChangeItemBackground

class HelmPlanData(
    val fileName: String,
    val changes: List<HelmChange>
) {
    val hasChangedResources: Boolean
        get() {
            return changes.any { it.isModify }
        }


    val changedResources: List<HelmChange>
        get() {
            return changes.filter { it.isModify }
        }


    val getHasChangedOutputValues = false

    val changedOutputValues: Map<String, HelmChange>
        get() {
            return emptyMap()
        }

}