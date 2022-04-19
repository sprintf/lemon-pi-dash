package com.normtronix.dash

import com.normtronix.meringue.CarData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.stream.Collectors


data class TimeResponse(val hour: Int, val minute: Int, val second: Int)

enum class FlagStatus {
    UNKNOWN,
    GREEN,
    YELLOW,
    RED,
    BLACK,
    FINISH,
}

data class CarDataResponse(
    val carNumber: String,
    val timestamp: Long,
    val flagStatus: FlagStatus,
    val lapCount: Int,
    val position: Int,
    val positionInClass: Int,
    val lastLapTime: Float,
    val fastLapTime: Float,
    val fastLap: Int,
    val gap: String,
    val carAhead: String,
    val coolantTemp: Int,
    val fuelRemainingPercent: Int,
    val driverName: String,
)

data class TrackAndCar(
    val trackCode: String,
    val carNumber: String,
)

data class RaceData(
    val raceId: String,
    val teamName: String,
    var driverName: String,
    val trackAndCar: TrackAndCar,
)

data class LiveRace(
    val raceId: String,
    val trackCode: String,
    val trackName: String,
)

data class ConfigData(
    val channelId: String,
    val trackCode: String,
    val teamName: String,
    val carNumber: String,
    val driverName: String,
    val raceName: String,
)

data class ChannelResponse(
    val trackCode: String,
    val teamName: String,
    val carNumber: String,
    val raceName: String,
)

data class RaceParticipant(
    val carNumber: String,
    val teamName: String,
)

@RestController
class RestController {

    @Autowired
    lateinit var connector: MeringueConnector

    private val driverAssociation = mutableMapOf<TrackAndCar, String>()
    private val channelAssociation = mutableMapOf<String, TrackAndCar>()
    private val channelTeamAssociation = mutableMapOf<String, String>()
    private val channelRaceAssociation = mutableMapOf<String, String>()

    @GetMapping("/racelist")
    suspend fun getRaceList(): List<LiveRace> {
        return connector.getRaceData().let {
            it.responseList.stream().filter {
                it.running
            }.map {
                LiveRace(it.handle, it.trackCode, it.trackName)
            }
        }.collect(Collectors.toList())
    }

    @GetMapping("/racefield/{track}")
    suspend fun getRaceField(@PathVariable track: String): List<RaceParticipant> {
        return connector.getRaceField(track).let {
            it.participantsList.stream().map {
                RaceParticipant(it.carNumber, it.teamName)
            }
        }.collect(Collectors.toList())
    }

    @GetMapping("/config/{channelId}")
    suspend fun getConfig(@PathVariable channelId: String) : ConfigData {
        val trackAndCar = channelAssociation[channelId]?: TrackAndCar("", "")
        return ConfigData(
            channelId = channelId,
            trackCode = trackAndCar.trackCode,
            teamName = channelTeamAssociation[channelId]?:"",
            carNumber = trackAndCar.carNumber,
            driverName = driverAssociation[trackAndCar]?:"",
            raceName = channelRaceAssociation[channelId]?:""
        )
    }

    @PostMapping("/configure")
    fun configure(@RequestBody request: ConfigData) {
        // validate everything
        // find the track Code (or have then pass it in)
        // create the channelAssociation
        val trackAndCar = TrackAndCar(request.trackCode, request.carNumber)
        channelAssociation[request.channelId] = trackAndCar
        channelTeamAssociation[request.channelId] = request.teamName
        channelRaceAssociation[request.channelId] = request.raceName
        // keep this on its own as it can change independently
        driverAssociation[trackAndCar] = request.driverName
    }

    @GetMapping("/time")
    fun time():TimeResponse {
        val now = LocalDateTime.now()
        return TimeResponse(now.hour, now.minute,now.second)
    }

    @GetMapping("/channel/{channelId}")
    suspend fun channelAssociation(@PathVariable channelId: String) : ChannelResponse? {
        log.info("request for channel $channelId maps to ${channelAssociation[channelId]}")
        return channelAssociation[channelId]?.let {
            ChannelResponse(
                trackCode = it.trackCode,
                teamName = channelTeamAssociation[channelId]?:"-",
                carNumber = it.carNumber,
                raceName = channelRaceAssociation[channelId]?:"-")
        }
    }

    @GetMapping("/cardata/{track}/{car}")
    suspend fun cardata(@PathVariable track: String,
                @PathVariable car: String): CarDataResponse {
        return buildResponse(connector.getData(track, car), driverAssociation[TrackAndCar(track, car)])
    }

    @GetMapping("/cardatastream/{track}/{car}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    suspend fun cardataStream(@PathVariable track: String,
                              @PathVariable car: String): Flow<CarDataResponse> {

        return connector.streamData(track, car).map {
            buildResponse(it, driverAssociation[TrackAndCar(track, car)])
        }
    }

    private fun buildResponse(it: CarData.CarDataResponse, driverName: String?): CarDataResponse {
        return CarDataResponse(
            carNumber = it.carNumber,
            timestamp = it.timestamp,
            flagStatus = FlagStatus.values()[it.flagStatusValue],
            lapCount = it.lapCount,
            position = it.position,
            positionInClass = it.positionInClass,
            lastLapTime = it.lastLapTime,
            fastLap = it.fastestLap,
            fastLapTime = it.fastestLapTime,
            gap = it.gap,
            carAhead = it.carAhead,
            coolantTemp = it.coolantTemp,
            fuelRemainingPercent = it.fuelRemainingPercent,
            driverName = driverName?:"-"
        )
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(RestController::class.java)
    }
}

// bring this backif you need detailed debugging
//@Configuration
//class RequestLoggingFilterConfig {
//    @Bean
//    fun logFilter(): CommonsRequestLoggingFilter {
//        val filter = CommonsRequestLoggingFilter()
//        filter.setIncludeQueryString(true)
//        filter.setIncludePayload(true)
//        filter.setMaxPayloadLength(10000)
//        filter.setIncludeHeaders(false)
//        filter.setAfterMessagePrefix("REQUEST DATA : ")
//        return filter
//    }
//}