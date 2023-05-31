package io.billie.invoices.resource

import io.billie.invoices.data.InvoiceNotFoundException
import io.billie.invoices.model.InvoicesRequest
import io.billie.invoices.model.InvoicesResponse
import io.billie.invoices.service.InvoicesService
import io.billie.organisations.viewmodel.*
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*
import javax.validation.Valid


@RestController
@RequestMapping("invoices")
class InvoicesResource(val service: InvoicesService) {

    @GetMapping
    fun getInvoiceByInvoiceId(@RequestParam("invoice_id") invoiceId: String, @RequestParam("supplier_id") supplierId: String): InvoicesResponse {
        return service.getInvoiceByIdAndSupplier(InvoicesRequest(invoiceId, supplierId = supplierId))
    }

    @PostMapping
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Accepted the new invoice",
                content = [
                    (Content(
                        mediaType = "application/json",
                        array = (ArraySchema(schema = Schema(implementation = Entity::class)))
                    ))]
            ),
            ApiResponse(responseCode = "400", description = "Bad request", content = [Content()])]
    )
    fun createInvoice(@Valid @RequestBody request: InvoicesRequest): Entity {
        try {
            val id = service.createInvoice(request)
            return Entity(id)
        } catch (e: Exception) {
            throw ResponseStatusException(BAD_REQUEST, e.message)
        }
    }

    @PostMapping("/shipped")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Order is shipped successfully",
                content = [
                    (Content(
                        mediaType = "application/json",
                        array = (ArraySchema(schema = Schema(implementation = Entity::class)))
                    ))]
            ),
            ApiResponse(responseCode = "400", description = "Bad request", content = [Content()])]
    )
    fun orderIsShipped(@Valid @RequestBody request: InvoicesRequest): Entity {
        try {
            val id = service.orderIsShipped(request)
            return Entity(id)
        } catch (e: InvoiceNotFoundException) {
            throw ResponseStatusException(BAD_REQUEST, e.message)
        }catch (e: IllegalStateException) {
            throw ResponseStatusException(BAD_REQUEST, e.message)
        } catch (e: Exception) {
            throw ResponseStatusException(INTERNAL_SERVER_ERROR, e.message)
        }
    }

}
