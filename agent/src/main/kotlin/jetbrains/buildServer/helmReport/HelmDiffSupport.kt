package jetbrains.buildServer.helmReport

import jetbrains.buildServer.BuildProblemData
import jetbrains.buildServer.agent.*
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher
import jetbrains.buildServer.helmReport.diffParser.HelmDiffParser
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes
import jetbrains.buildServer.helmReport.jsonOutput.ParsingUtil
import jetbrains.buildServer.helmReport.jsonOutput.model.HelmChange
import jetbrains.buildServer.helmReport.jsonOutput.model.HelmPlanData
import jetbrains.buildServer.helmReport.report.HelmDiffReportGenerator
import jetbrains.buildServer.util.EventDispatcher
import java.io.Closeable
import java.io.File


class HelmDiffSupport(
    events: EventDispatcher<AgentLifeCycleListener>,
    watcher: ArtifactsWatcher
) : AgentLifeCycleAdapter() {

    private val myWatcher = watcher
    private val myFlowId = FlowGenerator.generateNewFlow()
    private val knownChangeTypes = mapOf(
        "ADD" to "add",
        "MODIFY" to "change",
        "REMOVE" to "destroy",
        "OWNERSHIP" to "ownership"
    )

    init {
        events.addListener(this)
    }

    private fun getFeature(build: AgentRunningBuild): AgentBuildFeature? {
        val features = build.getBuildFeaturesOfType(HelmDiffFeatureConstants.FEATURE_TYPE)
        if (features.isNotEmpty()) {
            return features.first() // isMultipleFeaturesPerBuildTypeAllowed = false on server side
        }
        return null
    }

    private fun getFeatureConfiguration(feature: AgentBuildFeature): HelmDiffFeatureConfiguration {
        return HelmDiffFeatureConfiguration(
            feature.parameters
        )
    }

    private fun getBuildLogger(build: AgentRunningBuild): FlowLogger {
        return build.buildLogger.getFlowLogger(myFlowId)!!
    }

    private fun parsePlanDataFromFile(
        logger: BuildProgressLogger,
        planOutputFile: File
    ): HelmPlanData {
        val text = planOutputFile.readText()
        try {
            logger.debug("Parsing template report data from the ${planOutputFile.absolutePath}")
            val objectMapper = ParsingUtil.getObjectMapper()
            val changes: List<HelmChange> = objectMapper.readValue(
                text,
                Array<HelmChange>::class.java
            ).toList()
            return HelmPlanData(planOutputFile.name, changes, text)
        } catch (_: Exception) {
            logger.debug("data file doesn't seem to be a helm diff template. Will parse it as a raw diff")
        }
        logger.debug("Will now parse raw diff output")
        return HelmPlanData(planOutputFile.name, HelmDiffParser.parse(text))
    }


    private fun updateBuildStatusWithPlanData(
        logger: BuildProgressLogger,
        planData: HelmPlanData
    ) {
        logger.message("Updating build status")
        if (planData.changes.isEmpty()) {
            updateBuildStatus(logger, "No resource changes are planned")
        } else {
            val changesMap = planData.changes.groupBy { it.change }
            val statusBuilder = StringBuilder()
            knownChangeTypes.forEach {
                if (!changesMap[it.key].isNullOrEmpty()) {
                    statusBuilder.append(", ${changesMap[it.key]?.size} to ${it.value}")
                }
            }
            changesMap.filter { !knownChangeTypes.contains(it.key) }.forEach {
                statusBuilder.append(",${it.value.size} to ${it.key.toLowerCase()}")
            }
            updateBuildStatus(logger, statusBuilder.substring(2))
        }
    }

    override fun beforeBuildFinish(runningBuild: AgentRunningBuild, buildStatus: BuildFinishedStatus) {
        val feature = getFeature(runningBuild)
        if (!buildStatus.isFailed && feature != null) {
            val logger = getBuildLogger(runningBuild)
            try {
                handleHelmDiffOutput(runningBuild, feature, logger)
            } catch (e: Exception) {
                logger.warning(e.stackTraceToString())
                throw e
            }
        }
    }

    private fun handleHelmDiffOutput(
        runningBuild: AgentRunningBuild,
        feature: AgentBuildFeature,
        logger: BuildProgressLogger
    ) {
        val configuration = getFeatureConfiguration(feature)
        if (configuration.isReportEnabled()) { // generate temporary report path
            val planFile = File(
                runningBuild.checkoutDirectory,
                configuration.getPlanJsonFile()!!
            )

            if (!planFile.exists()) {
                throw IllegalStateException("File ${planFile.absolutePath} does not exist, cannot parse Terraform plan info")
            }

            val reportFile = File(
                runningBuild.agentTempDirectory,
                HelmDiffFeatureConstants.HIDDEN_ARTIFACT_REPORT_FILENAME
            )

            ServiceMessageBlock(logger, "Handle Helm Diff output").use {
                val planData: HelmPlanData = parsePlanDataFromFile(logger, planFile)
                HelmDiffReportGenerator(logger, planData.jsonData).generate(reportFile)

                if (configuration.updateBuildStatus()) {
                    updateBuildStatusWithPlanData(logger, planData)
                }
            }

            myWatcher.addNewArtifactsPath( // publish report and plan file as hidden artifacts
                buildString {
                    appendLine("${planFile.absolutePath} => ${HelmDiffFeatureConstants.HIDDEN_ARTIFACT_REPORT_FOLDER}")
                    appendLine("${reportFile.absolutePath} => ${HelmDiffFeatureConstants.HIDDEN_ARTIFACT_REPORT_FOLDER}")
                }
            )
        }
    }

    companion object {
        class ServiceMessageBlock(
            private val myLogger: BuildProgressLogger,
            private val myBlockName: String
        ) : Closeable {
            init {
                val serviceMessage = ServiceMessage.asString(
                    ServiceMessageTypes.BLOCK_OPENED,
                    mapOf("name" to myBlockName)
                )
                myLogger.message(serviceMessage)
            }

            override fun close() {
                val serviceMessage = ServiceMessage.asString(
                    ServiceMessageTypes.BLOCK_CLOSED,
                    mapOf("name" to myBlockName)
                )
                myLogger.message(serviceMessage)
            }
        }

        fun createBuildProblem(
            logger: BuildProgressLogger,
            problemUniqueDescription: String,
            problemGenericDescription: String
        ) {
            val problemIdentityHash = problemUniqueDescription.hashCode()

            logger.logBuildProblem(
                BuildProblemData.createBuildProblem(
                    problemIdentityHash.toString(),
                    "PlannedChangesProblem",
                    problemGenericDescription
                )
            )
        }

        fun updateBuildStatus(
            logger: BuildProgressLogger,
            statusText: String,
            includeCalculatedPrefix: Boolean = false
        ) {
            val arguments = mutableMapOf<String, String>()
            when {
                includeCalculatedPrefix -> {
                    arguments["text"] = "{build.status.text}$statusText"
                }
                else -> {
                    arguments["text"] = statusText
                }
            }

            logger.message(
                ServiceMessage.asString(
                    ServiceMessageTypes.BUILD_STATUS,
                    arguments
                )
            )
        }
    }
}