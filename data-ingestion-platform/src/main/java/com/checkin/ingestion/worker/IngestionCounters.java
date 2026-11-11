package com.checkin.ingestion.worker;

import java.util.concurrent.atomic.AtomicLong;

public class IngestionCounters {
    public final AtomicLong processed = new AtomicLong(0);
    public final AtomicLong errors = new AtomicLong(0);
    public final AtomicLong stored = new AtomicLong(0);
}
