package com.opendatamask.domain.port

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service

/** Scanner that also detects interfaces (Spring Data JPA repos are interfaces, not classes). */
private fun interfaceAwareScanner() = object : ClassPathScanningCandidateComponentProvider(false) {
    override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition) =
        beanDefinition.metadata.isIndependent
}

class PortContractTest {

    @Test
    fun `every application service implements at most one use-case input port`() {
        val scanner = interfaceAwareScanner()
        scanner.addIncludeFilter(AnnotationTypeFilter(Service::class.java))

        val serviceClasses = scanner
            .findCandidateComponents("com.opendatamask.application.service")
            .mapNotNull { runCatching { Class.forName(it.beanClassName) }.getOrNull() }

        assertTrue(serviceClasses.isNotEmpty(), "No @Service classes found in application.service")

        val inputPortPrefix = "com.opendatamask.domain.port.input"
        val violations = serviceClasses.filter { svc ->
            svc.interfaces.count { it.name.startsWith(inputPortPrefix) } > 1
        }

        assertTrue(violations.isEmpty(),
            "Services implementing more than one input port: ${violations.map { it.simpleName }}")
    }

    @Test
    fun `every JPA repository in persistence adapter implements its output port`() {
        val scanner = interfaceAwareScanner()
        scanner.addIncludeFilter(AnnotationTypeFilter(Repository::class.java))

        val repoClasses = scanner
            .findCandidateComponents("com.opendatamask.adapter.output.persistence")
            .mapNotNull { runCatching { Class.forName(it.beanClassName) }.getOrNull() }

        assertTrue(repoClasses.isNotEmpty(), "No @Repository classes found in adapter.output.persistence")

        val outputPortPrefix = "com.opendatamask.domain.port.output"
        val violations = repoClasses.filter { repo ->
            repo.interfaces.none { it.name.startsWith(outputPortPrefix) }
        }

        assertTrue(violations.isEmpty(),
            "Repositories not implementing an output port: ${violations.map { it.simpleName }}")
    }

    @Test
    fun `no domain class directly references adapter types`() {
        val adapterPrefix = "com.opendatamask.adapter"
        val violations = mutableListOf<String>()

        val resolver = PathMatchingResourcePatternResolver()
        val resources = resolver.getResources("classpath*:com/opendatamask/domain/**/*.class")

        for (resource in resources) {
            val uri = resource.uri.toString()
            val relativePath = uri.substringAfterLast("com/opendatamask/domain/")
                .removeSuffix(".class")
                .replace('/', '.')
            if ('$' in relativePath) continue
            val fullName = "com.opendatamask.domain.$relativePath"
            val cls = runCatching { Class.forName(fullName) }.getOrNull() ?: continue

            cls.declaredFields.forEach { field ->
                if (field.type.name.startsWith(adapterPrefix)) {
                    violations += "${cls.simpleName}.${field.name} references adapter type ${field.type.simpleName}"
                }
            }
            cls.declaredMethods.forEach { method ->
                if (method.returnType.name.startsWith(adapterPrefix)) {
                    violations += "${cls.simpleName}.${method.name}() returns adapter type"
                }
                method.parameterTypes.forEach { param ->
                    if (param.name.startsWith(adapterPrefix)) {
                        violations += "${cls.simpleName}.${method.name}() has adapter-type param"
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(),
            "Domain classes referencing adapter types (hexagonal boundary violation):\n${violations.joinToString("\n")}")
    }
}
