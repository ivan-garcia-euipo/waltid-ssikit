package id.walt.auditor

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import id.walt.auditor.dynamic.DynamicPolicy
import id.walt.auditor.dynamic.DynamicPolicyArg
import id.walt.common.deepMerge
import id.walt.model.oidc.VpTokenClaim
import id.walt.services.context.WaltIdContext
import id.walt.services.hkvstore.HKVKey
import org.web3j.abi.datatypes.Bool
import java.io.StringReader
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor

open class PolicyFactory<P : VerificationPolicy, A: Any>(val policyType: KClass<P>, val argType: KClass<A>?, val name: String, val description: String? = null) {
    open fun create(argument: Any? = null): P {
        return argType?.let {
            policyType.primaryConstructor!!.call(argument)
        }
        ?: policyType.createInstance()
    }


    val requiredArgumentType = when(argType) {
        null -> "None"
        else -> argType.simpleName!!
    }
}

class DynamicPolicyFactory(val dynamicPolicyArg: DynamicPolicyArg, name: String, description: String?) : PolicyFactory<DynamicPolicy, JsonObject>(
    DynamicPolicy::class, JsonObject::class, name, description) {
    override fun create(argument: Any?): DynamicPolicy {
        val mergedInput = if(argument != null) {
            JsonObject(dynamicPolicyArg.input).deepMerge(argument as JsonObject)
        } else {
            dynamicPolicyArg.input
        }
        return super.create(
            DynamicPolicyArg(
            input = mergedInput,
            policy = dynamicPolicyArg.policy,
            dataPath = dynamicPolicyArg.dataPath,
            policyQuery = dynamicPolicyArg.policyQuery,
            name = name
        )
        )
    }
}

object PolicyRegistry {
    const val SAVED_POLICY_ROOT_KEY = "policies"
    private val policies = LinkedHashMap<String, PolicyFactory<*, *>>()
    val defaultPolicyId: String

    fun <P : ParameterizedVerificationPolicy<A>, A: Any> register(policy: KClass<P>, argType: KClass<A>, description: String? = null)
        = policies.put(policy.simpleName!!, PolicyFactory(policy, argType, policy.simpleName!!, description))

    fun <P : SimpleVerificationPolicy> register(policy: KClass<P>, description: String? = null)
        = policies.put(policy.simpleName!!, PolicyFactory<P, Unit>(policy, null, policy.simpleName!!, description))

    fun registerSavedPolicy(name: String, dynamicPolicyArg: DynamicPolicyArg)
        = policies.put(name, DynamicPolicyFactory(dynamicPolicyArg, name, dynamicPolicyArg.description))

    fun <A: Any> getPolicy(id: String, argument: A? = null) = policies[id]!!.create(argument)
    fun getPolicy(id: String) = getPolicy(id, null)
    fun contains(id: String) = policies.containsKey(id)
    fun listPolicies() = policies.keys
    fun listPolicyInfo() = policies.values.map{ p -> VerificationPolicyMetadata(p.name, p.description, p.requiredArgumentType, p is DynamicPolicyFactory) }

    fun getPolicyWithJsonArg(id: String, argumentJson: JsonObject?): VerificationPolicy {
        val policyFactory = policies[id]!!
        val argument =
            policyFactory.argType?.let {
                argumentJson?.let {
                    if(policyFactory.argType == JsonObject::class) {
                        argumentJson
                    } else {
                        Klaxon().fromJsonObject(
                            argumentJson,
                            policyFactory.argType.java,
                            policyFactory.argType
                        )
                    }
                }
            }

        return policyFactory.create(argument)
    }

    fun getPolicyWithJsonArg(id: String, argumentJson: String?): VerificationPolicy {
        return getPolicyWithJsonArg(id, argumentJson?.let { Klaxon().parseJsonObject(StringReader(it)) })
    }

    fun canCreatePolicy(name: String, override: Boolean): Boolean {
        return !policies.contains(name) || (policies[name] is DynamicPolicyFactory && override)
    }

    fun createSavedPolicy(name: String, dynPolArg: DynamicPolicyArg): DynamicPolicyArg {
        WaltIdContext.hkvStore.put(HKVKey(SAVED_POLICY_ROOT_KEY, name), Klaxon().toJsonString(dynPolArg))
        registerSavedPolicy(name, dynPolArg)
        return dynPolArg
    }

    fun deleteSavedPolicy(name: String) {
        WaltIdContext.hkvStore.delete(HKVKey(SAVED_POLICY_ROOT_KEY, name))
        policies.remove(name)
    }

    fun initSavedPolicies() {
        WaltIdContext.hkvStore.listChildKeys(HKVKey(SAVED_POLICY_ROOT_KEY)).forEach {
            registerSavedPolicy(it.name, Klaxon().parse(WaltIdContext.hkvStore.getAsString(it)!!)!!)
        }
    }

    init {
        defaultPolicyId = SignaturePolicy::class.simpleName!!
        register(SignaturePolicy::class, "Verify by signature")
        register(JsonSchemaPolicy::class, "Verify by JSON schema")
        register(TrustedSchemaRegistryPolicy::class, "Verify by EBSI Trusted Schema Registry")
        register(TrustedIssuerDidPolicy::class, "Verify by trusted issuer did")
        register(TrustedIssuerRegistryPolicy::class, "Verify by trusted EBSI Trusted Issuer Registry record")
        register(TrustedSubjectDidPolicy::class, "Verify by trusted subject did")
        register(IssuedDateBeforePolicy::class, "Verify by issuance date")
        register(ValidFromBeforePolicy::class, "Verify by valid from")
        register(ExpirationDateAfterPolicy::class, "Verify by expiration date")
        register(GaiaxTrustedPolicy::class, "Verify Gaiax trusted fields")
        register(GaiaxSDPolicy::class, "Verify Gaiax SD fields")
        register(ChallengePolicy::class, ChallengePolicyArg::class, "Verify challenge")
        register(VpTokenClaimPolicy::class, VpTokenClaim::class, "Verify verifiable presentation by OIDC/SIOPv2 VP token claim")
        register(CredentialStatusPolicy::class, "Verify by credential status")
        register(DynamicPolicy::class, DynamicPolicyArg::class, "Verify credential by rego policy")

        // predefined, hardcoded rego policy specializations
        // VerifiableMandate policy as specialized rego policy
        registerSavedPolicy("VerifiableMandatePolicy", DynamicPolicyArg(
            "VerifiableMandatePolicy", "Predefined policy for verifiable mandates",
            JsonObject(), "$.credentialSubject.policySchemaURI",
            "$.credentialSubject.holder", "data.system.main" )
        )

        // other saved (Rego) policies
        initSavedPolicies()

        //RegoPolicy(RegoPolicyArg(mapOf(), "")).argument.input

    }
}
