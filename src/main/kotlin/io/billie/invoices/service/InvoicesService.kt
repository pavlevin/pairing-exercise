package io.billie.invoices.service

import io.billie.invoices.data.InvoicesRepository
import io.billie.invoices.data.OrganisationNotFoundException
import io.billie.invoices.model.InvoicesRequest
import io.billie.invoices.model.InvoicesResponse
import io.billie.organisations.data.OrganisationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class InvoicesService(
    private val invoicesRepo: InvoicesRepository,
    private val orgsRepo: OrganisationRepository,
    private val invoiceSender: InvoiceSender
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun createInvoice(request: InvoicesRequest): UUID {
        validateRequestForInvoiceCreation(request)
        log.info("Creating invoice from request <$request>")
        return invoicesRepo.createInvoice(request)
    }

    fun orderIsShipped(request: InvoicesRequest) : UUID {
        val invoice = getInvoiceByIdAndSupplier(request)
        return invoiceSender.sendInvoiceAndUpdateStatus(invoice)
    }

    fun getInvoiceByIdAndSupplier(request: InvoicesRequest): InvoicesResponse = invoicesRepo.getInvoiceByIdAndSupplier(request)

    private fun validateRequestForInvoiceCreation(request: InvoicesRequest) {
        val fieldNameAndIsItNull = listOf(
            "buyer" to (request.buyerId == null),
            "currency" to (request.ccy == null),
            "amount" to (request.amount == null)
        )
        if (fieldNameAndIsItNull.any { it.second }) {
            val msg = "Request for invoice creation is not valid. Missing field(s): ${
                fieldNameAndIsItNull.filter { it.second }
                    .joinToString(separator = ",", prefix = "<", postfix = ">") { it.first }
            }"
            log.error(msg)
            throw IllegalArgumentException(msg)
        }
        if (orgsRepo.getOrganisationById(request.buyerId!!) == null) {
            val msg = "Request for invoice creation is not valid. There is no such buyer with id <${request.buyerId}>"
            log.error(msg)
            throw OrganisationNotFoundException(msg)
        } else if (orgsRepo.getOrganisationById(request.supplierId) == null) {
            val msg = "Request for invoice creation is not valid. There is no such such with id <${request.buyerId}>"
            log.error(msg)
            throw OrganisationNotFoundException(msg)
        }
    }
}


