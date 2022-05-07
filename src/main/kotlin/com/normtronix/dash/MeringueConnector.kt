package com.normtronix.dash

import com.google.protobuf.Empty
import com.normtronix.meringue.AdminServiceGrpcKt
import com.normtronix.meringue.CarData
import com.normtronix.meringue.CarDataServiceGrpcKt
import com.normtronix.meringue.MeringueAdmin
import io.grpc.CallCredentials
import io.grpc.Channel as GrpcChannel
import io.grpc.Metadata
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.Executor


@Service
class MeringueConnector {

    @Value("\${meringueHost}")
    lateinit var meringueHost:String

    @Value("\${meringuePort}")
    lateinit var meringuePort:String

    @Value("\${adminUsername}")
    lateinit var adminUsername:String

    @Value("\${adminPassword}")
    lateinit var adminPassword:String

    var trackChannelMap:MutableMap<String, GrpcChannel> = mutableMapOf()
    var carTrackSharedFlow:MutableMap<String, MutableStateFlow<CarData.CarDataResponse>> = mutableMapOf()

    // hold a connection to the meringue stuff
    // todo : cache last known value and refresh after ... say 30s
    suspend fun getData(track: String, car: String) : CarData.CarDataResponse {
        val stub = CarDataServiceGrpcKt.CarDataServiceCoroutineStub(getGrpcChannelForTrack(track))
        return stub.getCarData(CarData.CarDataRequest.newBuilder().apply {
            this.trackCode = track
            this.carNumber = car
        }.build())
    }

    suspend fun streamData(track: String, car: String) : StateFlow<CarData.CarDataResponse> {
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

    suspend fun getRaceData(): MeringueAdmin.RaceDataConnectionsResponse {
        val channel = getGrpcChannelForTrack("all")
        // todo : stop this from always authing ... store token locally and re-use
        log.info("authenticating via admin api")
        val stub = AdminServiceGrpcKt.AdminServiceCoroutineStub(channel)
        val authRequest = MeringueAdmin.AuthRequest.newBuilder()
            .setUsername(adminUsername)
            .setPassword(adminPassword)
            .build()
        val authResponse = stub.auth(authRequest)
        log.info("authenticated ok")
        return stub.withCallCredentials(BearerToken(authResponse.bearerToken)).listRaceDataConnections(Empty.getDefaultInstance())
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
}

