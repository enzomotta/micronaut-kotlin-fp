package com.enzomotta

import arrow.fx.coroutines.Environment
import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.RxStreamingHttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.reactivex.Flowable
import kotlinx.coroutines.flow.Flow
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.URL
import javax.inject.Inject

@Client("/headlinesWithFlow")
interface HeadlineFlowClient {

    @Get(value = "/headlinesWithFlow", processes = [MediaType.APPLICATION_JSON_STREAM])
    fun streamFlow(): Flow<Headline>
}

@MicronautTest
class MicronautKotlinFpTest {

    @Inject
    lateinit var application: EmbeddedApplication<*>

    @Inject
    var server: EmbeddedServer? = null

    @Inject
    lateinit var client: HeadlineFlowClient

    private val env = Environment()

    @Test
    fun testItWorks() {
        Assertions.assertTrue(application.isRunning)
    }

    @Test
    fun testFindAllStream() {
        val client = RxStreamingHttpClient.create(URL("http://" + server!!.host + ":" + server!!.port))
        val stream : Flowable<PositionTest> =  client.jsonStream<Unit, PositionTest>(HttpRequest.GET("/listings?description=kotlin"), PositionTest::class.java)
        val positions = stream.toList().blockingGet()
        positions.size shouldBeGreaterThan 0
    }

}

class PositionTest {
    var id: String? = null
    var  type: String? = null
    var  url: String? = null
    var  createdAt: String?  = null
    var  company: String? = null
    var  companyUrl: String? = null
    var  location: String? = null
    var  title: String? = null
    var  description: String? = null
    var  howToApply: String? = null
    var  companyLogo: String? = null
    var  starred: Number = 0
    override fun toString(): String {
        return "PositionTest(id='$id', type='$type', url='$url', createdAt='$createdAt', company='$company', companyUrl=$companyUrl, location='$location', title='$title', description='$description', howToApply='$howToApply', companyLogo=$companyLogo, starred=$starred)"
    }


}