package id.walt.model.oidc

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.nimbusds.oauth2.sdk.*
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic
import com.nimbusds.oauth2.sdk.auth.Secret
import com.nimbusds.oauth2.sdk.http.HTTPRequest
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.oauth2.sdk.token.AccessToken
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import com.nimbusds.openid.connect.sdk.OIDCScopeValue
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse
import com.nimbusds.openid.connect.sdk.SubjectType
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata
import id.walt.model.dif.CredentialManifest
import id.walt.model.dif.Issuer
import id.walt.model.dif.OutputDescriptor
import id.walt.services.oidc.OIDC4CIService
import id.walt.services.oidc.OIDC4VPService
import id.walt.vclib.model.AbstractVerifiableCredential
import id.walt.vclib.model.Proof
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.model.toCredential
import id.walt.vclib.registry.VcTypeRegistry
import net.minidev.json.JSONObject
import net.minidev.json.parser.JSONParser
import java.net.URI
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
open class OIDCProvider(
  val id: String,
  val url: String,
  @Json(serializeNull = false) val description: String? = null,
  @Json(serializeNull = false) val client_id: String? = null,
  @Json(serializeNull = false) val client_secret: String? = null
)

class OIDCProviderWithMetadata(
  id: String,
  url: String,
  description: String? = null,
  client_id: String? = null,
  client_secret: String? = null,
  @Json(ignored = true) val oidc_provider_metadata: OIDCProviderMetadata
) : OIDCProvider (id, url, description, client_id, client_secret) {

}
