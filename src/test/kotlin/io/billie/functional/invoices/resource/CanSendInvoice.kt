package io.billie.functional.invoices.resource

import com.fasterxml.jackson.databind.ObjectMapper
import io.billie.functional.data.Fixtures
import io.billie.invoices.model.Currency
import io.billie.invoices.model.InvoiceStatus
import io.billie.invoices.model.InvoicesRequest
import io.billie.invoices.model.InvoicesResponse
import io.billie.organisations.viewmodel.Entity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal
import java.util.*

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class CanSendInvoice {

    @LocalServerPort
    private val port = 8080

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var mapper: ObjectMapper

    @Autowired
    private lateinit var template: JdbcTemplate

    @Test
    fun `Invoice successfully sent to buyer`() {
        val supplierId = postOrg(Fixtures.orgRequestJson()).toString()
        val buyerId = postOrg(Fixtures.orgRequestJson()).toString()
        val invoiceId = UUID.randomUUID().toString()
        val invoicesRequest =
            InvoicesRequest(invoiceId, buyerId, supplierId, BigDecimal.valueOf(12345.67), Currency.EUR)
        postInvoice(invoicesRequest).andExpect(MockMvcResultMatchers.status().isOk)
        val shippingNotification = InvoicesRequest(invoiceId, supplierId = supplierId)
        mockMvc.perform(
            MockMvcRequestBuilders.post("/invoices/shipped")
                .content(mapper.writeValueAsString(shippingNotification))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)

        val invoiceResponse = mockMvc.perform(
            MockMvcRequestBuilders.get("/invoices")
                .param("invoiceId", invoiceId)
                .param("supplierId", invoicesRequest.supplierId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val invoice = mapper.readValue(invoiceResponse.response.contentAsString, InvoicesResponse::class.java)
        assertEquals(InvoiceStatus.SENT, invoice.status)

        template.update("DELETE FROM organisations_schema.invoices where invoice_id = '${invoicesRequest.invoiceId}'")
        template.update("DELETE FROM organisations_schema.organisations where id in ('$supplierId', '$buyerId')")
        template.update("DELETE FROM organisations_schema.contact_details where id in ('${invoice.supplier.contactDetails.id}'::uuid, '${invoice.buyer.contactDetails.id}'::uuid)")
    }


    @Test
    fun `Invoice is not sent to buyer cause it was not previously received`() {
        val supplierId = postOrg(Fixtures.orgRequestJson()).toString()
        val buyerId = postOrg(Fixtures.orgRequestJson()).toString()
        val invoiceId = UUID.randomUUID().toString()
        val shippingNotification = InvoicesRequest(invoiceId, supplierId = supplierId)
        mockMvc.perform(
            MockMvcRequestBuilders.post("/invoices/shipped")
                .content(mapper.writeValueAsString(shippingNotification))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)

        template.update("DELETE FROM organisations_schema.organisations where id in ('$supplierId', '$buyerId')")
    }
    fun postInvoice(request: InvoicesRequest): ResultActions {
        return mockMvc.perform(
            MockMvcRequestBuilders.post("/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
    }

    fun postOrg(request: String): UUID {
        val buyer = mockMvc.perform(
            MockMvcRequestBuilders.post("/organisations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        return mapper.readValue(buyer.response.contentAsString, Entity::class.java).id
    }
}