// Global map variable
var map;

// Set the center as Firebase HQ
var locations = {
  "Grenoble": [45.1841656, 5.7155425]
};
var center_stations = locations["Grenoble"];

// Query radius
var radiusInKm_stations = 8;

// Get a reference to the Firebase public transit open data set
// Create a new GeoFire instance, pulling data from the public transit data
var trainStationsFirebaseRef = new Firebase("https://shining-inferno-9452.firebaseio.com/")
var geoFire_stations = new GeoFire(trainStationsFirebaseRef.child("_geofire"));

// Create a callback to handle the result of the authentication
function authHandler(error, authData) {
  if (error) {
    console.log("Login Failed!", error);
  } else {
    console.log("Authenticated successfully with payload:", authData);
  }
}

// Auth with an email/password combination
trainStationsFirebaseRef.authWithPassword({
  email    : 'alexis.mp@gmail.com',
  password : 'foo'
}, authHandler);

// trainStationsFirebaseRef.authWithOAuthRedirect("google", authHandler);


/*************/
/*  GEOQUERY */
/*************/
// Keep track of all of the vehicles currently within the query
var stationsInQuery = {};

// Create a new GeoQuery instance
var geoQuery_stations = geoFire_stations.query({
  center: center_stations,
  radius: radiusInKm_stations
})

/* Adds new train station markers to the map when they enter the query */
geoQuery_stations.on("key_entered", function(stationId, stationLocation) {
  // Specify that the station has entered this query
  stationsInQuery[stationId] = true;

  // Look up the train station's data in the appropriate Firebase reference
  trainStationsFirebaseRef.child(stationId).once("value", function(dataSnapshot) {
    // Get the train station data from Firebase
    station = dataSnapshot.val();

    // If the train station has not already exited this query in the time it
    // took to look up its data add it to the map
    if (station !== null && stationsInQuery[stationId] === true) {
      // Add the train station to the list of stations in the query
      stationsInQuery[stationId] = station;
      // Create a new marker for the train station
      station.marker = createStationMarker(station);
    }
  });
});

/* Removes train station markers from the map when they exit the query */
geoQuery_stations.on("key_exited", function(stationId, stationLocation) {
  // Get the train station from the list of stations in the query
  var station = stationsInQuery[stationId];

  // If the train station's data has already been loaded from Firebase,
  // remove its marker from the map
  if (station !== true) {
    station.marker.setMap(null);
  }

  // Remove the train station from the list of stations in the query
  delete stationsInQuery[stationId];
});
