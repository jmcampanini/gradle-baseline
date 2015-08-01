package com.palantir.baseline.plugins

import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.plugins.quality.CheckstylePlugin

/**
 * Configures the Gradle 'checkstyle' task with Baseline settings.
 */
class BaselineCheckstyle extends AbstractBaselinePlugin {

    static String DEFAULT_CHECKSTYLE_VERSION = '6.7'

    void apply(Project project) {
        this.project = project

        project.plugins.apply CheckstylePlugin

        // Set default version (outside afterEvaluate so it can be overriden).
        project.extensions.findByType(CheckstyleExtension).toolVersion = DEFAULT_CHECKSTYLE_VERSION

        project.afterEvaluate { Project p ->
            configureCheckstyle()
            configureCheckstyleForEclipse()
        }
    }

    def configureCheckstyle() {
        project.logger.info("Baseline: Configuring Checkstyle tasks")

        def configProps = project.checkstyle.configProperties
        // Required to enable checkstyle suppressions
        configProps['samedir'] = "${configDir}/checkstyle"

        // Configure checkstyle
        project.checkstyle {
            configFile = project.file("${configDir}/checkstyle/checkstyle.xml")
            configProperties = configProps
        }

        // Set custom source rules for checkstyleMain task.
        Checkstyle task = (Checkstyle) project.tasks.checkstyleMain

        // Make checkstyle include files in src/main/resources in whitespace checks.
        task.source 'src/main/resources'

        // These sources are only checked by gradle, NOT by Eclipse.
        def sources = ['checks', 'manifests', 'scripts', 'templates']
        sources.each { source -> task.source source }

        // Make sure java files are still included. This should match list in etc/eclipse-template/.checkstyle.
        // Currently not enforced, but could be eventually.
        def includeExtensions =
            ['java', 'cfg', 'coffee', 'erb', 'groovy', 'handlebars', 'json', 'less', 'pl', 'pp', 'sh', 'xml']
        includeExtensions.each { extension ->
            task.include "**/*.$extension"
        }
    }

    def configureCheckstyleForEclipse() {
        def eclipse = project.plugins.findPlugin "eclipse"
        if (eclipse == null) {
            project.logger.info "Baseline: Skipping configuring Eclipse for Checkstyle (eclipse not applied)"
            return
        }
        project.logger.info "Baseline: Configuring Eclipse Checkstyle"
        project.eclipse.project {
            natures "net.sf.eclipsecs.core.CheckstyleNature"
            buildCommand "net.sf.eclipsecs.core.CheckstyleBuilder"
        }
    }
}