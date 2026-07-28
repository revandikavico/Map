package adamtri.rs.mymap.data.model

import com.google.gson.annotations.SerializedName

data class PlaceSearchResponse(
    @SerializedName("results")
    val results: List<PlaceSearchResult>,
    @SerializedName("status")
    val status: String
)

data class PlaceSearchResult(
    @SerializedName("place_id")
    val placeId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("formatted_address")
    val formattedAddress: String?,
    @SerializedName("geometry")
    val geometry: Geometry
)

data class Geometry(
    @SerializedName("location")
    val location: Location
)

data class Location(
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lng")
    val lng: Double
)

data class PlaceDetailsResponse(
    @SerializedName("result")
    val result: PlaceDetails,
    @SerializedName("status")
    val status: String
)

data class PlaceDetails(
    @SerializedName("name")
    val name: String,
    @SerializedName("formatted_address")
    val formattedAddress: String?,
    @SerializedName("rating")
    val rating: Double?,
    @SerializedName("user_ratings_total")
    val userRatingsTotal: Int?
)
