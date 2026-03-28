package com.opendatamask

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class OpenDataMaskApplication

fun main(args: Array<String>) {
    runApplication<OpenDataMaskApplication>(*args)
}
