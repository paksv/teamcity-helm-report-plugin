package jetbrains.buildServer.helmReport

import jetbrains.buildServer.log.Loggers
import jetbrains.buildServer.serverSide.BuildsManager
import jetbrains.buildServer.serverSide.SBuild
import jetbrains.buildServer.serverSide.artifacts.BuildArtifact
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode
import jetbrains.buildServer.web.openapi.BuildTab
import jetbrains.buildServer.web.openapi.PluginDescriptor
import jetbrains.buildServer.web.openapi.WebControllerManager
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.stream.Collectors


class HelmDiffReportTab(
    manager: WebControllerManager,
    buildManager: BuildsManager,
    descriptor: PluginDescriptor
) : BuildTab(
    "helmDiffReport",
    "Helm Diff",
    manager,
    buildManager,
    descriptor.getPluginResourcesPath("helmDiffReport.jsp")
) {
    init {
        addCssFile(descriptor.getPluginResourcesPath("helmDiffReport.css"))
    }

    override fun isAvailableFor(build: SBuild): Boolean {
        return getHelmDiffReportFromBuildArtifacts(build) != null
    }

    private fun getHelmDiffReportFromBuildArtifacts(build: SBuild): BuildArtifact? {
        return try {
            val hiddenArtifacts = build.getArtifacts(
                BuildArtifactsViewMode.VIEW_HIDDEN_ONLY
            )
            hiddenArtifacts.getArtifact(
                "${HelmDiffFeatureConstants.HIDDEN_ARTIFACT_REPORT_FOLDER}${File.separator}${HelmDiffFeatureConstants.HIDDEN_ARTIFACT_REPORT_FILENAME}"
            )
        } catch (e: AccessDeniedException) {
            null
        }
    }

    override fun fillModel(model: MutableMap<String, Any>, build: SBuild) {
        val reportArtifact = getHelmDiffReportFromBuildArtifacts(build)
        try {
            val inputStream = reportArtifact!!.inputStream // isAvailableFor is evaluated before filling the model
            val reportString = BufferedReader(
                InputStreamReader(inputStream)
            ).lines().collect(
                Collectors.joining("\n")
            )
            model["reportString"] = reportString
        } catch (e: Exception) {
            Loggers.SERVER.warnAndDebugDetails(
                "Cannot process Terraform report artifact to show it in the build tab",
                e
            )
        }
    }
}