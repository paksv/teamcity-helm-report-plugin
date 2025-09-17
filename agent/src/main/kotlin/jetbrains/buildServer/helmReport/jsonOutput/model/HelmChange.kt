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
sealed class HelmChange {
    abstract val namespace: String
    abstract val name: String
    abstract val kind: String
    abstract val api: String?
    abstract val change: String
    abstract val snippet: String?

    data class Added(
        override val namespace: String,
        override val name: String,
        override val kind: String,
        override val api: String?,
        override val snippet: String?
    ) : HelmChange() {
        override val change = "ADD"
    }

    data class Removed(
        override val namespace: String,
        override val name: String,
        override val kind: String,
        override val api: String?,
        override val snippet: String?
    ) : HelmChange() {
        override val change = "REMOVE"
    }

    data class Modified(
        override val namespace: String,
        override val name: String,
        override val kind: String,
        override val api: String?,
        override val snippet: String?
    ) : HelmChange() {
        override val change = "MODIFY"
    }

}
