package com.normtronix.dash

import com.google.protobuf.Empty
import com.normtronix.meringue.AdminServiceGrpcKt
import com.normtronix.meringue.CarData
import com.normtronix.meringue.CarDataServiceGrpcKt
import com.normtronix.meringue.MeringueAdmin
import io.grpc.*
import io.grpc.Channel as GrpcChannel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.ModelAndView
import java.util.concurrent.Executor
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Service
class MeringueConnector : HandlerExceptionResolver {

    private var bearerToken: String? = null

    @Value("\${meringueHost}")
    lateinit var meringueHost:String

    @Value("\${meringuePort}")
    lateinit var meringuePort:String

    @Autowired
    lateinit var adminCreds: AdminCredentialProvider

    @Autowired
    lateinit var trackLoader: TrackMetaDataLoader

    var trackChannelMap:MutableMap<String, GrpcChannel> = mutableMapOf()
    var carTrackSharedFlow:MutableMap<String, MutableStateFlow<CarData.CarDataResponse>> = mutableMapOf()
    var carPositionSharedFlow:MutableMap<String, MutableSharedFlow<CarData.CarPositionDataResponse>> = mutableMapOf()

    // hold a connection to the meringue stuff
    // todo : cache last known value and refresh after ... say 30s
    suspend fun getData(track: String, car: String) : CarData.CarDataResponse {
        val stub = CarDataServiceGrpcKt.CarDataServiceCoroutineStub(getGrpcChannelForTrack(track))
        return stub.getCarData(CarData.CarDataRequest.newBuilder().apply {
            this.trackCode = track
            this.carNumber = car
        }.build())
    }

    suspend fun streamCarData(track: String, car: String) : StateFlow<CarData.CarDataResponse> {
        // mild bug that two cars at different tracks synchronize together. good problem if it shows up
        val key = "$track:$car"

        //return synchronized(car.intern()) {
            // see if there's already a shared flow for this .. if there is then return a new
            // shared flow from it
            if (!carTrackSharedFlow.containsKey(key)) {
                val waiter = Waiter()
                GlobalScope.launch {
                    log.info("in global scope")
                    val stub = CarDataServiceGrpcKt.CarDataServiceCoroutineStub(getGrpcChannelForTrack(track))
                    stub.streamCarData(CarData.CarDataRequest.newBuilder().apply {
                        this.trackCode = track
                        this.carNumber = car
                    }.build())
                        .collect {
                            log.info("emitting message onto base flow for key $key to ${carTrackSharedFlow[key]?.subscriptionCount?.value} subscribers")
                            carTrackSharedFlow.getOrPut(key, { MutableStateFlow<CarData.CarDataResponse>(it) }).value = it
                            log.info("notifying")
                            waiter.doNotify()
                        }
                }
                log.info("waiting")
                waiter.doWait()
                log.info("done waiting")
            }
            return carTrackSharedFlow[key]?.asStateFlow()?:throw RuntimeException("bad")

    }

    suspend fun streamCarPositionData(track: String): Flow<CarData.CarPositionDataResponse> {
        val stub = CarDataServiceGrpcKt.CarDataServiceCoroutineStub(getGrpcChannelForTrack(track))
        return stub.streamCarPositionsAtTrack(CarData.CarPositionDataRequest.newBuilder().apply {
            this.trackCode = track
        }.build())
    }

    suspend fun getLiveRaces(trackName: String) : MeringueAdmin.LiveRaceListResponse {
        val words = trackName.splitToSequence(" ").map {
            it.lowercase()
        }
        val channel = getGrpcChannelForTrack("all")
        val stub = AdminServiceGrpcKt.AdminServiceCoroutineStub(channel)
        authenticate(stub)
        this.bearerToken?.let {
            val bldr = MeringueAdmin.SearchTermsRequest.newBuilder()
            words.forEach { bldr.addTerm(it) }
            try {
                return stub.withCallCredentials(BearerToken(it)).findLiveRaces(bldr.build())
            } catch (e: StatusException) {
                handleExpiredToken(e)
                log.error("grpc exception", e)
            }
        }
        throw RuntimeException("bad shizzle")
    }

