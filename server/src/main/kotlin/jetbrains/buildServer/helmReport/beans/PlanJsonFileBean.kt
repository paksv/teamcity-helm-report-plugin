package jetbrains.buildServer.helmReport.beans

import jetbrains.buildServer.helmReport.HelmDiffFeatureConstants

class PlanJsonFileBean {
    val key = HelmDiffFeatureConstants.FEATURE_SETTING_PLAN_JSON_FILE
    val label = "Plan changes file:"
    val description = "Relative path to the JSON file summarizing the planned changes (i.e. output.json)"
}