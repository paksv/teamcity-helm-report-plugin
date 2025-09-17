package jetbrains.buildServer.helmReport.beans

import jetbrains.buildServer.helmReport.HelmDiffFeatureConstants

class UpdateBuildStatusBean {
    val key = HelmDiffFeatureConstants.FEATURE_SETTING_UPDATE_BUILD_STATUS
    val label = "Update build status:"
    val description = "Update build status with diff summary"
}