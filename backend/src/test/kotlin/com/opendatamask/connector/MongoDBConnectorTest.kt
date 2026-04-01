package com.opendatamask.connector

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*

class MongoDBConnectorTest {

    private fun createConnector(mockClient: MongoClient): MongoDBConnector {
        return object : MongoDBConnector("mongodb://localhost:27017", "testdb") {
            override fun createMongoClient() = mockClient
        }
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
