package com.mongodb.memphis.mutators;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonValue;

import com.mongodb.memphis.Config;

public class EpochMutator implements Mutator {

	private final Config config;

	public EpochMutator(Config config) {
		this.config = config;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.mongodb.johnlpage.scalingtest.queryrun.mutators.QueryMutator#mutate(com.
	 * mongodb.johnlpage.scalingtest.queryrun.query.QueryTemplate)
	 */
	@Override
	public void mutate(BsonDocument document) {
		for (String epochIndex : config.getEpochIndex()) {
			BsonValue queryValue = document.get(epochIndex);
			if (queryValue != null && queryValue.isString()) {
				String value = queryValue.asString().getValue();
				long epoch = 0l;
				if (value.length() > 10) {
					epoch = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(value)).getEpochSecond();
				} else {
					epoch = LocalDate.from(DateTimeFormatter.ISO_DATE.parse(value)).toEpochDay();
				}
				document.put(epochIndex + config.getEpochSuffix(), new BsonInt32((int) epoch));
			}
		}
	}

	public String toString() {
		return getClass().getSimpleName() + " [epochSuffix=" + config.getEpochSuffix() + ", EpochFields=" + config.getEpochIndex() + "]";
	}

}