    suspend fun getRaceData(): MeringueAdmin.RaceDataConnectionsResponse {
        val channel = getGrpcChannelForTrack("all")
        val stub = AdminServiceGrpcKt.AdminServiceCoroutineStub(channel)
        authenticate(stub)
        this.bearerToken?.let {
            try {
                return stub.withCallCredentials(BearerToken(it)).listRaceDataConnections(Empty.getDefaultInstance())
            } catch (e: StatusException) {
                handleExpiredToken(e)
                log.error("grpc exception", e)
            }
        }
        throw RuntimeException("bad shizzle")
    }

    suspend fun listConnectedCars(trackCode: String) : List<String> {
        val channel = getGrpcChannelForTrack("all")
        val stub = AdminServiceGrpcKt.AdminServiceCoroutineStub(channel)
        authenticate(stub)
        this.bearerToken?.let {
            try {
                val request = MeringueAdmin.ConnectedCarRequest.newBuilder()
                    .setTrackCode(trackCode)
                    .build()
                return stub.withCallCredentials(BearerToken(it)).listConnectedCars(request).carNumberList.toList()
            } catch (e: StatusException) {
                handleExpiredToken(e)
                log.error("grpc exception", e)
            }
        }
        throw RuntimeException("bad shizzle")
    }

    suspend fun sendDriverMessage(trackCode: String, carNumber: String, message: String) {
        val channel = getGrpcChannelForTrack("all")
        val stub = AdminServiceGrpcKt.AdminServiceCoroutineStub(channel)
        authenticate(stub)
        this.bearerToken?.let {
            try {
                val request = MeringueAdmin.DriverMessageRequest.newBuilder()
                    .setTrackCode(trackCode)
                    .setCarNumber(carNumber)
                    .setMessage(message)
                    .build()
                stub.withCallCredentials(BearerToken(it)).sendDriverMessage(request)
            } catch (e: StatusException) {
                handleExpiredToken(e)
                log.error("grpc exception", e)
            }
        }
    }

    suspend fun resetFastLapTime(trackCode: String, carNumber: String) {
        val channel = getGrpcChannelForTrack("all")
        val stub = AdminServiceGrpcKt.AdminServiceCoroutineStub(channel)
        authenticate(stub)
        this.bearerToken?.let {
            try {
                val request = MeringueAdmin.ResetFastLapTimeRequest.newBuilder()
                    .setTrackCode(trackCode)
                    .setCarNumber(carNumber)
                    .build()
                stub.withCallCredentials(BearerToken(it)).resetFastLapTime(request)
            } catch (e: StatusException) {
                handleExpiredToken(e)
                log.error("grpc exception", e)
            }
        }
    }

    suspend fun setTargetTime(trackCode: String, carNumber: String, targetTimeSec: Int) {
        val channel = getGrpcChannelForTrack("all")
        val stub = AdminServiceGrpcKt.AdminServiceCoroutineStub(channel)
        authenticate(stub)
        this.bearerToken?.let {
            try {
                val request = MeringueAdmin.SetTargetLapTimeRequest.newBuilder()
                    .setTrackCode(trackCode)
                    .setCarNumber(carNumber)
                    .setTargetTimeSeconds(targetTimeSec)
                    .build()
                stub.withCallCredentials(BearerToken(it)).setTargetLapTime(request)
            } catch (e: StatusException) {
                handleExpiredToken(e)
                log.error("grpc exception", e)
            }
        }
    }

    suspend fun connectToRace(
        trackCode: String,
        providerName: String,
        providerId: String
    ): MeringueAdmin.RaceDataConnectionResponse {
        val channel = getGrpcChannelForTrack("all")
        val stub = AdminServiceGrpcKt.AdminServiceCoroutineStub(channel)
        authenticate(stub)
        this.bearerToken?.let {
            val request = MeringueAdmin.ConnectToRaceDataRequest.newBuilder()
                .setProvider(MeringueAdmin.RaceDataProvider.valueOf(providerName))
                .setProviderId(providerId)
                .setTrackCode(trackCode)
                .build()
            try {
                return stub.withCallCredentials(BearerToken(it)).connectToRaceData(request)
            } catch (e: StatusException) {
                handleExpiredToken(e)
                log.error("grpc exception", e)
            }
        }
        throw RuntimeException("bad shizzle")
    }

