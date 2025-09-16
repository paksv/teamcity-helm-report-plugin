package jetbrains.buildServer.helmReport

import jetbrains.buildServer.helmReport.HelmDiffFeatureConstants as FeatureConst

class HelmDiffFeatureConfiguration(private val properties: Map<String, String>) {
    fun isReportEnabled(): Boolean {
        return !getPlanJsonFile().isNullOrEmpty()
    }

    fun getPlanJsonFile(): String? {
        return properties[FeatureConst.FEATURE_SETTING_PLAN_JSON_FILE]
    }

    fun hasProtectedResourcePattern(): Boolean {
        return !getProtectedResourcePattern().isNullOrEmpty()
    }

    fun getProtectedResourcePattern(): String? {
        return properties[FeatureConst.FEATURE_SETTING_PROTECTED_RESOURCES]
    }

    fun updateBuildStatus(): Boolean {
        return properties[FeatureConst.FEATURE_SETTING_UPDATE_BUILD_STATUS].toBoolean()
    }
}