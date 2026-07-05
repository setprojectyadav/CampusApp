package com.college.campusapp.api

import com.google.gson.annotations.SerializedName

data class Product(
    @SerializedName("code") val code: String?,
    @SerializedName("product_name") val productName: String?,
    @SerializedName("brands") val brands: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("image_front_url") val imageFrontUrl: String? = null
) {
    fun getHighResImageUrl(): String? {
        return if (!imageFrontUrl.isNullOrEmpty()) imageFrontUrl else imageUrl
    }
}
