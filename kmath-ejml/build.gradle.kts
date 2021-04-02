import ru.mipt.npm.gradle.Maturity

plugins {
    id("ru.mipt.npm.gradle.jvm")
}

dependencies {
    api("org.ejml:ejml-simple:0.40")
    api(project(":kmath-core"))
}

readme {
    maturity = Maturity.PROTOTYPE
    propertyByTemplate("artifact", rootProject.file("docs/templates/ARTIFACT-TEMPLATE.md"))

    feature(
        id = "ejml-vector",
        description = "The Point implementation using SimpleMatrix.",
        ref = "src/main/kotlin/space/kscience/kmath/ejml/EjmlVector.kt"
    )

    feature(
        id = "ejml-matrix",
        description = "The Matrix implementation using SimpleMatrix.",
        ref = "src/main/kotlin/space/kscience/kmath/ejml/EjmlMatrix.kt"
    )

    feature(
        id = "ejml-linear-space",
        description = "The LinearSpace implementation using SimpleMatrix.",
        ref = "src/main/kotlin/space/kscience/kmath/ejml/EjmlLinearSpace.kt"
    )
}