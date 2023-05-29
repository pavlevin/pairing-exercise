package io.billie.utils

import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BillieQuartzScheduler {

    private val schedulerFactory: SchedulerFactory = StdSchedulerFactory()
    private val scheduler: Scheduler = schedulerFactory.scheduler
    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    init {
        scheduler.start()
    }

    fun scheduleJob(jobDetail: JobDetail, trigger: Trigger) {
        log.info("Scheduling job <${jobDetail.jobClass.simpleName}>")
        scheduler.scheduleJob(jobDetail, trigger)
    }

}