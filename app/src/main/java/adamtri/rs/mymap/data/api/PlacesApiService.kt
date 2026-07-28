package adamtri.rs.mymap.data.api

import adamtri.rs.mymap.data.model.PlaceDetailsResponse
import adamtri.rs.mymap.data.model.PlaceSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PlacesApiService {
    @GET("maps/api/place/textsearch/json")
    suspend fun searchPlace(
        @Query("query") query: String,
        @Query("key") apiKey: String
    ): PlaceSearchResponse

    @GET("maps/api/place/details/json")
    suspend fun getPlaceDetails(
        @Query("place_id") placeId: String,
        @Query("fields") fields: String = "name,formatted_address,rating,user_ratings_total",
        @Query("key") apiKey: String
    ): PlaceDetailsResponse

    @GET("maps/api/place/nearbysearch/json")
    suspend fun searchNearby(
        @Query("location") location: String,
        @Query("radius") radius: Int = 50,
        @Query("key") apiKey: String
    ): PlaceSearchResponse
}
