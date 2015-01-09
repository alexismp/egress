/*****************/
/*  GOOGLE MAPS  */
/*****************/
/* Initializes Google Maps */
function initializeMap() {
  // Get the location as a Google Maps latitude-longitude object
  var loc = new google.maps.LatLng(center_stations[0], center_stations[1]);

  // Create the Google Map
  map = new google.maps.Map(document.getElementById("map-canvas"), {
    center: loc,
    zoom: 11,
    mapTypeId: google.maps.MapTypeId.SATELLITE
  });

  // Create a draggable circle centered on the map
  var circle = new google.maps.Circle({
    strokeColor: "#6D3099",
    strokeOpacity: 0.7,
    strokeWeight: 1,
    fillColor: "#B650FF",
    fillOpacity: 0.35,
    map: map,
    center: loc,
    radius: ((radiusInKm_stations) * 1000),
    draggable: true
  });

  //Update the query's criteria every time the circle is dragged
  var updateCriteria = _.debounce(function() {
    var latLng = circle.getCenter();
    geoQuery_stations.updateCriteria({
      center: [latLng.lat(), latLng.lng()],
      radius: radiusInKm_stations
    });
  }, 10);
  google.maps.event.addListener(circle, "drag", updateCriteria);
}

function createStationMarker(station) {
  var color;
  if ( station.owner !== "" ) {
    color = "888888";
  } else {
    color = "50B1FF" ;
  }
  var marker = new google.maps.Marker({
    icon: "https://chart.googleapis.com/chart?chst=d_bubble_icon_text_small&chld=train|bbT|" + encodeURI(station.NOM) + "|" + color + "|555",
    position: new google.maps.LatLng(station.LATITUDE, station.LONGITUDE),
    optimized: true,
    map: map
  });

  return marker;
}
