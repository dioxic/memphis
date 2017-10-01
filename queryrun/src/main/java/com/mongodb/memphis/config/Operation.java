package com.mongodb.memphis.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.bson.BsonDocument;
import org.bson.RawBsonDocument;

import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import com.mongodb.memphis.DocumentPool;
import com.mongodb.memphis.engine.Results;
import com.mongodb.memphis.util.StringUtils;

public abstract class Operation extends Config {

	protected int threads = 1;
	protected List<Template> templates;

	public final List<Template> getTemplates() {
		return templates;
	}

	@Override
	public List<Template> getChildren() {
		return templates;
	}

	public int getThreads() {
		return threads;
	}

	protected abstract int getIterations();

	protected abstract DocumentPool createDocumentPool();

	protected abstract void execute(MongoCollection<BsonDocument> collection, List<BsonDocument> documents);

	@Override
	public void execute() {
		logger.info("Operation {} starting", getClass().getSimpleName());

		ExecutorService executor = Executors.newFixedThreadPool(threads);
		List<Worker> tasks = new ArrayList<>(threads);

		for (int i = 0; i < threads; i++) {
			tasks.add(new Worker(i));
		}

		long startTime = System.currentTimeMillis();

		try {
			// collect the query times once the threads have completed
			List<Results> results = executor.invokeAll(tasks).stream().map(future -> {
				try {
					return future.get();
				}
				catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				}
			}).collect(Collectors.toList());
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		long totalTime = System.currentTimeMillis() - startTime;

		executor.shutdown();

		logger.info("Operation {} completed in {}", getClass().getSimpleName(), StringUtils.prettifyTime(totalTime));
	}

	class Worker implements Callable<Results> {
		int threadNum;
		Results results;

		public Worker(int threadNumber) {
			this.threadNum = threadNumber;
			results = new Results();
		}

		@Override
		public Results call() throws Exception {
			logger.debug("Thread {} starting", threadNum);
			try {
				results.initialise(getIterations());
				long rawSize = 0;

				DocumentPool docPool = createDocumentPool();
				MongoCollection<BsonDocument> collection = getMongoCollection();

				for (int counter = 0; counter < getIterations(); counter++) {
					logger.trace("Thread {} running insert iteration {}", threadNum, counter);

					docPool.regenerateValues();

					if (rawSize == 0L) {
						rawSize = docPool.getAverageDocumentSize();
					}

					long startTime = System.currentTimeMillis();
					execute(collection, docPool.getDocuments());
					results.setOperationTime(counter, System.currentTimeMillis() - startTime);
					results.incSize(rawSize * 10);
					results.incRecordCount(10);
				}
			}
			catch (Exception e) {
				logger.error(e.getMessage(), e);
				throw e;
			}

			logger.debug("Thread {} complete processed {} operations", threadNum, getIterations());
			return results;
		}

		Block<RawBsonDocument> printBlock = new Block<RawBsonDocument>() {
			@Override
			public void apply(final RawBsonDocument document) {
				if (logger.isTraceEnabled()) {
					logger.trace("document returned: {}", document.toJson());
				}
			}
		};

	}

}