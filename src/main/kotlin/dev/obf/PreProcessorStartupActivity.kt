package dev.obf

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class PreProcessorStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.getService(VariantSyncListener::class.java)
    }
}
