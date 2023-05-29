package io.billie.invoices.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.billie.organisations.viewmodel.OrganisationResponse
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Table("INVOICES")
data class InvoicesResponse(
    val id: UUID,
    @JsonProperty("invoice_id") val invoiceId: String,
    @JsonProperty val buyer: OrganisationResponse,
    @JsonProperty val supplier: OrganisationResponse,
    @JsonProperty val status: InvoiceStatus,
    @JsonProperty val amount: BigDecimal,
    @JsonProperty val ccy: Currency,
    @JsonProperty("invoice_date") val invoiceDate: LocalDate?,
    @JsonProperty("payment_date") val paymentDate: LocalDate?,

    )
