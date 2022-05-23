
let map;
let carMarkers = {}
let carMarkerTimestamps = {}

function initMap() {
    console.log("map drawing for track " + trackCode)
    map = new google.maps.Map(document.getElementById("map"), {
        center: { lat: mapLat, lng: mapLong },
        mapTypeId: 'satellite',
        zoom: 17,
    });
    console.log("map drawn")
    streamBackendData(trackCode)
    setInterval(function(){
        markOfflineCars()
    }, 30000)
}

window.initMap = initMap;

const streamBackendData = async (trackCode) => {
    const stream = new EventSource(backendServer + "/trackdatastream/" + trackCode);
    stream.onmessage = function(event) {
        const payload = JSON.parse(event.data)

        //payload["speedMph"]
        //payload["timestamp"]
        //payload["heading"]
        let carNum = payload["carNumber"]

        if (carNum in carMarkers) {
            carMarkers[carNum].setPosition({ lat: payload["lat"], lng: payload["long"]})
            let icon = carMarkers[carNum].getIcon()
            icon.rotation = payload["heading"]
            carMarkers[carNum].setIcon(icon)
            let label = carMarkers[carNum].getLabel()
            if ("speedMph" in payload) {
                label.text = payload["speedMph"].toString()
            } else {
                label.text = carNum
            }
            carMarkers[carNum].setLabel(label)
            carMarkerTimestamps[carNum] = payload["timestamp"]
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
            // add the timestamp from gps so we can tellwhen they go offline
            carMarkerTimestamps[carNum] = payload["timestamp"]
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

function markOfflineCars() {
    for (const key of Object.keys(carMarkerTimestamps)) {
        let value = carMarkerTimestamps[key]
        let nowSeconds = Date.now() / 1000
        if (nowSeconds - value > 30) {
            // show the car number in black
            let label = carMarkers[key].getLabel()
            label.text = "<<offline>>"
            carMarkers[key].setLabel(label)
        }
    }
}


