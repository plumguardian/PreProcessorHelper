package dev.obf

import com.android.tools.idea.gradle.project.sync.GradleSyncListenerWithRoot
import com.android.tools.idea.gradle.project.sync.GradleSyncState
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.plugins.gradle.util.GradleConstants

@Service(Service.Level.PROJECT)
class VariantSyncListener(private val project: Project) : Disposable {
    init {
        GradleSyncState.getInstance(project).subscribe(
            project,
            object : GradleSyncListenerWithRoot {
                override fun syncSucceeded(
                    project: Project,
                    rootProjectPath: String
                ) {
                    onSyncSucceeded(rootProjectPath)
                }
            },
            this
        )
    }

    private fun onSyncSucceeded(rootProjectPath: String) {
        if (project.isDisposed) return

        val properties = PropertiesComponent.getInstance(project)

        for (module in ModuleManager.getInstance(project).modules) {
            val facet = AndroidFacet.getInstance(module) ?: continue

            val variantName = facet.properties.SELECTED_BUILD_VARIANT ?: continue
            if (variantName.isBlank()) continue

            val key = PROPERTY_PREFIX + module.name
            val previousVariant = properties.getValue(key)
            if (variantName == previousVariant) continue

            // TODO: is module.name correct?
            val taskName = ":${module.name}:applyPreprocess${variantName[0].uppercaseChar()}${variantName.substring(1)}Sources"

            val settings = ExternalSystemTaskExecutionSettings()
            settings.externalProjectPath = rootProjectPath
            settings.taskNames = mutableListOf(taskName)
            settings.externalSystemIdString = GradleConstants.SYSTEM_ID.id

            ExternalSystemUtil.runTask(
                settings,
                DefaultRunExecutor.EXECUTOR_ID,
                project,
                GradleConstants.SYSTEM_ID,
                object : TaskCallback {
                    override fun onSuccess() {
                        if (!project.isDisposed) {
                            properties.setValue(key, variantName)
                        }
                    }

                    override fun onFailure() {}
                },
                ProgressExecutionMode.IN_BACKGROUND_ASYNC,
                false
            )
        }
    }

    override fun dispose() {}

    companion object {
        private const val PROPERTY_PREFIX = "dev.tocraft.preprocessor.variant."
    }
}