    class ReauthNeededException : RuntimeException()

    private fun handleExpiredToken(e: StatusException) {
        if (e.status == Status.UNAUTHENTICATED) {
            this.bearerToken = null
            throw ReauthNeededException()
        }
    }

    data class TrackAndRaceStatus(
        val trackCode: String,
        val trackName: String,
        var active: Boolean = false,
        var handle: String? = null
    )

    suspend fun listTracksAndRaces(): List<TrackAndRaceStatus> {
        val channel = getGrpcChannelForTrack("all")
        val stub = AdminServiceGrpcKt.AdminServiceCoroutineStub(channel)
        authenticate(stub)
        this.bearerToken?.let {
            val trackMap =
                trackLoader.listTracks().associateBy({ it.code }, { TrackAndRaceStatus(it.code, it.name) })

            try {
                stub.withCallCredentials(BearerToken(it))
                    .listRaceDataConnections(Empty.getDefaultInstance()).responseList.forEach {
                    trackMap[it.trackCode]?.apply {
                        this.active = it.running
                        this.handle = it.handle
                    }
                }
            } catch (e: StatusException) {
                handleExpiredToken(e)

                log.error("grpc exception", e)
                throw e
            }

            return trackMap.toSortedMap().values.toList()
        }
        throw RuntimeException("bad shizzle")
    }

    suspend fun disconnectRace(handle: String) {
        val channel = getGrpcChannelForTrack("all")
        val stub = AdminServiceGrpcKt.AdminServiceCoroutineStub(channel)
        authenticate(stub)
        this.bearerToken?.let {
            try {
                stub.withCallCredentials(BearerToken(it)).disconnectRaceData(
                    MeringueAdmin.RaceDataConnectionRequest.newBuilder()
                        .setHandle(handle)
                        .build()
                )
            } catch (e: StatusException) {
                handleExpiredToken(e)

                log.error("grpc exception", e)
            }
        }
    }

    private suspend fun authenticate(stub: AdminServiceGrpcKt.AdminServiceCoroutineStub) {
        if (bearerToken == null) {
            log.info("authenticating via admin api")
            val authRequest = MeringueAdmin.AuthRequest.newBuilder()
                .setUsername(adminCreds.adminUsername)
                .setPassword(adminCreds.adminPassword)
                .build()
            val authResponse = stub.auth(authRequest)
            log.info("authenticated ok")
            this.bearerToken = authResponse.bearerToken
        }
    }

    suspend fun getRaceField(track: String) : CarData.RaceFieldResponse {
        val stub = CarDataServiceGrpcKt.CarDataServiceCoroutineStub(getGrpcChannelForTrack(track))
        return stub.getRaceField(CarData.RaceFieldRequest.newBuilder().apply {
            this.trackCode = track
        }.build())
    }

    internal fun getGrpcChannelForTrack(track: String) : GrpcChannel {
        if (!trackChannelMap.containsKey(track)) {
            val intPort = meringuePort.toInt()
            val channelBuilder = ManagedChannelBuilder.forAddress(meringueHost, meringuePort.toInt())
            if (intPort != 443) {
                channelBuilder.usePlaintext()
            }
            trackChannelMap[track] = channelBuilder.build()
        }
        return trackChannelMap[track]!!
    }

    private class BearerToken(private val value: String) : CallCredentials() {
        override fun applyRequestMetadata(
            requestInfo: RequestInfo?,
            executor: Executor,
            metadataApplier: MetadataApplier
        ) {
            executor.execute {
                try {
                    val headers = Metadata()
                    headers.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
                        "Bearer $value")
                    metadataApplier.apply(headers)
                } catch (e: Throwable) {
                    metadataApplier.fail(Status.UNAUTHENTICATED.withCause(e))
                }
            }
        }

        override fun thisUsesUnstableApi() {
            // noop
        }
    }

    inline class Waiter(private val channel: Channel<Unit> = Channel<Unit>(0)) {
        suspend fun doWait() { channel.receive() }
        fun doNotify() { channel.trySend(Unit) }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(MeringueConnector::class.java)
    }

    override fun resolveException(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any?,
        ex: java.lang.Exception
    ): ModelAndView? {
        log.warn("need to handle this ", ex)
        return null
    }
}

