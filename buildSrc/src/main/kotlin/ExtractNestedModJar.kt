import org.gradle.api.GradleException
import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.util.zip.ZipFile

@CacheableTransform
abstract class ExtractNestedModJar : TransformAction<ExtractNestedModJar.Parameters> {
    interface Parameters : TransformParameters {
        @get:Input
        val nestedPath: Property<String>
    }

    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        val path = parameters.nestedPath.get()
        val output = outputs.file("nested-mod.jar")

        ZipFile(input).use { archive ->
            val entry = archive.getEntry(path)
                ?: throw GradleException("$path was not found in ${input.name}")

            archive.getInputStream(entry).use { source ->
                output.outputStream().use(source::copyTo)
            }
        }
    }
}
