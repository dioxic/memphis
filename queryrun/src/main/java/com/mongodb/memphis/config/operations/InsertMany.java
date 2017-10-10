package com.mongodb.memphis.config.operations;

import org.bson.BsonDocument;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.memphis.annotations.Name;
import com.mongodb.memphis.config.Operation;
import com.mongodb.memphis.engine.BatchDocumentPool;
import com.mongodb.memphis.engine.Results;

@Name("insertMany")
public class InsertMany extends Operation<BatchDocumentPool> {

	private int totalDocuments;
	private int batchSize;
	private InsertManyOptions options;

	public final int getTotalDocuments() {
		return totalDocuments;
	}

	public final int getBatchSize() {
		return batchSize;
	}

	public final InsertManyOptions getOptions() {
		return options;
	}

	@Override
	public int getIterations() {
		return totalDocuments / batchSize / getThreads();
	}

	@Override
	protected void execute(MongoCollection<BsonDocument> collection, BatchDocumentPool documentPool, Results results) {
		collection.insertMany(documentPool.getDocuments(), options);
		results.bytesWritten(documentPool.getAverageDocumentSize() * batchSize);
		results.docsWritten(batchSize);
	}

	@Override
	protected BatchDocumentPool createDocumentPool() {
		return new BatchDocumentPool(templates, batchSize);
	}

}
