package com.willeypianotuning.toneanalyzer.billing

import com.willeypianotuning.toneanalyzer.sync.json.ObjectSerializer
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class PurchaseSerializer : ObjectSerializer<InAppPurchase>() {
    override fun fromJson(json: JSONObject): InAppPurchase {
        val id = json.getString("id")
        val products = json.optJSONArray("products")?.let { jsonArray ->
            List(jsonArray.length()) { i -> jsonArray.getString(i) }
        } ?: listOf(json.getString("sku"))
        val packageName = json.getString("packageName")
        val purchaseTime = Date(json.getLong("purchaseTime"))
        val purchaseState = json.getInt("purchaseState")
        val developerPayload = json.getString("developerPayload")
        val purchaseToken = json.getString("purchaseToken")
        val signature = json.getString("signature")
        val responseData = json.getString("responseData")
        val autoRenewing = json.getBoolean("autoRenewing")
        val acknowledged = json.getBoolean("acknowledged")
        val verificationStatusInt = json.getInt("verificationStatus")
        val verificationStatus =
            VerificationStatus.values().firstOrNull { it.ordinal == verificationStatusInt }
                ?: VerificationStatus.VERIFICATION_FAILED
        val expirationDate = if (json.has("expirationDate")) {
            Date(json.getLong("expirationDate"))
        } else {
            null
        }
        return InAppPurchase(
            id,
            packageName,
            products,
            purchaseTime,
            purchaseState,
            developerPayload,
            purchaseToken,
            signature,
            responseData,
            autoRenewing,
            acknowledged,
            verificationStatus,
            expirationDate
        )
    }

    override fun toJson(obj: InAppPurchase): JSONObject {
        val json = JSONObject()
        json.put("id", obj.orderId)
        val skus = obj.products.fold(JSONArray()) { acc, sku -> acc.put(sku) }
        json.put("products", skus)
        json.put("packageName", obj.packageName)
        json.put("purchaseTime", obj.purchaseTime.time)
        json.put("purchaseState", obj.purchaseState)
        json.put("developerPayload", obj.developerPayload)
        json.put("purchaseToken", obj.purchaseToken)
        json.put("signature", obj.signature)
        json.put("responseData", obj.responseData)
        json.put("autoRenewing", obj.autoRenewing)
        json.put("acknowledged", obj.acknowledged)
        json.put("verificationStatus", obj.verificationStatus.ordinal)
        obj.expirationDate?.let {
            json.put("expirationDate", it.time)
        }
        return json
    }
}