var channelId = ""
window.Twitch.ext.onAuthorized(async function(auth) {
    channelId = auth.channelId;
    const response = await fetch(backendServer + "/config/" + channelId);
    const payload = await response.json();
    if (response.status === 200) {
        $("#trackCode").val(payload.trackCode);
        $("#carNumber").val(payload.carNumber);
        $("#teamName").val(payload.teamName);
        $("#raceName").val(payload.raceName);
        $("#driverName").val(payload.driverName);
    }
    loadActiveRaces()
});

$(document).ready(function() {
    $("#submit").click(function () {
        handleSubmit();
    })
});

const loadActiveRaces = async function () {
    const response = await fetch(backendServer  + "/racelist/");
    const payload = await response.json()
    window.Twitch.ext.rig.log("race list  " + JSON.stringify(payload))
    for(var index in payload) {
        var entry = payload[index]
        let clone = $("#seedRow").clone();
        let raceId = entry["raceId"].split(":")[1]
        let trackCode = entry["trackCode"]
        let button = clone.find("td:nth-child(1)");
        button.html("<button value='" + trackCode + "'>Select</button>")
        clone.find("td:nth-child(2)").html(trackCode)
        clone.find("td:nth-child(3)").html(entry["trackName"])
        clone.appendTo("#currentRaceTable")
        button.click(function() {
            $("#trackCode").val(trackCode);
            loadRaceField(trackCode);
        });
    }
}

const loadRaceField = async function(trackCode) {
    const response = await fetch(backendServer  + "/racefield/" + trackCode);
    const payload = await response.json()
    const picker = $("#carNumberPicker")
    const carLookup = []
    picker.children().remove()
    for(var entry in payload) {
        const teamName = payload[entry].teamName
        carLookup[payload[entry].carNumber] = teamName
        picker.append($('<option>', {
            value: payload[entry].carNumber,
            text: payload[entry].carNumber + ":" + teamName,
        }));
    }
    $("#carNumber").change(function(val) {
        $("#teamName").val(carLookup[val.target.value])
    })
}

const handleSubmit = async function () {
    const postJson = {
        "channelId": channelId,
        "trackCode": $("#trackCode").val(),
        "raceName": $("#raceName").val(),
        "teamName": $("#teamName").val(),
        "carNumber": $("#carNumber").val(),
        "driverName": $("#driverName").val(),
    }

    window.Twitch.ext.rig.log("sending  " + JSON.stringify(postJson))

    const response = await fetch(backendServer + "/configure", {
        method: 'POST',
        mode: 'cors',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(postJson),
    });
    await response.json().then(
        $('#confirm').html('Good To Go!')
    )
}