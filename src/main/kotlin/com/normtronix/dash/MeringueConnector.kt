package com.normtronix.dash

import com.google.protobuf.Empty
import com.normtronix.meringue.AdminServiceGrpcKt
import com.normtronix.meringue.CarData
import com.normtronix.meringue.CarDataServiceGrpcKt
import com.normtronix.meringue.MeringueAdmin
import io.grpc.*
import kotlinx.coroutines.flow.Flow
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController
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

    var trackChannelMap:MutableMap<String, Channel> = mutableMapOf()

    // hold a connection to the meringue stuff
    // todo : cache last known value and refresh after ... say 30s
    suspend fun getData(track: String, car: String) : CarData.CarDataResponse {
        val stub = CarDataServiceGrpcKt.CarDataServiceCoroutineStub(getGrpcChannelForTrack(track));
        return stub.getCarData(CarData.CarDataRequest.newBuilder().apply {
            this.trackCode = track
            this.carNumber = car
        }.build())
    }

    suspend fun streamData(track: String, car: String) : Flow<CarData.CarDataResponse> {
        val stub = CarDataServiceGrpcKt.CarDataServiceCoroutineStub(getGrpcChannelForTrack(track));
        return stub.streamCarData(CarData.CarDataRequest.newBuilder().apply {
            this.trackCode = track
            this.carNumber = car
        }.build())
    }

    suspend fun getRaceData(): MeringueAdmin.RaceDataConnectionsResponse {
        val channel = getGrpcChannelForTrack("all")
        log.info("authenticating via admin api")
        val stub = AdminServiceGrpcKt.AdminServiceCoroutineStub(channel);
        val authRequest = MeringueAdmin.AuthRequest.newBuilder()
            .setUsername(adminUsername)
            .setPassword(adminPassword)
            .build()
        val authResponse = stub.auth(authRequest)
        log.info("authenticated ok")
        return stub.withCallCredentials(BearerToken(authResponse.bearerToken)).listRaceDataConnections(Empty.getDefaultInstance())
    }

    suspend fun getRaceField(track: String) : CarData.RaceFieldResponse {
        val stub = CarDataServiceGrpcKt.CarDataServiceCoroutineStub(getGrpcChannelForTrack(track));
        return stub.getRaceField(CarData.RaceFieldRequest.newBuilder().apply {
            this.trackCode = track
        }.build())
    }

    internal fun getGrpcChannelForTrack(track: String) : Channel {
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

    fun getServerAddress(): String {
        return when (meringuePort) {
            "443" -> "https://{meringueHost}"
            else -> "http://${meringueHost}:${meringuePort}"
        }
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

    companion object {
        val log: Logger = LoggerFactory.getLogger(MeringueConnector::class.java)
    }
}

