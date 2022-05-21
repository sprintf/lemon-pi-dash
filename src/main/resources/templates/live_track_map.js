
let map;
let carMarkers = {}

function initMap() {
    console.log("map drawing for track " + trackCode)
    map = new google.maps.Map(document.getElementById("map"), {
        center: { lat: 37.926223, lng: -122.295029 },
        zoom: 16,
    });
    console.log("map drawn")
    streamBackendData(trackCode)
}

window.initMap = initMap;

const streamBackendData = async (trackCode) => {
    const stream = new EventSource(backendServer + "/trackdatastream/" + trackCode);
    stream.onmessage = function(event) {
        const payload = JSON.parse(event.data)

        payload["speedMph"]
        payload["timestamp"]
        payload["heading"]
        let carNum = payload["carNumber"]

        if (carNum in carMarkers) {
            carMarkers[carNum].setPosition({ lat: payload["lat"], lng: payload["long"]})
            let icon = carMarkers[carNum].getIcon()
            icon.rotation = payload["heading"]
            carMarkers[carNum].setIcon(icon)
        } else {
            const svgMarker = {
                path: google.maps.SymbolPath.FORWARD_CLOSED_ARROW,
                fillColor: getCarColor(carNum),
                strokeColor: getCarColor(carNum),
                rotation: 0,
                scale: 5,
                labelOrigin: new google.maps.Point(4, 4),
            };
            carMarkers[carNum] = new google.maps.Marker({
                position: { lat: payload["lat"], lng: payload["long"]},
                icon: svgMarker,
                map,
                label: {
                    text: carNum,
                    color: getCarColor(carNum),
                    fontSize: "32px",
                    fontWeight: "bold"
                },
            });
        }

    }
}

function getCarColor(carNum) {
    switch (carNum) {
        case "181": return "purple"
        case "15": return "#dd2222"  /// a kind of red
        case "86": return "#4444ff"  /// a kind of ble
        case "8": return "purple"
        default : return "black"   /// could chose a random color hashed off carNum
    }
}

// todo : improve display : show speed
// make map api key non nickable : use a key that only works from dash
// gray out after 30s
// find out why pi stopped sending overnight

