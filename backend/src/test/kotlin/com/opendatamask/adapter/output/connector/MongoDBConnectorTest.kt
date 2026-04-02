package com.opendatamask.adapter.output.connector

import com.mongodb.client.FindIterable
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito
import org.mockito.kotlin.*

class MongoDBConnectorTest {

    private fun createConnector(mockClient: MongoClient): MongoDBConnector {
        return object : MongoDBConnector("mongodb://localhost:27017", "testdb") {
            override fun createMongoClient() = mockClient
        }
    }

    /**
     * Returns a FindIterable mock that uses RETURNS_DEEP_STUBS so that the
     * MongoIterable.map(...).toList() call chain in fetchData() works without NPE.
     * fetchData() will return an empty list, but the filter/limit arguments
     * passed to find()/limit() can still be verified.
     */
    private fun mockFindIterable(): FindIterable<Document> {
        val iterable = mock<FindIterable<Document>>(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        whenever(iterable.limit(any<Int>())).thenReturn(iterable)
        return iterable
    }

    @Test
    fun `fetchData uses empty filter when no whereClause provided`() {
        val mockCollection = mock<MongoCollection<Document>>()
        val mockDb = mock<MongoDatabase>()
        val mockClient = mock<MongoClient>()
        val findIterable = mockFindIterable()

        whenever(mockClient.getDatabase("testdb")).thenReturn(mockDb)
        whenever(mockDb.getCollection("users")).thenReturn(mockCollection)
        whenever(mockCollection.find(any<Document>())).thenReturn(findIterable)

        val connector = createConnector(mockClient)
        connector.fetchData("users")

        verify(mockCollection).find(Document())
    }

    @Test
    fun `fetchData applies JSON filter when whereClause provided`() {
        val mockCollection = mock<MongoCollection<Document>>()
        val mockDb = mock<MongoDatabase>()
        val mockClient = mock<MongoClient>()
        val findIterable = mockFindIterable()
        val expectedFilter = Document.parse("""{"age": 18}""")

        whenever(mockClient.getDatabase("testdb")).thenReturn(mockDb)
        whenever(mockDb.getCollection("users")).thenReturn(mockCollection)
        whenever(mockCollection.find(any<Document>())).thenReturn(findIterable)

        val connector = createConnector(mockClient)
        connector.fetchData("users", whereClause = """{"age": 18}""")

        verify(mockCollection).find(expectedFilter)
    }

    @Test
    fun `fetchData applies limit when specified`() {
        val mockCollection = mock<MongoCollection<Document>>()
        val mockDb = mock<MongoDatabase>()
        val mockClient = mock<MongoClient>()
        val findIterable = mockFindIterable()

        whenever(mockClient.getDatabase("testdb")).thenReturn(mockDb)
        whenever(mockDb.getCollection("users")).thenReturn(mockCollection)
        whenever(mockCollection.find(any<Document>())).thenReturn(findIterable)

        val connector = createConnector(mockClient)
        connector.fetchData("users", limit = 5)

        verify(findIterable).limit(5)
    }

    @Test
    fun `writeData calls insertMany and returns row count`() {
        val mockCollection = mock<MongoCollection<Document>>()
        val mockDb = mock<MongoDatabase>()
        val mockClient = mock<MongoClient>()
        whenever(mockClient.getDatabase("testdb")).thenReturn(mockDb)
        whenever(mockDb.getCollection("users")).thenReturn(mockCollection)

        val connector = createConnector(mockClient)
        val rows = listOf(
            mapOf("name" to "Alice", "email" to "alice@example.com"),
            mapOf("name" to "Bob", "email" to "bob@example.com")
        )
        val count = connector.writeData("users", rows)
        assertEquals(2, count)
        verify(mockCollection).insertMany(any())
    }

    @Test
    fun `writeData returns 0 for empty list`() {
        val mockClient = mock<MongoClient>()
        val connector = createConnector(mockClient)
        assertEquals(0, connector.writeData("users", emptyList()))
        verifyNoInteractions(mockClient)
    }

    @Test
    fun `createTable creates collection if not exists`() {
        val mockDb = mock<MongoDatabase>()
        val mockClient = mock<MongoClient>()
        whenever(mockClient.getDatabase("testdb")).thenReturn(mockDb)

        val connector = createConnector(mockClient)
        connector.createTable("newcoll", emptyList())
        verify(mockDb).createCollection("newcoll")
    }

    @Test
    fun `truncateTable calls deleteMany`() {
        val mockCollection = mock<MongoCollection<Document>>()
        val mockDb = mock<MongoDatabase>()
        val mockClient = mock<MongoClient>()
        whenever(mockClient.getDatabase("testdb")).thenReturn(mockDb)
        whenever(mockDb.getCollection("users")).thenReturn(mockCollection)

        val connector = createConnector(mockClient)
        connector.truncateTable("users")
        verify(mockCollection).deleteMany(any())
    }
}
