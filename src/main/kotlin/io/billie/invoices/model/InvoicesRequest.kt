package io.billie.invoices.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.util.*
import javax.validation.constraints.NotBlank

data class InvoicesRequest(
    @field:NotBlank @JsonProperty("invoice_id") val invoiceId: String,
    @JsonProperty("buyer_id") val buyerId: String? = null,
    @field:NotBlank @JsonProperty("supplier_id") val supplierId: String,
    @JsonProperty val amount: BigDecimal? = null,
    @JsonProperty val ccy: Currency? = null,
)
