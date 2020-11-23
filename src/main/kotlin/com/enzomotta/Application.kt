package com.enzomotta

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import arrow.core.computations.either
import arrow.core.getOrHandle
import arrow.fx.coroutines.Environment
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Put
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.uri.UriBuilder
import io.micronaut.runtime.Micronaut.build
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.reactivex.Flowable
import javax.inject.Singleton


fun main(args: Array<String>) {
	build()
		.args(*args)
		.packages("com.enzomotta")
		.start()
}

@Controller
class MainController(private val mainService: MainService) {

	private val env = Environment()

	@Get("/listings{?description}{?location}")
	@ExecuteOn(TaskExecutors.IO)
	fun listPositions(description: String?, location: String?): HttpResponse<*> {
		val response= env.unsafeRunSync {
			mainService.listPositions(Description(description.orEmpty()), location.orEmpty())
		}
		return response.map<HttpResponse<*>> {
			HttpResponse.ok(it)
		}.getOrHandle {
			HttpResponse.serverError(it.message)
		}
	}

	@Put("/listings/{positionId}/star")
	fun starPosition(@PathVariable positionId: String): HttpResponse<*> {
		return HttpResponse.ok("Hello World")
	}

}

@Singleton
class MainService(private val jobsDirectory: JobsDirectory) {

	suspend fun listPositions(description: Description, location: String): Either<Exception, Flowable<Position>>  =
		either {
			val positions = jobsDirectory.searchBy(description = description, location = location)
			positions
		}
}

data class Position(
	val id: String,
	val type: String,
	val url: String,
	val createdAt: String,
	val company: String,
	val companyUrl: String?,
	val location: String,
	val title: String,
	val description: String,
	val howToApply: String,
	val companyLogo: String?,
	var starred: Number = 0,
)

inline class Description(val value: String)

interface JobsDirectory {
	suspend fun searchBy(description: Description, location: String): Flowable<Position>
	fun searchById(Id: String): Either<Unit, Position>
}

/*
interface Bill {
	lateinit var billId: BillId
	lateinit var accountId: AccountId
	lateinit var description: String
	lateinit var dueDate: String
	lateinit var billType: BillType
	var created: Long = 0
	var payer: Payer? = null
	var assignor: String? = null
	var paymentLimitTime: String? = null
	var amountTotal: Long = 0
	var recipient: Recipient? = null
	var paidDate: Long? = null
	var status: BillStatus = BillStatus.PENDING
	var lastRegisterUpdate: Long = 0
	@JsonIgnore
	var history: MutableList<BillEvent> = mutableListOf()
	lateinit var source: ActionSource
	var paymentScheduled: Boolean = false
	var scheduledDate: String? = null
}

class Boleto(): Bill {
	var barcode: BarCode? = null
}

class FichaCompensacao(private val boleto: Boleto) : Bill by boleto {
	var originalAmount: Long = 0
	var discount: Long = 0
	var fine: Long = 0
	var interest: Long = 0
	var amountCalculationModel: String? = null
	var expirationDate: String? = null
	var lastSettleDate: String? = null
}

class BoletoConcessionaria() : Boleto()

class BillTED(): Cobranca()

class BillPIX(): Cobranca()

*/



@Singleton
class GitHubJobsAdapter(@param:Client(value = "https://jobs.github.com") private val httpClient: RxHttpClient) : JobsDirectory {

	override suspend fun searchBy(description: Description, location: String): Flowable<Position> {
		val uri = UriBuilder.of("/positions.json").queryParam("search", description.value).build()
		val httpRequest = HttpRequest.GET<PositionTO>(uri)
			.contentType(MediaType.APPLICATION_JSON)
			.accept(MediaType.APPLICATION_JSON_TYPE)
		val call = httpClient.exchange(
			httpRequest,
			Argument.listOf(PositionTO::class.java),
			Argument.STRING)
		val response = call.firstOrError().blockingGet()
		val positions = response.getBody(Argument.listOf(PositionTO::class.java))
		if (response.status == HttpStatus.NO_CONTENT || positions.isEmpty) {
			return Flowable.empty()
		}
		return Flowable.fromIterable(positions.get().map { to -> to.toPosition() })
	}

	override fun searchById(Id: String): Either<Unit, Position> {
		TODO("Not yet implemented")
	}

}

interface StarRepository {
	@Singleton
	class InMemoryStarRepository(): StarRepository {

		private val positions  = mutableMapOf<String, Position>()

		override suspend fun findById(id: String): Either<Unit, Position> {
			return positions[id]?.let { Right(it) } ?: Left(Unit)
		}

		override suspend fun findAll(): Flowable<Position> {
			return Flowable.fromIterable(positions.values)
		}

		override suspend fun save(position: Position) {
			positions.putIfAbsent(position.id, position)
		}


	}
	suspend fun findById(Id: String): Either<Unit, Position>
	suspend fun findAll(): Flowable<Position>
	suspend fun save(position: Position)
}


data class PositionTO(
	val id: String,
	val type: String,
	val url: String,
	@JsonProperty("created_at") val createdAt: String,
	val company: String,
	@JsonProperty("company_url") val companyUrl: String?,
	val location: String,
	val title: String,
	val description: String,
	@JsonProperty("how_to_apply") val howToApply: String,
	@JsonProperty("company_logo") val companyLogo: String?,
) {
	fun toPosition() = Position(
		id = id,
		type = type,
		url = url,
		createdAt = createdAt,
		company = company,
		companyUrl = companyUrl,
		location = location,
		title = title,
		description = description,
		howToApply = howToApply,
		companyLogo = companyLogo
	)
}