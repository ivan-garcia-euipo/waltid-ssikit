package id.walt.model.oidc

import com.beust.klaxon.Json
import id.walt.model.ListOrSingleValue
import id.walt.model.dif.PresentationDefinition
import id.walt.model.dif.PresentationSubmission
import id.walt.vclib.credentials.VerifiablePresentation

data class VpTokenClaim (
    val presentation_definition: PresentationDefinition
)

data class CredentialClaim(
    @Json(serializeNull = false)
    val type: String?, // CONDITIONAL. A JSON string denoting the type of the requested credential. MUST be present if manifest_id is not present.
    @Json(serializeNull = false)
    val manifest_id: String?, // CONDITIONAL. JSON String refering to a credentoal manifest published by the credential issuer. MUST be present if type is not present.
    @Json(serializeNull = false)
    val format: String? = null, // OPTIONAL. A JSON string representing a format in which the credential is requested to be issued. Valid values defined by this specification are jwt_vc and ldp_vc. Profiles of this specification MAY define addtional format values.
    @Json(serializeNull = false) @ListOrSingleValue
    val vp_token: List<VerifiablePresentation>? = null, // OPTIONAL. A parameter defined in [OIDC4VP] used to convey required verifiable presentations. The verifiable presentations passed in this parameter MUST be bound to a p_nonce generated by the respective issuer from the Nonce Endpoint.
    @Json(serializeNull = false)
    val presentation_submission: PresentationSubmission? = null, // OPTIONAL. JSON object as defined in [DIF.CredentialManifest]. This object refers to verifiable presentations required for the respective credential accoridng to the Credential Manifest and provided in an authorization request. All entries in the descriptor_map refer to verifiable presentations provided in the vp_token authorization request parameter.
    @Json(serializeNull = false)
    val wallet_issuer: String? = null, //OPTIONAL. JSON String containing the wallet's OpenID Connect Issuer URL. The Issuer will use the discovery process as defined in [SIOPv2] to determine the wallet's capabilities and endpoints. RECOMMENDED in Dynamic Credential Request.
    @Json(serializeNull = false)
    val user_hint: String? = null, // OPTIONAL. JSON String containing an opaque user hint the wallet MAY use in sub-sequent callbacks to optimize the user's experience. RECOMMENDED in Dynamic Credential Request.
)

data class Claims (
    @Json(serializeNull = false) val vp_token: VpTokenClaim? = null,
    @Json(serializeNull = false) val credentials: List<CredentialClaim>? = null
)