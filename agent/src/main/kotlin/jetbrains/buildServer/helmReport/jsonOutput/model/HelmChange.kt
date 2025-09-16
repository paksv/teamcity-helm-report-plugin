package jetbrains.buildServer.helmReport.jsonOutput.model

import jetbrains.buildServer.helmReport.report.ChangeItemBackground

/*
{
  "api": "v1",
  "kind": "Service",
  "namespace": "default",
  "name": "ingress-ingress-nginx-controller",
  "change": "MODIFY"
}
 */
data class HelmChange (
    val api: String,
    val kind: String,
    val namespace: String,
    val name: String,
    val change: String
) {
    val isModify = change == "MODIFY"

    val collapsibleButtonColorCSSClass: String
        get() {
            return when {
//                isCreated -> ChangeItemBackground.GREEN.cssClass
                isModify -> ChangeItemBackground.BLUE.cssClass
//                isReplaced -> ChangeItemBackground.ORANGE.cssClass
//                isDeleted -> ChangeItemBackground.RED.cssClass
                else -> ChangeItemBackground.FALLBACK.cssClass
            }
        }

}