let channelId = null
let raceFlagStatus = "UNKNOWN"
let videoLag = 0

window.Twitch.ext.rig.log("starting!")

window.Twitch.ext.onError(function(errors) {
    window.Twitch.ext.rig.log("onError callback " + errors)
});

// see if jquery is working
$(document).ready(function() {
    // todo : put anything you need upon init in here
});

window.Twitch.ext.onAuthorized(function(auth) {
    channelId = auth.channelId;
    window.Twitch.ext.rig.log("authenticated " + channelId)
    getTrackAndCar()
});

const getTrackAndCar = async() => {
    const response = await fetch(backendServer  + "/channel/" + channelId);
    const payload = await response.json()
    window.Twitch.ext.rig.log("response json = " + JSON.stringify(payload));
    if ('trackCode' in payload && 'carNumber' in payload) {
        $("#lpi-teamName").text(payload.teamName)
        $("#lpi-raceName").text(payload.raceName)
        streamBackendData(payload.trackCode, payload.carNumber)
    } else {
        window.Twitch.ext.rig.log("no data ready for this channel")
    }
}



window.Twitch.ext.onContext(function(deets, fields) {
    if (fields.includes("hlsLatencyBroadcaster")) {
        videoLag = deets.hlsLatencyBroadcaster * 1000
    } else if (fields.includes("isFullScreen")) {
        if (deets.isFullScreen) {
            changeFontSizes(14, 28, 12);
        } else {
            changeFontSizes(12, 18, 10);
        }
    } else if (fields.includes("isTheatreMode")) {
        if (deets.isTheatreMode) {
            changeFontSizes(14, 28, 12);
        } else {
            changeFontSizes(12, 18, 10);
        }
    }
});

function changeFontSizes(headingSize, bigSize, smallSize) {
    $(".dash-heading").css("font-size", headingSize + "pt")
    $(".dash-big").css("font-size", bigSize + "pt")
    $(".dash-subtitle").css("font-size", smallSize + "pt")
}

function chooseColor(raceFlagStatus) {
    switch (raceFlagStatus) {
        case "GREEN" : return "#2a8b0f"
        case "YELLOW" : return "#bdbd14"
        case "RED" : return "#bf146b"
        default : return ""
    }
}

// convert a floating point time in seconds into a human readable amount
function toTime(floatTime) {
    let time = ""
    if (floatTime / 3600 >= 1) {
        time = String(parseInt(floatTime / 3600)) + ":"
        floatTime = floatTime % 3600
    }
    if (floatTime / 60 > 0) {
        time += String(parseInt(floatTime / 60)) + ":"
        floatTime = floatTime % 60
    }
    time += String(parseInt(floatTime)).padStart(2, '0') + "."
    floatTime = floatTime % 1
    time += String(parseInt(floatTime * 10))
    return time;
}

const streamBackendData = async (trackCode, carNumber) => {
    const stream = new EventSource(backendServer + "/cardatastream/" + trackCode + "/" + carNumber);
    stream.onmessage = function(event) {
        const payload = JSON.parse(event.data)
        window.Twitch.ext.rig.log("message = " + JSON.stringify(payload))

        if (payload["flagStatus"] !== raceFlagStatus) {
            raceFlagStatus = payload["flagStatus"]
            $("#raceInfoTable").css("background-color", chooseColor(raceFlagStatus));
        }

        // we delay updating the display based on the video lag
        setTimeout(function (payload) {
            for (let attrName in payload) {
                window.Twitch.ext.rig.log("applying " + attrName + " = " + payload[attrName])
                if (attrName.endsWith("Time")) {
                    $("#lpi-" + attrName).text(toTime(payload[attrName]));
                } else {
                    $("#lpi-" + attrName).text(payload[attrName]);
                }
            }
        }, videoLag, payload)

    }
}
