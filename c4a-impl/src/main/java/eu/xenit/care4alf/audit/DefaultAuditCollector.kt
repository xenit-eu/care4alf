package eu.xenit.care4alf.audit

import com.fasterxml.jackson.databind.ObjectMapper
import org.alfresco.service.cmr.audit.AuditService.AuditQueryCallback
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter
import org.alfresco.service.cmr.repository.datatype.TypeConversionException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

class DefaultAuditCollector : AuditQueryCallback {
    var entries: MutableList<Entry> = ArrayList()
        private set

    override fun handleAuditEntry(entryId: Long, applicationName: String, user: String?, time: Long,
                                  values: MutableMap<String, Serializable>?): Boolean {
        var entry = Entry(entryId, applicationName, Date(time), user, null);
        if (user != null) {
            entry.user
        }
        if (values != null) {
            val stringvals = values.mapValues { makeString(it.value) }
            entry.values = stringvals;
        }
        entries.add(entry)

        return true
    }

    private fun makeString(value: Serializable): String = try {
        DefaultTypeConverter.INSTANCE.convert(String::class.java, value)
    } catch (e: TypeConversionException) {
        value.toString()
    }

    override fun valuesRequired() = true;

    override fun handleAuditEntryError(entryId: Long?, errorMsg: String?, error: Throwable?) = true

}

data class Entry(val id: Long,
                 val application: String,
                 val time: Date,
                 val user: String?,
                 var values: Map<String, Serializable>?): Serializable
