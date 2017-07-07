package org.botellier.log

import org.junit.Test
import org.junit.Assert
import java.io.File
import java.util.*

class LogTest {
    fun log(root: String, size: Int, clear: Boolean = false, f: Log.() -> Unit) {
        val log = Log(root, "test-segment-", size, clear)
        log.f()
        log.clear()
    }

    @Test
    fun segmentSize() {
        val before = "before".toByteArray()
        val after = "after".toByteArray()
        withDummy { log(it.toString(), 10) {
            set("key", before, after)
            set("key", before, after)
            set("key", before, after)
            Assert.assertTrue(File(segments[1].path.toUri()).exists())
            Assert.assertTrue(File(segments[2].path.toUri()).exists())
        }}
    }

    @Test
    fun iteratingEntries() {
        val before = "before".toByteArray()
        val after = "after".toByteArray()

        withDummy { log(it.toString(),10) {
            set("key", before, after)
            set("key", before, after)
            set("key", before, after)

            val res = fold(Pair("", ""), { (keys, datas), entry ->
                if (entry is SetEntry) {
                    Pair(keys + entry.key, datas + String(entry.before) + String(entry.after))
                } else {
                    Pair(keys, datas)
                }
            })

            Assert.assertEquals(Pair("keykeykey", "beforeafterbeforeafterbeforeafter"), res)
        }}
    }

    @Test
    fun findSegments() {
        withDummy(3) { log(it.toString(), 10) {
            Assert.assertEquals(3, segments.size)
            Assert.assertTrue(toList().isEmpty())
        }}
    }

    @Test
    fun clearSegments() {
        withDummy(3) { log(it.toString(), 10, true) {
            Assert.assertEquals(1, segments.size)
            Assert.assertTrue(toList().isEmpty())
        }}
    }
}