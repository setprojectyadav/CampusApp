package com.college.campusapp.api

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface FoodFactsApi {

    @GET("cgi/search.pl")
    fun searchProducts(
        @Query("search_terms") terms: String,
        @Query("search_simple") simple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): Call<SearchResponse>

    companion object {
        private const val BASE_URL = "https://world.openfoodfacts.org/"

        fun create(): FoodFactsApi {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(FoodFactsApi::class.java)
        }
    }
}
