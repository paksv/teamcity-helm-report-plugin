package jetbrains.buildServer.helmReport.beans

import jetbrains.buildServer.helmReport.HelmDiffFeatureConstants

class ProtectedResourcesBean {
    val key = HelmDiffFeatureConstants.FEATURE_SETTING_PROTECTED_RESOURCES
    val label = "Protected resource types:"
    val description = "Create build problem if any resource type matching Java regex pattern is planned for destroy"
}