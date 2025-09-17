package jetbrains.buildServer.helmReport.jsonOutput.model

import jetbrains.buildServer.helmReport.report.ChangeItemBackground

data class HelmPlanData(
    val fileName: String,
    val changes: List<HelmChange>
) {
}