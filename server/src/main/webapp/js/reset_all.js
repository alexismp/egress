// Get a reference to the data set
var ref = new Firebase("https://shining-inferno-9452.firebaseio.com/stations");

// Create a callback to handle the result of the authentication
function authHandler(error, authData) {
    if (error) {
        console.log("Login Failed!", error);
    } else {
        console.log("Authenticated successfully with payload:", authData);
        console.log("Name: ", authData.google.displayName);
        updateUserUI(authData.google.displayName);

        resetAll();
        updateUserUI("Done reseting all station owners!");
    }
}

// brute force FTW!
function resetAll() {
    for (var i = 1; i <= 6441; i++) {
        var station = ref.child(i);
        station.update({
            "owner": "",
            "when": "",
            "OwnerMail":""
        });
    }
}

function updateUserUI(message) {
    var legend = document.getElementById('userlegend');
    var div = document.createElement('div');
    div.innerHTML = "User: " + message;
    legend.appendChild(div);
}

ref.authWithOAuthPopup("google", authHandler);

