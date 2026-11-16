package com.dragongsm.bloqueo;

import android.app.job.JobParameters;
import android.app.job.JobService;

/** Revisión periódica (cada 15 minutos). */
public class CheckJobService extends JobService {
    @Override
    public boolean onStartJob(final JobParameters params) {
        new Thread(() -> {
            try { Sync.run(getApplicationContext()); } catch (Throwable ignored) {}
            jobFinished(params, false);
        }, "dragon-job").start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
