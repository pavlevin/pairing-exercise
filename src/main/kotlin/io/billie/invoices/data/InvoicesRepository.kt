package io.billie.invoices.data

import io.billie.invoices.model.Currency
import io.billie.invoices.model.InvoiceStatus
import io.billie.invoices.model.InvoicesRequest
import io.billie.invoices.model.InvoicesResponse
import io.billie.organisations.data.OrganisationRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.support.KeyHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDateTime.now
import java.util.UUID

@Repository
class InvoicesRepository(
    private val orgRepo: OrganisationRepository,
    private val jdbcTemplate: JdbcTemplate
) {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    @Transactional(readOnly = true)
    fun getInvoiceByIdAndSupplier(request: InvoicesRequest): InvoicesResponse {
        val invoices = jdbcTemplate.query(
            "select id, invoice_id, buyer_id, supplier_id, status, amount, ccy, invoice_date, payment_date " +
                    "from organisations_schema.invoices " +
                    "where invoice_id = ? " +
                    "and supplier_id = ?::uuid",
            invoiceMapper(),
            request.invoiceId,
            request.supplierId
        )
        when {
            invoices.size == 0 -> {
                val msg = "There is no such invoice with id <${request.invoiceId}> for supplier <${request.supplierId}>"
                log.error(msg)
                throw InvoiceNotFoundException(msg)
            }
            invoices.size > 1 -> {
                val msg = "There is more that one invoice with invoice_id <${request.invoiceId}> for supplier <${request.supplierId}>"
                log.error(msg)
                throw MultipleInvoicesFoundException(msg)
            }
            else -> return invoices.single()
        }
    }

    @Transactional
    fun createInvoice(request: InvoicesRequest): UUID {
        val sql =
            "INSERT INTO organisations_schema.invoices(invoice_id, buyer_id, supplier_id, status, amount, ccy, invoice_date, updated_by) " +
                    "values(?, ?, ?, ?, ?, ?, ?, ?)"
        val keyHolder: KeyHolder = GeneratedKeyHolder()
        jdbcTemplate.update(
            { connection ->
                val ps = connection.prepareStatement(sql, arrayOf("id"))
                ps.setObject(1, request.invoiceId)
                ps.setObject(2, UUID.fromString(request.buyerId))
                ps.setObject(3, UUID.fromString(request.supplierId))
                ps.setString(4, InvoiceStatus.NEW.name)
                ps.setBigDecimal(5, request.amount)
                ps.setString(6, request.ccy!!.name)
                ps.setDate(7, Date(System.currentTimeMillis()))
                ps.setString(8, "invoices_service")
                ps
            }, keyHolder
        )
        return keyHolder.getKeyAs(UUID::class.java)!!
    }

    @Transactional
    fun updateInvoiceStatus(invoiceId: UUID, status: InvoiceStatus): UUID {
        val sql = "UPDATE organisations_schema.invoices set status = ? where id::uuid = ?::uuid"
        val keyHolder: KeyHolder = GeneratedKeyHolder()
        jdbcTemplate.update(
            { connection ->
                val ps = connection.prepareStatement(sql, arrayOf("id"))
                ps.setString(1, status.name)
                ps.setObject(2, invoiceId)
                ps
            }, keyHolder
        )
        return keyHolder.getKeyAs(UUID::class.java)!!
    }

    private fun invoiceMapper(): RowMapper<InvoicesResponse> = RowMapper { rs, _ ->
        val id = rs.getString(1)
        val invoiceId = rs.getString(2)
        val buyer = orgRepo.getOrganisationById(rs.getString(3))
        val supplier = orgRepo.getOrganisationById(rs.getString(4))
        val status = InvoiceStatus.valueOf(rs.getString(5))
        val amount = rs.getBigDecimal(6)
        val ccy = Currency.valueOf(rs.getString(7))
        val invoiceDate = rs.getDate(8)
        val paymentDate = rs.getDate(9)
        InvoicesResponse(
            UUID.fromString(id),
            invoiceId,
            buyer!!,
            supplier!!,
            status,
            amount,
            ccy,
            invoiceDate?.toLocalDate(),
            paymentDate?.toLocalDate()
        )
    }


}
