package jetbrains.buildServer.helmReport.report

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.pebbletemplates.pebble.PebbleEngine
import io.pebbletemplates.pebble.loader.ClasspathLoader
import io.pebbletemplates.pebble.template.PebbleTemplate
import jetbrains.buildServer.agent.BuildProgressLogger
import jetbrains.buildServer.helmReport.HelmDiffFeatureConstants
import java.io.File
import java.io.FileWriter
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.pebbletemplates.pebble.extension.Filter
import io.pebbletemplates.pebble.template.EvaluationContext

class HelmDiffReportGenerator(
    private val myLogger: BuildProgressLogger,
    private val myPlanData: String
) {
    private fun getTemplate(): PebbleTemplate {
        val resourcesLoader = ClasspathLoader()
        resourcesLoader.prefix = HelmDiffFeatureConstants.REPORT_RESOURCE_FOLDER_PATH
        val mapper = jacksonObjectMapper()

        // Define filter inline
        val jsonEncodeFilter = object: Filter {
            override fun apply(
                input: Any?,
                args: Map<String?, Any?>?,
                self: PebbleTemplate?,
                context: EvaluationContext?,
                lineNumber: Int
            ): Any? {
                return when (input) {
                    null -> "null"                       // emit JSON null
                    is String -> mapper.writeValueAsString(input)   // JSON string literal
                    else -> mapper.writeValueAsString(input.toString())
                }
            }

            override fun getArgumentNames() = null

        }

        val engine = PebbleEngine.Builder()
            .extension(IndentationExtension())
            .loader(resourcesLoader)
            .apply {
                extension(object : io.pebbletemplates.pebble.extension.AbstractExtension() {
                    override fun getFilters(): Map<String, Filter> =
                        mapOf("json_encode" to jsonEncodeFilter)
                })
            }
            .build()

        try {
            return engine.getTemplate(HelmDiffFeatureConstants.REPORT_TEMPLATE_FILE)
        } catch (e: Exception) {
            myLogger.warning(e.stackTraceToString())
            throw(e)
        }
    }

    fun generate(reportFile: File): String {
        myLogger.message("Generating report...")

        val writer = FileWriter(reportFile)
        getTemplate().evaluate(writer, mapOf("planData" to myPlanData))

        return reportFile.absolutePath
    }

}