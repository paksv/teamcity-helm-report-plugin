package jetbrains.buildServer.helmReport

import jetbrains.buildServer.serverSide.BuildFeature
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.helmReport.HelmDiffFeatureConstants as CommonConst

class HelmSupportBuildFeature(descriptor: PluginDescriptor) : BuildFeature() {
    private val myEditUrl = descriptor.getPluginResourcesPath(
        "editHelmReportSettings.jsp"
    )

    override fun getType(): String = CommonConst.FEATURE_TYPE

    override fun getDisplayName(): String = CommonConst.FEATURE_DISPLAY_NAME

    override fun getEditParametersUrl(): String? = myEditUrl

    override fun isMultipleFeaturesPerBuildTypeAllowed(): Boolean = false

    override fun describeParameters(params: Map<String, String>): String {
        return buildString {
            val config = HelmDiffFeatureConfiguration(params)

            if (config.isReportEnabled()) {
                appendLine("Provide report tab on changes in '${config.getPlanJsonFile()}'")
            }

            if (config.updateBuildStatus()) {
                appendLine("Update build status with plan results")
            }

            if (config.hasProtectedResourcePattern()) {
                appendLine("Create build problem if any of the protected resources are marked for destroy: ${config.getProtectedResourcePattern()}")
            }
        }
    }
}