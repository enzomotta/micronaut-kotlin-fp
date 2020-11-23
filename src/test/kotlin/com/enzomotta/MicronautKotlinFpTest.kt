package com.enzomotta
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.RxStreamingHttpClient
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.reactivex.Flowable
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URL
import javax.inject.Inject

@MicronautTest
class MicronautKotlinFpTest {

    @Inject
    lateinit var application: EmbeddedApplication<*>

    @Inject
    var server: EmbeddedServer? = null

    @Test
    fun testItWorks() {
        Assertions.assertTrue(application.isRunning)
    }

    @Test
    fun testFindAllStream() {
        val client = RxStreamingHttpClient.create(URL("http://" + server!!.host + ":" + server!!.port))
        val stream : Flowable<Position> =  client.jsonStream<Unit, Position>(HttpRequest.GET("/listings?description=kotlin"), Position::class.java)
        val positions = stream.toList().blockingGet()
        assertTrue(positions.size > 0)
    }

}
