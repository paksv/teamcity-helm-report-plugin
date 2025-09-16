package jetbrains.buildServer.helmReport

object HelmDiffFeatureConstants {
    // plugin-level data
    const val FEATURE_DISPLAY_NAME = "Helm Diff Reports"
    const val FEATURE_TYPE = "helm-diff-reports"

    // feature parameters bean data
    const val FEATURE_SETTING_PLAN_JSON_FILE = "planJsonFile"
    const val FEATURE_SETTING_UPDATE_BUILD_STATUS = "updateBuildStatus"
    const val FEATURE_SETTING_PROTECTED_RESOURCES = "protectedResources"

    // internal properties
    const val REPORT_TEMPLATE_FILE = "helmDiffReportTemplate.peb"
    const val REPORT_RESOURCE_FOLDER_PATH = "buildAgentResources"

    const val HIDDEN_ARTIFACT_REPORT_FILENAME = "helmDiffReport.html"
    const val HIDDEN_ARTIFACT_REPORT_FOLDER = ".teamcity/helmDiff"
}

