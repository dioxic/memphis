package com.mongodb.memphis.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bson.BsonValue;

import com.mongodb.memphis.data.DataCache;
import com.mongodb.memphis.placeholder.Placeholder;

public abstract class Generator extends Placeholder implements Cloneable {

	protected boolean batchMode = false;
	protected int uniqueValues;
	private String fieldKey;
	private String cacheKey;

	private transient List<BsonValue> valueCache;
	private transient BsonValue currentValue;
	protected transient final Random random = new Random();

	@Override
	public BsonValue getValue() {
		if (!batchMode && cacheKey == null) {
			currentValue = getNextValue();
		}
		return currentValue;
	}

	private BsonValue getNextValue() {
		// if we have a list of values or a fixed number of unique values set
		if (valueCache != null) {
			return valueCache.get(random.nextInt(valueCache.size()));
		}
		else {
			return generateValue();
		}
	}

	@Override
	public void nextBatch(int iteration) {
		if (cacheKey != null) {
			currentValue = DataCache.getValue(cacheKey, fieldKey, Thread.currentThread(), iteration);
			if (currentValue == null) {
				throw new IllegalStateException("Could not find fieldKey:" + fieldKey + " in data cache " + cacheKey);
			}
		}
		else if (batchMode) {
			currentValue = getNextValue();
		}
	}

	@Override
	public void initialise() {
		super.initialise();
		List<BsonValue> list = getListValues();
		if (list != null) {
			valueCache = list;
		}
		else if (uniqueValues > 0) {
			valueCache = new ArrayList<>(uniqueValues);
			for (int i = 0; i < uniqueValues; i++) {
				valueCache.add(generateValue());
			}
		}
	}

	protected abstract BsonValue generateValue();

	protected List<BsonValue> getListValues() {
		return null;
	}

}