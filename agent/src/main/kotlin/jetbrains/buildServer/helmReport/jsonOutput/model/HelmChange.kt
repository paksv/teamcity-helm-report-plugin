package jetbrains.buildServer.helmReport.jsonOutput.model

/*
{
  "api": "v1",
  "kind": "Service",
  "namespace": "default",
  "name": "ingress-ingress-nginx-controller",
  "change": "MODIFY"
}
 */
// ===== Model =====
data class HelmChange(
    val change: String,
    val namespace: String,
    val name: String,
    val kind: String,
    val api: String?,
    val snippet: String?
)