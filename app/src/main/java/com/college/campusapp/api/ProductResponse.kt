package com.college.campusapp.api

import com.google.gson.annotations.SerializedName

data class SearchResponse(
    @SerializedName("products") val products: List<Product>?
)

data class Product(
    @SerializedName("code") val code: String?,
    @SerializedName("product_name") val productName: String?,
    @SerializedName("brands") val brands: String?,
    @SerializedName("image_url") val imageUrl: String?
)
